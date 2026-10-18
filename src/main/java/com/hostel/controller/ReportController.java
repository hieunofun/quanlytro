package com.hostel.controller;
import com.hostel.security.Access;
import com.hostel.service.Database;
import org.springframework.web.bind.annotation.*;
import java.time.YearMonth;
import java.util.*;
@RestController
@RequestMapping("/api")
public class ReportController {
    private final Database db;private final Access access;
    public ReportController(Database db,Access access) {this.db=db;this.access=access;}
    @GetMapping("/dashboard") public Object dashboard(@RequestParam(required=false) String period) {
        access.manager();String month=period==null?YearMonth.now().toString():YearMonth.parse(period).toString();
        var data=new LinkedHashMap<String,Object>(db.one("select count(*) total_rooms,count(*) filter(where display_status='AVAILABLE') available_rooms,count(*) filter(where display_status='RENTED') rented_rooms,count(*) filter(where display_status='MAINTENANCE') maintenance_rooms from v_room_status where "+access.roomScope("id")));
        data.putAll(db.one("select coalesce(sum(i.total_amount),0) billed,coalesce(sum(i.paid_amount),0) credited,coalesce(sum(i.balance_due),0) outstanding from v_invoice_balances i join contracts c on c.id=i.contract_id where "+access.contractScope("c")+" and i.status='ISSUED' and i.billing_period=?::date",month+"-01"));
        data.put("collected",db.jdbc.queryForObject("select coalesce(sum(p.amount),0) from payment_transactions p join invoices i on i.id=p.invoice_id join contracts c on c.id=i.contract_id where "+access.contractScope("c")+" and p.payment_method<>'DEPOSIT_OFFSET' and to_char(p.payment_date at time zone 'Asia/Ho_Chi_Minh','YYYY-MM')=?",java.math.BigDecimal.class,month));
        data.put("expiringContracts",db.rows("select c.*,r.room_code from contracts c join rooms r on r.id=c.room_id where "+access.contractScope("c")+" and c.status='ACTIVE' and c.end_date<=current_date+30 order by c.end_date"));
        data.put("open_maintenance",db.jdbc.queryForObject("select count(*) from maintenance_requests m where status<>'COMPLETED' and "+access.maintenanceScope("m"),Long.class));
        data.put("period",month);return data;
    }
    private String monthlyFinance() {
        String scope=access.contractScope("c");
        return "with bills as (select to_char(i.billing_period,'YYYY-MM') billing_period,sum(i.total_amount) billed,sum(i.balance_due) outstanding from v_invoice_balances i join contracts c on c.id=i.contract_id where "+scope+" and i.status='ISSUED' group by i.billing_period), receipts as (select to_char(p.payment_date at time zone 'Asia/Ho_Chi_Minh','YYYY-MM') period,sum(p.amount) collected from payment_transactions p join invoices i on i.id=p.invoice_id join contracts c on c.id=i.contract_id where "+scope+" and p.payment_method<>'DEPOSIT_OFFSET' group by period) select coalesce(b.billing_period,r.period) billing_period,coalesce(b.billed,0) billed,coalesce(r.collected,0) collected,coalesce(b.outstanding,0) outstanding from bills b full join receipts r on r.period=b.billing_period";
    }
    @GetMapping("/reports/revenue") public Object revenue() {access.manager();return db.rows(monthlyFinance()+" order by billing_period");}
    @GetMapping("/reports/debts") public Object debts() {access.manager();return db.rows("select i.invoice_code,i.billing_period,r.room_code,t.full_name,i.total_amount,i.paid_amount,i.balance_due,i.due_date from v_invoice_balances i join contracts c on c.id=i.contract_id join rooms r on r.id=c.room_id join tenant_profiles t on t.id=c.primary_tenant_id where "+access.contractScope("c")+" and i.status='ISSUED' and i.balance_due>0 order by i.due_date");}
    @GetMapping("/reports/revenue-quarterly") public Object quarterly() {access.manager();return db.rows("select to_char(to_date(m.billing_period||'-01','YYYY-MM-DD'),'YYYY-\"Q\"Q') quarter,sum(m.billed) billed,sum(m.collected) collected,sum(m.outstanding) outstanding from ("+monthlyFinance()+") m group by quarter order by quarter");}
    @GetMapping("/reports/occupancy-history") public Object occupancyHistory() {access.manager();return db.rows("select p.name,to_char(m.month,'YYYY-MM') period,count(distinct r.id) total_rooms,count(distinct r.id) filter(where exists(select 1 from contracts c where c.room_id=r.id and c.start_date<=(m.month+interval '1 month' - interval '1 day')::date and coalesce(c.actual_checkout_date,c.end_date)>(m.month+interval '1 month' - interval '1 day')::date)) occupied_rooms from properties p cross join generate_series(date_trunc('month',current_date)-interval '11 months',date_trunc('month',current_date),interval '1 month') m(month) left join v_room_status r on r.property_id=p.id and ("+access.roomScope("r.id")+") where "+access.propertyReadScope("p.id")+" group by p.id,p.name,m.month order by m.month,p.id");}
    @GetMapping("/reports/occupancy") public Object occupancy() {access.manager();return db.rows("select p.id,p.name,count(r.id) total_rooms,count(r.id) filter(where r.display_status='RENTED') rented_rooms,round(100.0*count(r.id) filter(where r.display_status='RENTED')/nullif(count(r.id),0),2) occupancy_percentage from properties p left join v_room_status r on r.property_id=p.id and ("+access.roomScope("r.id")+") where "+access.propertyReadScope("p.id")+" group by p.id,p.name order by p.id");}
    @GetMapping("/reports/maintenance") public Object maintenance() {access.manager();return db.rows("select to_char(created_at,'YYYY-MM') period,status,count(*) total, sum(actual_cost) cost from maintenance_requests m where "+access.maintenanceScope("m")+" group by period,status order by period");}
    @GetMapping(value="/reports/debts.csv",produces="text/csv;charset=UTF-8") public String export() {
        @SuppressWarnings("unchecked") var rows=(List<Map<String,Object>>)debts();StringBuilder out=new StringBuilder("\uFEFFMã hóa đơn,Kỳ,Phòng,Người thuê,Tổng tiền,Đã thu,Còn nợ,Hạn thanh toán\r\n");
        for(var row:rows) {out.append(String.join(",",row.values().stream().map(v -> csv(v)).toList())).append("\r\n");}return out.toString();
    }
    private String csv(Object v) {String s=Objects.toString(v,"");if(s.matches("^[=+@-].*"))s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
}
