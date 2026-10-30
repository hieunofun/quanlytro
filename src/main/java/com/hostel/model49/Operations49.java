package com.hostel.model49;

import com.hostel.service.Database;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static com.hostel.service.Database.*;
import static com.hostel.model49.Input49.*;

@Service @Profile("model49") @Transactional
public class Operations49 {
    private final Database db;private final Access49 access;private final Events49 events;
    public Operations49(Database db,Access49 access,Events49 events){this.db=db;this.access=access;this.events=events;}
    public Map<String,Object> detail(long id){var m=new LinkedHashMap<>(access.maintenance(id));var files=db.rows("select a.id,coalesce(f.external_url,'/api/uploads/'||f.storage_key) url from file_attachments a join stored_files f on f.id=a.file_id where a.maintenance_request_id=? order by a.id",id);m.put("images",files);m.put("image_urls",String.join("\n",files.stream().map(f->f.get("url").toString()).toList()));m.put("events",db.rows("select * from maintenance_events where request_id=? order by id",id));if(!access.role().equals("TENANT"))m.put("costs",db.rows("select * from maintenance_costs where request_id=? order by id",id));return m;}
    public Object create(Map<String,Object> m){
        Long room=optionalId(m,"roomId"),property=optionalId(m,"propertyId"),placement=optionalId(m,"assetPlacementId"),contract=optionalId(m,"contractId");
        if(contract!=null){var c=db.one("select c.* from contracts c where c.id=? and "+access.contractScope("c",false),contract);room=com.hostel.service.Database.id(c,"room_id");}
        if(placement!=null){access.manager();var p=db.one("select * from asset_placements where id=?",placement);if(p.get("room_id")!=null)access.room(com.hostel.service.Database.id(p,"room_id"));else access.property(com.hostel.service.Database.id(p,"property_id"));}
        else if(room!=null){access.room(room);if(access.role().equals("TENANT"))require(db.jdbc.queryForObject("select count(*) from contract_residencies r join contracts c on c.id=r.contract_id join tenant_profiles t on t.id=r.tenant_id where c.room_id=? and t.user_id=? and r.effective_from<=current_date and (r.effective_to is null or r.effective_to>current_date)",Long.class,room,access.user())>0,"Only current residents may report for this room");}
        else {require(property!=null,"A physical location is required");access.property(property);}
        String priority=text(m,"NORMAL","priority");LocalDate expected=day(m,LocalDate.now().plusDays(priority.equals("URGENT")?1:priority.equals("NORMAL")?3:7),"expectedDate");
        long id=db.insert("maintenance_requests","room_id,property_id,asset_placement_id,submitted_by,title,description,priority,expected_date,incident_type,schedule_id,scheduled_for",room,property,placement,access.user(),required(m,"title"),required(m,"description"),priority,expected,text(m,"GENERAL","incidentType"),optionalId(m,"scheduleId"),day(m,null,"scheduledFor"));
        db.insert("maintenance_events","request_id,action,actor_id,note",id,"SUBMITTED",access.user(),"Report received");events.audit("REPORT","maintenance_requests",id,null,db.one("select * from maintenance_requests where id=?",id),"Physical location independent of contract");return detail(id);
    }
    public Object progress(long id,Map<String,Object> m){
        access.manager();var old=access.maintenance(id);db.one("select id from maintenance_requests where id=? for update",id);
        String next=text(m,old.get("status").toString(),"status");boolean valid=next.equals(old.get("status"))||switch(old.get("status").toString()){case "NEW"->Set.of("ACCEPTED","CANCELLED").contains(next);case "ACCEPTED"->Set.of("IN_PROGRESS","CANCELLED").contains(next);case "IN_PROGRESS"->Set.of("COMPLETED","CANCELLED").contains(next);case "COMPLETED"->"IN_PROGRESS".equals(next);default->false;};require(valid,"Invalid maintenance state transition");
        Long assigned=optionalId(m,"assignedTo");if(assigned!=null){boolean allowed=old.get("resolved_room_id")!=null?Boolean.TRUE.equals(db.jdbc.queryForObject("select can_manage_room(?,?)",Boolean.class,assigned,old.get("resolved_room_id"))):Boolean.TRUE.equals(db.jdbc.queryForObject("select can_manage_property(?,?)",Boolean.class,assigned,old.get("resolved_property_id")));require(allowed,"Assignee is outside this physical location");}
        db.jdbc.update("update maintenance_requests set status=?,assigned_to=?,expected_date=? where id=?",next,assigned,day(m,date(old.get("expected_date")),"expectedDate"),id);
        db.insert("maintenance_events","request_id,action,actor_id,note",id,next,access.user(),text(m,"","note"));
        if(value(m,"actualCost")!=null){BigDecimal total=amount(m,"0","actualCost");BigDecimal previous=money(old,"actual_cost");require(total.compareTo(previous)>=0,"Cost reductions need an explicit correction; do not erase previous cost records");if(total.compareTo(previous)>0)cost(id,Map.of("description",text(m,"Recorded repair expense","note"),"amount",total.subtract(previous),"incurredOn",LocalDate.now()));}
        events.audit("PROGRESS","maintenance_requests",id,old,db.one("select * from maintenance_requests where id=?",id),text(m,"","note"));return detail(id);
    }
    public Object cost(long id,Map<String,Object> m){access.manager();access.maintenance(id);long row=db.insert("maintenance_costs","request_id,description,amount,incurred_on,advanced_by_tenant_id,recorded_by",id,required(m,"description"),amount(m,"-1","amount"),day(m,LocalDate.now(),"incurredOn"),optionalId(m,"advancedByTenantId"),access.user());return db.one("select * from maintenance_costs where id=?",row);}
    public Object schedule(long id,Map<String,Object> m){
        access.manager();var s=db.one("select * from maintenance_schedules where id=? for update",id);require(Boolean.TRUE.equals(s.get("is_active")),"Schedule inactive");LocalDate due=date(s.get("next_due"));
        var body=new HashMap<String,Object>();body.put("title",s.get("title"));body.put("description","Scheduled maintenance: "+s.get("title"));body.put("scheduleId",id);body.put("scheduledFor",due);body.put("expectedDate",due);
        if(s.get("room_id")!=null)body.put("roomId",s.get("room_id"));else if(s.get("property_id")!=null)body.put("propertyId",s.get("property_id"));else {var p=db.one("select id from asset_placements where asset_id=? and valid_to is null",s.get("asset_id"));body.put("assetPlacementId",p.get("id"));}
        var existing=db.rows("select id from maintenance_requests where schedule_id=? and scheduled_for=?",id,due);Object result=existing.isEmpty()?create(body):detail(com.hostel.service.Database.id(existing.getFirst(),"id"));
        db.jdbc.update("update maintenance_schedules set next_due=? where id=?",due.plusDays(((Number)s.get("interval_days")).longValue()),id);return result;
    }
}
