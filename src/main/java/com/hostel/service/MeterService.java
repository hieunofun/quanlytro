package com.hostel.service;
import com.hostel.dto.Requests.Reading;
import com.hostel.security.Access;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;
import static com.hostel.service.Database.*;
@Service
@Transactional
public class MeterService {
    private final Database db;private final Access access;private final Audit audit;private final BillingCalendar calendar;
    public MeterService(Database db,Access access,Audit audit,BillingCalendar calendar) {this.db=db;this.access=access;this.audit=audit;this.calendar=calendar;}
    public Map<String,Object> record(Reading r) {
        return record(r,false);
    }
    public Map<String,Object> recordSettlement(Reading r) {return record(r,true);}
    private Map<String,Object> record(Reading r,boolean settlement) {
        access.manager();var c=access.contract(r.contractId());
        db.one("select id from contracts where id=? for update",r.contractId());
        require("ACTIVE".equals(c.get("status")),"Contract is not active");
        YearMonth month=YearMonth.parse(r.period());LocalDate start=date(c,"start_date"),end=date(c,"end_date");
        require(month.atEndOfMonth().compareTo(start)>=0 && month.atDay(1).isBefore(end),"Period outside contract");
        require(YearMonth.from(r.readingDate()).equals(month),"Reading date must be in billing month");
        require(!r.readingDate().isBefore(start) && r.readingDate().isBefore(end),"Reading date outside contract");
        if(!settlement) require(!r.readingDate().isBefore((LocalDate)calendar.dates(r.contractId(),r.period()).get("cutoffDate")),"Chưa đến ngày chốt số của khu nhà");
        require(db.jdbc.queryForObject("select count(*) from invoices where contract_id=? and billing_period=?",Long.class,r.contractId(),r.period())==0,"Invoice already exists for period");
        require(db.jdbc.queryForObject("select count(*) from meter_readings where contract_id=? and meter_type=? and billing_period>?",Long.class,r.contractId(),r.meterType(),r.period())==0,"Cannot insert a reading before later readings");
        BigDecimal old=latestIndex(c,r.meterType(),r.period());require(r.newIndex().compareTo(old)>=0,"New index must not be below previous index");
        long id=db.insert("meter_readings","room_id,contract_id,billing_period,meter_type,old_index,new_index,consumption,reading_date,recorded_by",
            c.get("room_id"),r.contractId(),r.period(),r.meterType(),old,r.newIndex(),r.newIndex().subtract(old),r.readingDate(),access.userId());
        analyze(id,c);audit.log("CREATE","meter_readings",id,null,r);return db.one("select * from meter_readings where id=?",id);
    }
    public BigDecimal latestIndex(Map<String,Object> c,String type,String beforePeriod) {
        var rows=beforePeriod==null ? db.rows("select new_index from meter_readings where contract_id=? and meter_type=? order by billing_period desc limit 1",id(c,"id"),type)
            : db.rows("select new_index from meter_readings where contract_id=? and meter_type=? and billing_period<? order by billing_period desc limit 1",id(c,"id"),type,beforePeriod);
        return rows.isEmpty()?money(c,type.equals("ELECTRICITY")?"initial_electric_index":"initial_water_index"):money(rows.getFirst(),"new_index");
    }
    public Map<String,Object> edit(long id,BigDecimal newIndex,String reason) {
        access.manager();var reading=db.one("select * from meter_readings where id=? for update",id);var c=access.contract(id(reading,"contract_id"));
        db.one("select id from contracts where id=? for update",id(c,"id"));
        require("ACTIVE".equals(c.get("status")),"Contract no longer active");
        require(db.jdbc.queryForObject("select count(*) from invoices where contract_id=? and billing_period=?",Long.class,id(c,"id"),reading.get("billing_period"))==0,"Reading used by an invoice; cancel draft first");
        require(db.jdbc.queryForObject("select count(*) from meter_readings where contract_id=? and meter_type=? and billing_period>?",Long.class,id(c,"id"),reading.get("meter_type"),reading.get("billing_period"))==0,"Only latest reading can be corrected");
        require(newIndex.compareTo(money(reading,"old_index"))>=0,"New index is below old index");
        db.insert("meter_reading_history","meter_reading_id,old_value,new_value,changed_by,reason",id,reading.get("new_index"),newIndex,access.userId(),reason);
        db.jdbc.update("update meter_readings set new_index=?,consumption=? where id=?",newIndex,newIndex.subtract(money(reading,"old_index")),id);
        db.jdbc.update("delete from consumption_alerts where meter_reading_id=?",id);analyze(id,c);audit.log("CORRECT","meter_readings",id,reading,newIndex);
        return db.one("select * from meter_readings where id=?",id);
    }
    void analyze(long id,Map<String,Object> c) {
        var r=db.one("select * from meter_readings where id=?",id);
        var thresholds=db.rows("select * from alert_thresholds where property_id=? and (room_id=? or room_id is null) and meter_type=? and is_active order by room_id nulls last limit 1",c.get("property_id"),c.get("room_id"),r.get("meter_type"));
        BigDecimal threshold=thresholds.isEmpty()?new BigDecimal("50"):money(thresholds.getFirst(),"threshold_percentage");
        int window=thresholds.isEmpty()?3:((Number)thresholds.getFirst().get("calculation_window_months")).intValue();
        YearMonth period=YearMonth.parse(r.get("billing_period").toString());
        BigDecimal baseline=db.jdbc.queryForObject("select avg(monthly.consumption) from (select sum(consumption) consumption from meter_readings where room_id=? and meter_type=? and billing_period>=? and billing_period<? group by billing_period) monthly",BigDecimal.class,c.get("room_id"),r.get("meter_type"),period.minusMonths(window).toString(),period.toString());
        boolean anomaly=false;
        if(baseline!=null && baseline.signum()>0) {
            BigDecimal percentage=money(r,"consumption").subtract(baseline).multiply(new BigDecimal("100")).divide(baseline,2,RoundingMode.HALF_UP);
            anomaly=percentage.compareTo(threshold)>0;
            if(anomaly) db.insert("consumption_alerts","meter_reading_id,meter_type,baseline_average,current_consumption,percentage_increase,applied_threshold_percentage,alert_severity",
                id,r.get("meter_type"),baseline.setScale(2,RoundingMode.HALF_UP),r.get("consumption"),percentage.min(new BigDecimal("9999.99")),threshold,percentage.compareTo(threshold.multiply(new BigDecimal("2")))>=0?"CRITICAL":"WARNING");
        }
        db.jdbc.update("update meter_readings set has_anomaly=? where id=?",anomaly,id);
    }
    public static LocalDate date(Map<String,Object> row,String key) { Object value=row.get(key);return value instanceof java.sql.Date d?d.toLocalDate():LocalDate.parse(value.toString()); }
}
