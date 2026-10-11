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
public class InvoiceService {
    private final Database db;private final Access access;private final Audit audit;private final PricingService pricing;private final BillingCalendar calendar;
    private final com.hostel.repository.InvoiceItemRepository items;
    private final RoomFeeCalculator roomFee;private final ElectricityFeeCalculator electricFee;private final WaterFeeCalculator waterFee;private final ServiceFeeCalculator serviceFee;
    public InvoiceService(Database db,Access access,Audit audit,PricingService pricing,RoomFeeCalculator roomFee,ElectricityFeeCalculator electricFee,WaterFeeCalculator waterFee,ServiceFeeCalculator serviceFee,com.hostel.repository.InvoiceItemRepository items,BillingCalendar calendar) {
        this.db=db;this.access=access;this.audit=audit;this.pricing=pricing;this.roomFee=roomFee;this.electricFee=electricFee;this.waterFee=waterFee;this.serviceFee=serviceFee;
        this.items=items;this.calendar=calendar;
    }
    public Map<String,Object> create(Invoice r) {
        return create(r,false);
    }
    public Map<String,Object> createSettlement(Invoice r) {return create(r,true);}
    private Map<String,Object> create(Invoice request,boolean settlement) {
        access.manager();access.contract(request.contractId());Invoice r=request;
        if(!settlement) {
            var dates=calendar.dates(r.contractId(),r.period());
            LocalDate issue=(LocalDate)dates.get("issueDate"),due=(LocalDate)dates.get("dueDate");
            require(r.issueDate()==null || issue.equals(r.issueDate()),"Ngày phát hành theo chính sách khu nhà: "+issue);
            require(r.dueDate()==null || due.equals(r.dueDate()),"Hạn thanh toán theo hợp đồng: "+due);
            r=new Invoice(r.contractId(),r.period(),issue,due);
        }
        access.manager();var c=access.contract(r.contractId());db.one("select id from contracts where id=? for update",r.contractId());
        require(!"PENDING".equals(c.get("status")),"Contract not started"); require(!r.dueDate().isBefore(r.issueDate()),"Due date before issue date");
        YearMonth month=YearMonth.parse(r.period());LocalDate start=date(c,"start_date"),end=c.get("actual_checkout_date")!=null?date(c,"actual_checkout_date"):date(c,"end_date");
        LocalDate from=start.isAfter(month.atDay(1))?start:month.atDay(1);LocalDate to=end.isBefore(month.plusMonths(1).atDay(1))?end:month.plusMonths(1).atDay(1);
        int days=(int)ChronoUnit.DAYS.between(from,to);require(days>0,"No occupied days in billing period");
        pricing.requireMandatory(id(c,"property_id"),month.atDay(1));
        long invoiceId=db.insert("invoices","invoice_code,contract_id,billing_period,issue_date,due_date","INV-"+r.contractId()+"-"+r.period(),r.contractId(),r.period(),r.issueDate(),r.dueDate());
        db.jdbc.update("update invoices set is_settlement=? where id=?",settlement,invoiceId);
        BigDecimal rent=roomFee.calculate(money(c,"rental_price"),BigDecimal.ONE,days,month.lengthOfMonth());
        item(invoiceId,null,"Tiền phòng",money(c,"rental_price"),BigDecimal.ONE,null,null,rent,days+"/"+month.lengthOfMonth()+" ngày × "+c.get("rental_price"));
        int personDays=days+db.jdbc.queryForObject("select coalesce(sum(greatest(0,least(coalesce(effective_to,?::date),?::date)-greatest(effective_from,?::date))),0)::int from contract_members where contract_id=?",Integer.class,to,to,from,r.contractId());
        boolean anomaly=false;BigDecimal total=rent;
        for(var s:pricing.prices(id(c,"property_id"),month.atDay(1))) {
            BigDecimal quantity,subtotal,oldIndex=null,newIndex=null;String formula;
            if("METER_BASED".equals(s.get("calculation_type"))) {
                var readings=db.rows("select * from meter_readings where contract_id=? and billing_period=? and meter_type=?",r.contractId(),r.period(),s.get("service_type"));
                require(!readings.isEmpty(),"Missing "+s.get("service_type")+" reading");var m=readings.getFirst();
                quantity=money(m,"consumption");oldIndex=money(m,"old_index");newIndex=money(m,"new_index");
                subtotal=("WATER".equals(s.get("service_type"))?waterFee:electricFee).calculate(money(s,"unit_price"),quantity,days,month.lengthOfMonth());
                formula="("+newIndex+" − "+oldIndex+") × "+s.get("unit_price")+" = "+subtotal;
                anomaly|=Boolean.TRUE.equals(m.get("has_anomaly"));
            } else {
                boolean perPerson="PER_PERSON".equals(s.get("calculation_type"));
                quantity=BigDecimal.valueOf(perPerson?personDays:1);
                subtotal=serviceFee.calculate(money(s,"unit_price"),quantity,perPerson?1:days,month.lengthOfMonth());
                formula=perPerson?personDays+" ngày-người × "+s.get("unit_price")+" / "+month.lengthOfMonth()+" ngày = "+subtotal:quantity+" × "+s.get("unit_price")+" × "+days+"/"+month.lengthOfMonth()+" ngày = "+subtotal;
            }
            item(invoiceId,id(s,"id"),s.get("name").toString(),money(s,"unit_price"),quantity,oldIndex,newIndex,subtotal,formula);total=total.add(subtotal);
        }
        var policy=db.one("select * from properties where id=?",c.get("property_id"));
        if(Boolean.TRUE.equals(policy.get("late_fee_enabled"))) {
            BigDecimal debt=db.jdbc.queryForObject("select coalesce(sum(balance_due),0) from invoices where contract_id=? and status='ISSUED' and due_date<?",BigDecimal.class,r.contractId(),r.issueDate());
            BigDecimal fee=debt.multiply(money(policy,"late_fee_percentage")).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);
            if(fee.signum()>0) {item(invoiceId,null,"Phí chậm thanh toán",fee,BigDecimal.ONE,null,null,fee,debt+" × "+policy.get("late_fee_percentage")+"% = "+fee);total=total.add(fee);}
        }
        db.jdbc.update("update invoices set total_amount=?,balance_due=?,has_anomaly_alert=? where id=?",total,total,anomaly,invoiceId);
        audit.log("CREATE_DRAFT","invoices",invoiceId,null,r);return detail(invoiceId);
    }
    void item(long invoiceId,Long serviceId,String name,BigDecimal price,BigDecimal quantity,BigDecimal oldIndex,BigDecimal newIndex,BigDecimal subtotal,String formula) {
        db.insert("invoice_items","invoice_id,service_item_id,item_name_snapshot,unit_price_snapshot,quantity,old_index_snapshot,new_index_snapshot,subtotal,formula_description",invoiceId,serviceId,name,price,quantity,oldIndex,newIndex,subtotal,formula);
    }
    public Map<String,Object> detail(long id) {
        var i=new LinkedHashMap<>(access.invoice(id));
        if(access.role().equals("TENANT") && !"ISSUED".equals(i.get("status"))) Access.denied();
        i.put("items",items.findByInvoiceIdOrderByIdAsc(id).stream().map(line -> {
            Map<String,Object> row=new LinkedHashMap<>();row.put("id",line.getId());row.put("item_name_snapshot",line.getName());
            row.put("unit_price_snapshot",line.getPrice());row.put("quantity",line.getQuantity());row.put("subtotal",line.getSubtotal());
            row.put("old_index_snapshot",line.getOldIndex());row.put("new_index_snapshot",line.getNewIndex());row.put("formula_description",line.getFormula());return row;
        }).toList());
        i.put("payments",db.rows("select * from payment_transactions where invoice_id=? order by id",id));return i;
    }
    public Map<String,Object> issue(long id) {
        access.manager();access.invoice(id);var i=db.one("select * from invoices where id=? for update",id);
        require("DRAFT".equals(i.get("status")),"Only draft invoices can be issued");
        require(!date(i,"issue_date").isAfter(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))),"Chưa đến ngày phát hành "+i.get("issue_date"));
        require(db.jdbc.queryForObject("select count(*) from consumption_alerts a join meter_readings m on m.id=a.meter_reading_id where m.contract_id=? and m.billing_period=? and a.status='PENDING'",Long.class,i.get("contract_id"),i.get("billing_period"))==0,"Review consumption alerts before issuing");
        db.jdbc.update("update invoices set status='ISSUED',updated_at=current_timestamp where id=?",id);
        notifyTenant(id(i,"contract_id"),"INVOICE","Hóa đơn kỳ "+i.get("billing_period")+" đã phát hành","invoice:"+id);
        audit.log("ISSUE","invoices",id,i,"ISSUED");return detail(id);
    }
    public Map<String,Object> pay(long id,Payment r) {
        access.manager();access.invoice(id);var i=db.one("select * from invoices where id=? for update",id);
        require("ISSUED".equals(i.get("status")),"Only issued invoices can be paid");require(r.amount().compareTo(money(i,"balance_due"))<=0,"Payment exceeds outstanding balance");
        db.insert("payment_transactions","invoice_id,transaction_code,amount,payment_method,received_by,notes",id,r.code(),r.amount(),r.method(),access.userId(),r.notes());
        BigDecimal paid=money(i,"paid_amount").add(r.amount()),balance=money(i,"total_amount").subtract(paid);
        db.jdbc.update("update invoices set paid_amount=?,balance_due=?,payment_status=?,updated_at=current_timestamp where id=?",paid,balance,balance.signum()==0?"PAID":"PARTIALLY_PAID",id);
        audit.log("PAYMENT","invoices",id,i,r);return detail(id);
    }
    public void notifyTenant(long contractId,String type,String message,String key) {
        var tenants=db.rows("select t.user_id from contracts c join tenant_profiles t on t.id=c.primary_tenant_id where c.id=? and t.user_id is not null",contractId);
        if(!tenants.isEmpty()) db.jdbc.update("insert into notifications(user_id,type,message,reference_key) values(?,?,?,?) on conflict(user_id,reference_key) do nothing",tenants.getFirst().get("user_id"),type,message,key);
    }
    public void removeDraft(long id) {
        access.manager();access.invoice(id);var i=db.one("select * from invoices where id=? for update",id);require("DRAFT".equals(i.get("status")),"Issued invoices are immutable");
        audit.log("DELETE_DRAFT","invoices",id,i,null);db.jdbc.update("delete from invoices where id=?",id);
    }
}
