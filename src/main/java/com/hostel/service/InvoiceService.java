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
import static com.hostel.service.MeterService.*;
@Service @Transactional
public class InvoiceService {
 private final Database db;private final Access access;private final PricingService pricing;private final BillingCalendar calendar;
 public InvoiceService(Database db,Access access,PricingService pricing,BillingCalendar calendar){this.db=db;this.access=access;this.pricing=pricing;this.calendar=calendar;}
 public Map<String,Object> create(Invoice r){return create(r,false);}
 public Map<String,Object> createSettlement(Invoice r){return create(r,true);}
 private Map<String,Object> create(Invoice r,boolean settlement){
  access.manager();access.contract(r.contractId());db.one("select id from contracts where id=? for update",r.contractId());var c=access.contract(r.contractId());
  var dates=calendar.dates(r.contractId(),r.period());LocalDate issue=settlement?r.issueDate():(LocalDate)dates.get("issueDate"),due=settlement?r.dueDate():(LocalDate)dates.get("dueDate");
  require(settlement||r.issueDate()==null||r.issueDate().equals(issue),"Issue date differs from property policy");require(settlement||r.dueDate()==null||r.dueDate().equals(due),"Due date differs from contract policy");
  LocalDate p=period(r.period()),end=c.get("actual_checkout_date")==null?date(c,"end_date"):date(c,"actual_checkout_date");
  LocalDate from=date(c,"start_date").isAfter(p)?date(c,"start_date"):p,to=end.isBefore(p.plusMonths(1))?end:p.plusMonths(1);
  int days=(int)ChronoUnit.DAYS.between(from,to);require(days>0,"No occupied days in billing period");pricing.requireMandatory(id(c,"property_id"),p);
  long invoice=db.insert("invoices","invoice_code,contract_id,billing_period,issue_date,due_date,is_settlement","INV-"+r.contractId()+"-"+r.period(),r.contractId(),p,issue,due,settlement);
  item(invoice,null,null,"Tiền phòng","Tháng",money(c,"rental_price"),days);
  int people=days+db.jdbc.queryForObject("select coalesce(sum(greatest(0,least(coalesce(effective_to,?::date),?::date)-greatest(effective_from,?::date))),0)::int from contract_members where contract_id=?",Integer.class,to,to,from,r.contractId());
  for(var service:pricing.prices(id(c,"property_id"),p)){
   if("METER_BASED".equals(service.get("calculation_type"))){var reading=db.one("select id from meter_readings where contract_id=? and service_item_id=? and billing_period=?",r.contractId(),service.get("id"),p);item(invoice,null,id(reading,"id"),service.get("name").toString(),service.get("unit_name").toString(),money(service,"unit_price"),null);}
   else item(invoice,id(service,"id"),null,service.get("name").toString(),service.get("unit_name").toString(),money(service,"unit_price"),"PER_PERSON".equals(service.get("calculation_type"))?people:days);
  }
  return detail(invoice);
 }
 private void item(long invoice,Long service,Long reading,String name,String unit,BigDecimal price,Integer quantity){db.insert("invoice_items","invoice_id,service_item_id,meter_reading_id,item_name_snapshot,unit_name_snapshot,unit_price_snapshot,quantity",invoice,service,reading,name,unit,price,quantity);}
 public Map<String,Object> detail(long id){var result=new LinkedHashMap<>(access.invoice(id));if(access.role().equals("TENANT")&&!"ISSUED".equals(result.get("status")))Access.denied();
  var items=db.rows("select l.*,m.old_index old_index_snapshot,m.new_index new_index_snapshot from v_invoice_lines l left join meter_readings m on m.id=l.meter_reading_id where invoice_id=? order by l.id",id);
  for(var line:items){line.put("quantity",line.get("billed_quantity"));line.put("formula_description",line.get("meter_reading_id")!=null?"("+line.get("new_index_snapshot")+" - "+line.get("old_index_snapshot")+") × "+line.get("unit_price_snapshot"):line.get("quantity")+" ngày/ngày-người × "+line.get("unit_price_snapshot")+" / "+YearMonth.from(date(result,"billing_period")).lengthOfMonth());}
  result.put("items",items);result.put("payments",db.rows("select * from payment_transactions where invoice_id=? order by id",id));return result;
 }
 public Map<String,Object> issue(long id){access.manager();access.invoice(id);var i=db.one("select * from invoices where id=? for update",id);require("DRAFT".equals(i.get("status")),"Only DRAFT invoices may be issued");db.jdbc.update("update invoices set status='ISSUED' where id=?",id);return detail(id);}
 public Map<String,Object> pay(long id,Payment r){access.manager();access.invoice(id);db.one("select id from invoices where id=? for update",id);var i=db.one("select * from v_invoice_balances where id=?",id);require("ISSUED".equals(i.get("status")),"Only ISSUED invoices may be paid");require(r.amount().signum()>0 && r.amount().compareTo(money(i,"balance_due"))<=0,"Payment exceeds outstanding balance or is not positive");db.insert("payment_transactions","invoice_id,transaction_code,amount,payment_method,received_by,notes",id,r.code(),r.amount(),r.method(),access.userId(),r.notes());return detail(id);}
 public void removeDraft(long id){access.manager();access.invoice(id);var i=db.one("select * from invoices where id=? for update",id);require("DRAFT".equals(i.get("status")),"Issued invoice is immutable");db.jdbc.update("delete from invoices where id=?",id);}
}
