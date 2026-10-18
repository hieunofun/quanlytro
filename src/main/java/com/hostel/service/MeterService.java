package com.hostel.service;
import com.hostel.dto.Requests.Reading;
import com.hostel.security.Access;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;
import static com.hostel.service.Database.*;
@Service @Transactional
public class MeterService {
 private final Database db;private final Access access;private final BillingCalendar calendar;
 public MeterService(Database db,Access access,BillingCalendar calendar){this.db=db;this.access=access;this.calendar=calendar;}
 public static LocalDate period(String p){return YearMonth.parse(p).atDay(1);}
 public static LocalDate date(Map<String,Object> r,String k){Object v=r.get(k);return v instanceof java.sql.Date d?d.toLocalDate():LocalDate.parse(v.toString());}
 public long service(String type){return id(db.one("select id from service_items where service_code=? and calculation_type='METER_BASED'",type.equals("ELECTRICITY")?"ELEC":type),"id");}
 public Map<String,Object> record(Reading r){return record(r,false);}
 public Map<String,Object> recordSettlement(Reading r){return record(r,true);}
 private Map<String,Object> record(Reading r,boolean settlement){
  access.manager();access.contract(r.contractId());var c=db.one("select * from contracts where id=? for update",r.contractId());
  require("ACTIVE".equals(c.get("status"))||settlement && "TERMINATED".equals(c.get("status")),"Contract is not active");
  LocalDate p=period(r.period()),end=c.get("actual_checkout_date")==null?date(c,"end_date"):date(c,"actual_checkout_date");
  require(!r.readingDate().isBefore(date(c,"start_date")) && r.readingDate().isBefore(end) && YearMonth.from(r.readingDate()).equals(YearMonth.from(p)),"Reading date outside occupied period");
  if(!settlement)require(!r.readingDate().isBefore((LocalDate)calendar.dates(r.contractId(),r.period()).get("cutoffDate")),"Chưa đến ngày chốt số");
  require(db.jdbc.queryForObject("select count(*) from invoices where contract_id=? and billing_period=?",Long.class,r.contractId(),p)==0,"Invoice already exists for period");
  long service=service(r.meterType());
  require(db.jdbc.queryForObject("select count(*) from meter_readings where contract_id=? and service_item_id=? and billing_period>?",Long.class,r.contractId(),service,p)==0,"Cannot insert before later readings");
  var previous=db.rows("select new_index from meter_readings where contract_id=? and service_item_id=? and billing_period<? order by billing_period desc limit 1",r.contractId(),service,p);
  BigDecimal old=previous.isEmpty()?r.oldIndex():money(previous.getFirst(),"new_index");
  require(old!=null,"First reading requires oldIndex from the handover record");
  require(r.oldIndex()==null||old.compareTo(r.oldIndex())==0,"Opening index differs from previous closing index");
  require(r.newIndex().compareTo(old)>=0,"New index below opening index");
  long id=db.insert("meter_readings","contract_id,service_item_id,billing_period,old_index,new_index,reading_date,recorded_by",r.contractId(),service,p,old,r.newIndex(),r.readingDate(),access.userId());
  analyze(id);return detail(id);
 }
 public Map<String,Object> detail(long id){return db.one("select v.*,case service_code when 'ELEC' then 'ELECTRICITY' else service_code end meter_type,exists(select 1 from consumption_alerts a where a.meter_reading_id=v.id) has_anomaly from v_meter_consumption v where v.id=?",id);}
 public BigDecimal latestIndex(Map<String,Object> c,String type,String before){var rows=db.rows("select new_index from meter_readings where contract_id=? and service_item_id=?"+(before==null?"":" and billing_period<?")+" order by billing_period desc limit 1",before==null?new Object[]{id(c,"id"),service(type)}:new Object[]{id(c,"id"),service(type),period(before)});require(!rows.isEmpty(),"Record opening and closing meter indices before checkout");return money(rows.getFirst(),"new_index");}
 public Map<String,Object> edit(long id,BigDecimal value,String reason){
  access.manager();var r=db.one("select * from meter_readings where id=?",id);access.contract(id(r,"contract_id"));db.one("select id from contracts where id=? for update",r.get("contract_id"));
  require(value.compareTo(money(r,"old_index"))>=0,"New index below opening index");db.jdbc.update("update meter_readings set new_index=?,recorded_by=? where id=?",value,access.userId(),id);analyze(id);return detail(id);
 }
 private void analyze(long id){var r=detail(id);var p=db.one("select * from properties where id=?",r.get("property_id"));int window=((Number)p.get("alert_history_months")).intValue();LocalDate period=date(r,"billing_period");
  var baseline=db.one("select count(*) months,avg(consumption) average from (select billing_period,sum(consumption) consumption from v_meter_consumption where room_id=? and service_item_id=? and billing_period>=? and billing_period<? group by billing_period) monthly",r.get("room_id"),r.get("service_item_id"),period.minusMonths(window),period);
  if(((Number)baseline.get("months")).intValue()!=window||baseline.get("average")==null)return;
  BigDecimal average=money(baseline,"average").setScale(2,RoundingMode.HALF_UP),threshold=money(p,"alert_threshold_percentage");
  if(average.signum()>0 && money(r,"consumption").subtract(average).multiply(new BigDecimal("100")).compareTo(average.multiply(threshold))>0)db.insert("consumption_alerts","meter_reading_id,baseline_average,applied_threshold_percentage,history_months",id,average,threshold,window);
 }
}
