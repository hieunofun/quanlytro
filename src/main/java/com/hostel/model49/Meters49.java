package com.hostel.model49;

import com.hostel.service.Database;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;
import static com.hostel.service.Database.*;
import static com.hostel.model49.Input49.*;

@Service @Profile("model49") @Transactional
public class Meters49 {
    private final Database db;private final Access49 access;private final Events49 events;
    public Meters49(Database db,Access49 access,Events49 events){this.db=db;this.access=access;this.events=events;}
    public Map<String,Object> usage(Map<String,Object> m){
        access.manager();long meter=keyId(m,"meterId");var device=db.one("select * from meters where id=?",meter);access.room(com.hostel.service.Database.id(device,"room_id"));
        db.one("select id from rooms where id=? for update",device.get("room_id"));Long responsible=optionalId(m,"contractId");if(responsible!=null){access.contract(responsible);db.one("select id from contracts where id=? for update",responsible);}db.one("select id from meters where id=? for update",meter);
        var from=time(value(m,"startAt"));var to=time(value(m,"endAt"));require(to.isAfter(from)&&!to.isAfter(now()),"Actual start/end measurements required; end cannot be in the future");
        long a=point(meter,from,amount(m,"-1","oldIndex")),b=point(meter,to,amount(m,"-1","newIndex"));
        var body=new HashMap<String,Object>();body.put("startReadingId",a);body.put("endReadingId",b);if(optionalId(m,"contractId")!=null)body.put("contractId",optionalId(m,"contractId"));return segment(body);
    }
    private long point(long meter,OffsetDateTime at,BigDecimal value){var existing=db.rows("select r.* from meter_readings r where meter_id=? and observed_at=? and not exists(select 1 from meter_readings n where n.supersedes_reading_id=r.id)",meter,at);if(!existing.isEmpty()){require(money(existing.getFirst(),"reading_value").compareTo(value)==0,"Existing point differs; use an explicit reading revision");return com.hostel.service.Database.id(existing.getFirst(),"id");}return com.hostel.service.Database.id((Map<String,Object>)reading(Map.of("meterId",meter,"observedAt",at,"readingValue",value)),"id");}
    public Object reading(Map<String,Object> m){
        access.manager();long meter=keyId(m,"meterId");var device=db.one("select * from meters where id=? for update",meter);access.room(com.hostel.service.Database.id(device,"room_id"));
        var observed=time(value(m,"observedAt"));BigDecimal v=amount(m,"-1","readingValue");require(v.signum()>=0&&v.stripTrailingZeros().scale()<=3,"Nonnegative reading with at most three decimal places is required");
        require(!observed.isAfter(now())&&!observed.isBefore(time(device.get("installed_at")))&&(device.get("retired_at")==null||!observed.isAfter(time(device.get("retired_at")))),"Measurement must be within the meter lifetime and cannot be in the future");
        Long previous=optionalId(m,"supersedesReadingId");int revision=1;
        if(previous!=null){var old=db.one("select * from meter_readings where id=?",previous);require(com.hostel.service.Database.id(old,"meter_id")==meter&&time(old.get("observed_at")).toInstant().equals(observed.toInstant()),"Revision must describe the same meter/time");required(m,"reason");require(db.jdbc.queryForObject("select count(*) from consumption_segments s join billing_items i on i.segment_id=s.id join billing_documents d on d.id=i.document_id where (s.start_reading_id=? or s.end_reading_id=?) and d.status='ISSUED'",Long.class,previous,previous)==0,"Issued measurement needs an explicit adjustment workflow");revision=((Number)old.get("revision_no")).intValue()+1;}
        var before=db.rows("select * from meter_readings r where meter_id=? and observed_at<? and not exists(select 1 from meter_readings n where n.supersedes_reading_id=r.id) order by observed_at desc limit 1",meter,observed);
        require(before.isEmpty()||money(before.getFirst(),"reading_value").compareTo(v)<=0,"Reading decreased on the same meter; declare replacement/reset separately");
        var after=db.rows("select * from meter_readings r where meter_id=? and observed_at>? and not exists(select 1 from meter_readings n where n.supersedes_reading_id=r.id) order by observed_at limit 1",meter,observed);
        require(after.isEmpty()||money(after.getFirst(),"reading_value").compareTo(v)>=0,"Reading exceeds a later measurement; reconcile the dependent readings explicitly");
        long id=db.insert("meter_readings","meter_id,observed_at,reading_value,revision_no,supersedes_reading_id,handover_id,recorded_by,reason",meter,observed,v,revision,previous,optionalId(m,"handoverId"),access.user(),text(m,null,"reason"));
        events.audit("MEASURE","meter_readings",id,null,db.one("select * from meter_readings where id=?",id),text(m,"Actual reported measurement","reason"));return db.one("select * from meter_readings where id=?",id);
    }
    public Map<String,Object> segment(Map<String,Object> m){
        access.manager();long start=keyId(m,"startReadingId"),end=keyId(m,"endReadingId");var meter=db.one("select m.* from meters m join meter_readings r on r.meter_id=m.id where r.id=?",start);access.room(com.hostel.service.Database.id(meter,"room_id"));Long contract=optionalId(m,"contractId");db.one("select id from rooms where id=? for update",meter.get("room_id"));if(contract!=null){access.contract(contract);db.one("select id from contracts where id=? for update",contract);}db.one("select id from meters where id=? for update",meter.get("id"));
        long id=db.insert("consumption_segments","start_reading_id,end_reading_id,contract_id,responsibility,supersedes_segment_id,recorded_by",start,end,contract,contract==null?"OWNER":"TENANT",optionalId(m,"supersedesSegmentId"),access.user());analyze(id);return db.one("select * from v_segments where id=?",id);
    }
    @SuppressWarnings("unchecked") public Map<String,Object> revise(long id,Map<String,Object> m){var s=db.one("select * from v_segments where id=?",id);access.manager();access.room(com.hostel.service.Database.id(s,"room_id"));require(db.rows("select id from billing_items where segment_id=?",id).isEmpty(),"Remove unissued draft first; issued charge requires a linked adjustment");var revised=(Map<String,Object>)reading(Map.of("meterId",s.get("meter_id"),"observedAt",s.get("service_end"),"supersedesReadingId",s.get("end_reading_id"),"readingValue",amount(m,"-1","newIndex"),"reason",required(m,"reason")));var body=new HashMap<String,Object>();body.put("startReadingId",s.get("start_reading_id"));body.put("endReadingId",revised.get("id"));body.put("supersedesSegmentId",id);if(s.get("contract_id")!=null)body.put("contractId",s.get("contract_id"));return segment(body);}
    public Object replace(long id,Map<String,Object> m){
        access.manager();var old=db.one("select * from meters where id=? for update",id);access.room(com.hostel.service.Database.id(old,"room_id"));require(old.get("retired_at")==null,"Meter already replaced");var at=time(value(m,"replacedAt"));
        var close=new HashMap<String,Object>();close.put("meterId",id);close.put("observedAt",at);close.put("readingValue",amount(m,"-1","closingIndex"));close.put("reason",required(m,"reason"));var closing=reading(close);
        db.jdbc.update("update meters set retired_at=? where id=?",at,id);
        long fresh=db.insert("meters","room_id,service_item_id,meter_code,installed_at,recorded_by",old.get("room_id"),old.get("service_item_id"),required(m,"newMeterCode"),at,access.user());
        var opening=new HashMap<String,Object>();opening.put("meterId",fresh);opening.put("observedAt",at);opening.put("readingValue",amount(m,"-1","openingIndex"));opening.put("reason",required(m,"reason"));
        return Map.of("oldClosing",closing,"newMeter",db.one("select * from meters where id=?",fresh),"newOpening",reading(opening));
    }
    private void analyze(long id){
        var s=db.one("select * from v_segments where id=?",id);BigDecimal days=money(s,"duration_days");
        var policies=db.rows("select * from alert_policies where service_item_id=? and effective_from<=?::date and (room_id=? or property_id=?) order by (room_id is not null) desc,effective_from desc limit 1",s.get("service_item_id"),time(s.get("service_end")).toLocalDate(),s.get("room_id"),s.get("property_id"));if(policies.isEmpty())return;var p=policies.getFirst();
        var history=db.rows("select h.* from v_segments h where h.room_id=? and h.service_item_id=? and h.service_end<=? and h.service_end>? and h.id<>? and not exists(select 1 from consumption_segments n where n.supersedes_segment_id=h.id) order by h.service_end",s.get("room_id"),s.get("service_item_id"),s.get("service_start"),time(s.get("service_start")).minusMonths(((Number)p.get("history_months")).intValue()),id);
        Set<YearMonth> periods=new HashSet<>();BigDecimal total=BigDecimal.ZERO,duration=BigDecimal.ZERO;boolean sameTenancy=true;
        for(var h:history){periods.add(YearMonth.from(time(h.get("service_end")).minusSeconds(1)));total=total.add(money(h,"consumption"));duration=duration.add(money(h,"duration_days"));if(!Objects.equals(s.get("contract_id"),h.get("contract_id")))sameTenancy=false;}
        String status=null;BigDecimal average=duration.signum()>0?total.divide(duration,6,RoundingMode.HALF_UP):null;
        if(days.compareTo(BigDecimal.valueOf(((Number)p.get("minimum_days")).longValue()))<0||periods.size()<((Number)p.get("history_months")).intValue()||average==null)status="INSUFFICIENT_HISTORY";
        else if(!sameTenancy)status="INCOMPARABLE";
        else {BigDecimal current=money(s,"consumption").divide(days,6,RoundingMode.HALF_UP);if(average.signum()==0){if(p.get("absolute_daily_threshold")!=null&&current.compareTo(money(p,"absolute_daily_threshold"))>0)status="PENDING";}
            else if(current.subtract(average).multiply(new BigDecimal("100")).compareTo(average.multiply(money(p,"threshold_percentage")))>0)status="PENDING";}
        if(status==null)return;
        long alert=db.insert("consumption_alerts","segment_id,policy_id,baseline_daily_average,applied_threshold,status,context_note",id,p.get("id"),average,p.get("threshold_percentage"),status,sameTenancy?"Measured daily comparison; occupancy changes still require human review":"Different tenant group: historical comparison is contextual");
        for(var h:history)db.insert("alert_baseline_sources","alert_id,source_segment_id",alert,h.get("id"));
    }
}
