package com.hostel.service;
import com.hostel.dto.Requests.*;
import com.hostel.security.Access;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.hostel.service.Database.*;
import static com.hostel.service.MeterService.date;
@Service
@Transactional
public class ContractService {
    private final Database db;private final Access access;private final Audit audit;private final MeterService meters;private final InvoiceService invoices;
    public ContractService(Database db,Access access,Audit audit,MeterService meters,InvoiceService invoices) {this.db=db;this.access=access;this.audit=audit;this.meters=meters;this.invoices=invoices;}
    public Object create(Contract r) {
        access.manager();var room=db.one("select * from rooms where id=? for update",r.roomId());access.room(r.roomId());
        require("AVAILABLE".equals(room.get("status")),"Room must be available");require(r.startDate().isBefore(r.endDate()),"End date must be after start date");
        require(db.jdbc.queryForObject("select count(*) from contracts where room_id=? and status='ACTIVE'",Long.class,r.roomId())==0,"Room has an active contract");
        db.one("select id from tenant_profiles where id=?",r.tenantId());
        if(access.role().equals("STAFF")) require(db.jdbc.queryForObject("select count(*) from tenant_profiles t where t.id=? and (exists(select 1 from audit_logs where resource_type='tenant_profiles' and resource_id=t.id and user_id=? and action='CREATE') or exists(select 1 from contracts c join rooms r on r.id=c.room_id where c.primary_tenant_id=t.id and "+access.roomScope("r.id")+"))",Long.class,r.tenantId(),access.userId())>0,"Tenant outside assigned properties");
        long id=db.insert("contracts","contract_code,room_id,primary_tenant_id,start_date,end_date,rental_price,deposit_amount,payment_cycle_day,initial_electric_index,initial_water_index",r.code(),r.roomId(),r.tenantId(),r.startDate(),r.endDate(),r.rentalPrice(),r.deposit(),r.paymentDay(),r.electricIndex(),r.waterIndex());
        db.jdbc.update("update rooms set status='RENTED',updated_at=current_timestamp where id=?",r.roomId());audit.log("CREATE","contracts",id,null,r);return detail(id);
    }
    public Map<String,Object> detail(long id) {var c=new LinkedHashMap<>(access.contract(id));c.put("violations",db.rows("select v.*,u.full_name recorded_by_name from contract_violations v join users u on u.id=v.recorded_by where contract_id=? order by v.id desc",id));c.put("members",db.rows("select * from contract_members where contract_id=? order by id",id));var checkout=db.rows("select * from checkout_records where contract_id=?",id);c.put("checkout",checkout.isEmpty()?null:checkout.getFirst());return c;}
    public Object member(long id,Member r) {
        access.manager();var c=access.contract(id);db.one("select id from contracts where id=? for update",id);
        require("ACTIVE".equals(c.get("status")),"Contract not active");
        LocalDate from=r.effectiveFrom()==null?date(c,"start_date"):r.effectiveFrom();
        require(!from.isBefore(date(c,"start_date")) && from.isBefore(date(c,"end_date")),"Member date outside contract");
        require(db.jdbc.queryForObject("select count(*) from invoices where contract_id=? and billing_period>=?",Long.class,id,YearMonth.from(from).toString())==0,"Membership changes cannot affect an existing invoice");
        int max=((Number)db.one("select max_occupants from rooms where id=?",c.get("room_id")).get("max_occupants")).intValue();
        require(1+db.jdbc.queryForObject("select count(*)::int from contract_members where contract_id=? and (effective_to is null or effective_to>?)",Integer.class,id,from)<max,"Room capacity exceeded");
        long memberId=db.insert("contract_members","contract_id,full_name,identity_number,phone,relationship,effective_from",id,r.fullName(),r.identityNumber(),r.phone(),r.relationship(),from);audit.log("ADD_MEMBER","contracts",id,null,r);return Map.of("id",memberId);
    }
    public Object renew(long id,LocalDate end) {
        access.manager();var c=access.contract(id);db.one("select id from contracts where id=? for update",id);require("ACTIVE".equals(c.get("status")),"Contract not active");require(end.isAfter(date(c,"end_date")),"New end must extend current end");
        db.jdbc.update("update contracts set end_date=?,updated_at=current_timestamp where id=?",end,id);audit.log("RENEW","contracts",id,c,end);return detail(id);
    }
    public Object checkout(long id,Checkout r) {
        access.manager();var c=access.contract(id);db.one("select id from contracts where id=? for update",id);
        require(List.of("ACTIVE","EXPIRED").contains(c.get("status")),"Contract already ended");
        require(r.date().isAfter(date(c,"start_date")) && !r.date().isAfter(date(c,"end_date")),"Checkout date outside contract");
        // Checkout date is exclusive: someone leaving on day 16 occupies days 1 through 15.
        LocalDate lastOccupied=r.date().minusDays(1);YearMonth month=YearMonth.from(lastOccupied);
        String period=month.toString();
        require(r.electricIndex().compareTo(meters.latestIndex(c,"ELECTRICITY",null))>=0 && r.waterIndex().compareTo(meters.latestIndex(c,"WATER",null))>=0,"Final indices below recorded indices");
        require(db.jdbc.queryForObject("select count(*) from meter_readings where contract_id=? and billing_period>?",Long.class,id,period)==0,"Readings exist after checkout date");
        require(db.jdbc.queryForObject("select count(*) from invoices where contract_id=? and status='DRAFT'",Long.class,id)==0,"Issue or remove draft invoices before checkout");
        var current=db.rows("select * from invoices where contract_id=? and billing_period=?",id,period);
        db.jdbc.update("update contracts set actual_checkout_date=? where id=?",r.date(),id);
        if(current.isEmpty()) {
            // Temporarily allow the final readings even if a scheduled expiry already ran.
            db.jdbc.update("update contracts set status='ACTIVE' where id=?",id);
            finalReading(id,period,"ELECTRICITY",r.electricIndex(),lastOccupied);
            finalReading(id,period,"WATER",r.waterIndex(),lastOccupied);
            var invoice=invoices.createSettlement(new Invoice(id,period,lastOccupied,r.date()));invoices.issue(id(invoice,"id"));
        } else {
            require("ISSUED".equals(current.getFirst().get("status")),"Final invoice must be issued");
            require(r.electricIndex().compareTo(meters.latestIndex(c,"ELECTRICITY",null))==0 && r.waterIndex().compareTo(meters.latestIndex(c,"WATER",null))==0,"Final indices must match issued invoice");
            LocalDate from=date(c,"start_date").isAfter(month.atDay(1))?date(c,"start_date"):month.atDay(1);
            BigDecimal expected=money(c,"rental_price").multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(from,r.date()))).divide(BigDecimal.valueOf(month.lengthOfMonth()),2,RoundingMode.HALF_UP);
            BigDecimal billed=db.jdbc.queryForObject("select subtotal from invoice_items where invoice_id=? and service_item_id is null order by id limit 1",BigDecimal.class,id(current.getFirst(),"id"));
            require(expected.compareTo(billed)==0,"Issued rent covers different occupied dates; checkout must match billed period");
        }
        // Require all occupied months to be billed; prevents silently waiving previous rent.
        LocalDate firstMonth=date(c,"start_date").withDayOfMonth(1);
        long expectedMonths=ChronoUnit.MONTHS.between(firstMonth,month.atDay(1))+1;
        long billedMonths=db.jdbc.queryForObject("select count(*) from invoices where contract_id=? and status='ISSUED' and billing_period>=? and billing_period<=?",Long.class,id,YearMonth.from(firstMonth).toString(),period);
        require(expectedMonths==billedMonths,"Create and issue invoices for all occupied months before checkout");
        BigDecimal unpaid=db.jdbc.queryForObject("select coalesce(sum(balance_due),0) from invoices where contract_id=? and status='ISSUED'",BigDecimal.class,id);
        BigDecimal deposit=money(c,"deposit_amount"),refund=deposit.subtract(unpaid).subtract(r.damage());
        BigDecimal utilities=db.jdbc.queryForObject("select coalesce(sum(ii.subtotal),0) from invoices i join invoice_items ii on ii.invoice_id=i.id where i.contract_id=? and i.billing_period=? and i.status='ISSUED' and ii.old_index_snapshot is not null and ii.new_index_snapshot is not null",BigDecimal.class,id,period);
        long recordId=db.insert("checkout_records","contract_id,checkout_date,final_electric_index,final_water_index,final_utility_amount,damage_amount,unpaid_invoices_amount,initial_deposit,actual_refund_amount,settled_by,notes",id,r.date(),r.electricIndex(),r.waterIndex(),utilities,r.damage(),unpaid,deposit,refund,access.userId(),r.notes());
        // Allocate the deposit to debts, oldest first; every allocation is a real ledger transaction.
        BigDecimal available=deposit.subtract(r.damage()).max(BigDecimal.ZERO);
        for(var invoice:db.rows("select * from invoices where contract_id=? and status='ISSUED' and balance_due>0 order by billing_period",id)) {
            BigDecimal amount=available.min(money(invoice,"balance_due"));if(amount.signum()==0) break;
            invoices.pay(id(invoice,"id"),new Payment("DEPOSIT-"+recordId+"-"+invoice.get("id"),amount,"DEPOSIT_OFFSET","Cấn trừ tiền cọc khi trả phòng"));available=available.subtract(amount);
        }
        db.jdbc.update("update contracts set status='TERMINATED',updated_at=current_timestamp where id=?",id);
        db.jdbc.update("update rooms set status='AVAILABLE',updated_at=current_timestamp where id=?",c.get("room_id"));
        audit.log("CHECKOUT","contracts",id,c,r);return db.one("select * from checkout_records where id=?",recordId);
    }
    private void finalReading(long contractId,String period,String type,BigDecimal value,LocalDate date) {
        var readings=db.rows("select * from meter_readings where contract_id=? and billing_period=? and meter_type=?",contractId,period,type);
        if(readings.isEmpty()) meters.recordSettlement(new Reading(contractId,period,type,value,date));
        else if(money(readings.getFirst(),"new_index").compareTo(value)!=0) meters.edit(id(readings.getFirst(),"id"),value,"Chốt số khi trả phòng");
    }
}
