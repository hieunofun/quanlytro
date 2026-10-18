package com.hostel.service;
import com.hostel.dto.Requests.*;
import com.hostel.security.Access;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;
import static com.hostel.service.Database.*;
import static com.hostel.service.MeterService.*;
@Service @Transactional
public class ContractService {
 private final Database db;private final Access access;private final MeterService meters;private final InvoiceService invoices;
 public ContractService(Database db,Access access,MeterService meters,InvoiceService invoices){this.db=db;this.access=access;this.meters=meters;this.invoices=invoices;}
 private void tenantScope(long tenant){db.one("select id from tenant_profiles where id=?",tenant);if(access.role().equals("STAFF"))require(db.jdbc.queryForObject("select count(*) from contracts c where (c.primary_tenant_id=? or exists(select 1 from contract_members m where m.contract_id=c.id and m.tenant_id=?)) and "+access.contractScope("c"),Long.class,tenant,tenant)>0,"Tenant is outside assigned properties; ask ADMIN to receive a new tenant");}
 public Object create(Contract r){access.manager();access.room(r.roomId());tenantScope(r.tenantId());db.one("select id from rooms where id=? for update",r.roomId());long id=db.insert("contracts","contract_code,room_id,primary_tenant_id,start_date,end_date,rental_price,deposit_amount,payment_cycle_day",r.code(),r.roomId(),r.tenantId(),r.startDate(),r.endDate(),r.rentalPrice(),r.deposit(),r.paymentDay());return detail(id);}
 public Map<String,Object> detail(long id){var c=new LinkedHashMap<>(access.contract(id));c.put("members",db.rows("select m.*,t.full_name,t.identity_number,t.phone from contract_members m join tenant_profiles t on t.id=m.tenant_id where contract_id=? order by m.id",id));var x=db.rows("select *,settlement_amount actual_refund_amount,deposit_snapshot initial_deposit,outstanding_snapshot unpaid_invoices_amount from v_checkout_totals where contract_id=?",id);c.put("checkout",x.isEmpty()?null:x.getFirst());c.put("violations",List.of());return c;}
 public Object member(long id,Member r){access.manager();access.contract(id);var c=db.one("select * from contracts where id=? for update",id);require("ACTIVE".equals(c.get("status")),"Contract is not active");tenantScope(r.tenantId());long member=db.insert("contract_members","contract_id,tenant_id,relationship,effective_from",id,r.tenantId(),r.relationship(),r.effectiveFrom()==null?date(c,"start_date"):r.effectiveFrom());return db.one("select * from contract_members where id=?",member);}
 public Object renew(long id,LocalDate end){access.manager();access.contract(id);var c=db.one("select * from contracts where id=? for update",id);require("ACTIVE".equals(c.get("status")) && end.isAfter(date(c,"end_date")),"New end must extend an active contract");db.jdbc.update("update contracts set end_date=? where id=?",end,id);return detail(id);}
 public Object checkout(long id,Checkout r){
  access.manager();access.contract(id);var c=db.one("select * from contracts where id=? for update",id);require("ACTIVE".equals(c.get("status")),"Contract already ended");
  require(r.date().isAfter(date(c,"start_date"))&&!r.date().isAfter(date(c,"end_date")),"Checkout date outside contract");LocalDate last=r.date().minusDays(1),period=last.withDayOfMonth(1);
  require(db.jdbc.queryForObject("select count(*) from meter_readings where contract_id=? and (billing_period>? or reading_date>=?)",Long.class,id,period,r.date())==0,"Readings exist after checkout");
  require(db.jdbc.queryForObject("select count(*) from invoices where contract_id=? and status='DRAFT'",Long.class,id)==0,"Issue or remove draft invoices before checkout");
  db.jdbc.update("delete from contract_members where contract_id=? and effective_from>=?",id,r.date());
  db.jdbc.update("update contract_members set effective_to=? where contract_id=? and (effective_to is null or effective_to>?)",r.date(),id,r.date());
  db.jdbc.update("update contracts set status='TERMINATED',actual_checkout_date=? where id=?",r.date(),id);
  var current=db.rows("select * from invoices where contract_id=? and billing_period=?",id,period);
  if(current.isEmpty()){
   finalReading(id,period,"ELECTRICITY",r.electricIndex(),last);finalReading(id,period,"WATER",r.waterIndex(),last);
   var invoice=invoices.createSettlement(new Invoice(id,YearMonth.from(period).toString(),last,r.date()));invoices.issue(id(invoice,"id"));
  }else{
   require(meters.latestIndex(c,"ELECTRICITY",null).compareTo(r.electricIndex())==0 && meters.latestIndex(c,"WATER",null).compareTo(r.waterIndex())==0,"Final indices differ from issued data");
  }
  // guard_checkout locks all invoices, checks every occupied month and captures debt before offset.
  long record=db.insert("checkout_records","contract_id,deposit_snapshot,outstanding_snapshot,damage_amount,settled_by,notes",id,money(c,"deposit_amount"),BigDecimal.ZERO,r.damage(),access.userId(),r.notes());
  BigDecimal available=money(c,"deposit_amount").subtract(r.damage()).max(BigDecimal.ZERO);
  for(var invoice:db.rows("select * from v_invoice_balances where contract_id=? and status='ISSUED' and balance_due>0 order by id",id)){
   BigDecimal amount=available.min(money(invoice,"balance_due"));if(amount.signum()==0)break;
   invoices.pay(id(invoice,"id"),new Payment("DEPOSIT-"+record+"-"+invoice.get("id"),amount,"DEPOSIT_OFFSET","Cấn cọc khi trả phòng"));available=available.subtract(amount);
  }
  return db.one("select *,settlement_amount actual_refund_amount,deposit_snapshot initial_deposit,outstanding_snapshot unpaid_invoices_amount from v_checkout_totals where id=?",record);
 }
 private void finalReading(long contract,LocalDate period,String type,BigDecimal value,LocalDate last){long service=meters.service(type);var found=db.rows("select * from meter_readings where contract_id=? and billing_period=? and service_item_id=?",contract,period,service);if(found.isEmpty())meters.recordSettlement(new Reading(contract,YearMonth.from(period).toString(),type,value,last,null));else if(money(found.getFirst(),"new_index").compareTo(value)!=0)meters.edit(id(found.getFirst(),"id"),value,"Checkout");}
}
