package com.hostel.controller;
import com.hostel.security.Access;
import com.hostel.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static com.hostel.service.Database.*;
@RestController
@RequestMapping("/api")
@Transactional
public class MaintenanceController {
    private final Database db;private final Access access;private final Audit audit;private final InvoiceService invoices;
    public MaintenanceController(Database db,Access access,Audit audit,InvoiceService invoices) {this.db=db;this.access=access;this.audit=audit;this.invoices=invoices;}
    public record Request(@NotNull @Positive Long contractId,@NotBlank @Size(max=150) String title,@NotBlank String description,
        @NotNull @Pattern(regexp="ELECTRICITY|WATER|AIR_CONDITIONER|LOCK|OTHER") String category,@NotNull @Pattern(regexp="URGENT|NORMAL|LOW") String priority,List<@Pattern(regexp="https?://[^\\s]+") String> imageUrls) {}
    @GetMapping("/maintenance-requests") public Object list() {
        String scope=access.role().equals("TENANT")?"m.submitted_by="+access.userId():access.maintenanceScope("m");
        return db.rows("select m.*,r.room_code from maintenance_requests m left join rooms r on r.id=m.room_id where "+scope+" order by m.id desc");
    }
    @PostMapping("/maintenance-requests") public Object create(@Valid @RequestBody Request r) {
        var c=access.contract(r.contractId());require("ACTIVE".equals(c.get("status")),"Report requires an active contract");
        LocalDate due=LocalDate.now().plusDays(r.priority().equals("URGENT")?1:r.priority().equals("NORMAL")?3:7);
        long id=db.insert("maintenance_requests","contract_id,property_id,room_id,submitted_by,title,description,category,priority,image_urls,expected_date",r.contractId(),c.get("property_id"),c.get("room_id"),access.userId(),r.title(),r.description(),r.category(),r.priority(),r.imageUrls()==null?"":String.join("\n",r.imageUrls()),due);
        audit.log("CREATE","maintenance_requests",id,null,r);return db.one("select * from maintenance_requests where id=?",id);
    }
    public record Progress(@NotNull @Pattern(regexp="ACCEPTED|IN_PROGRESS|COMPLETED") String status,Long assignedTo,@NotNull LocalDate expectedDate,@NotNull @DecimalMin("0") BigDecimal actualCost,String note) {}
    @PutMapping("/maintenance-requests/{id}") public Object progress(@PathVariable long id,@Valid @RequestBody Progress r) {
        var before=db.one("select * from maintenance_requests where id=? for update",id);access.manager();access.maintenance(id);
        var allowed=Map.of("NEW",List.of("ACCEPTED"),"ACCEPTED",List.of("ACCEPTED","IN_PROGRESS"),"IN_PROGRESS",List.of("IN_PROGRESS","COMPLETED"),"COMPLETED",List.<String>of());
        require(allowed.get(before.get("status").toString()).contains(r.status()),"Invalid maintenance state transition");
        if(r.assignedTo()!=null) require(access.canManageRoom(r.assignedTo(),id(before,"room_id")),"Assignee is outside room assignment");
        db.jdbc.update("update maintenance_requests set status=?,assigned_to=?,expected_date=?,actual_cost=?,progress_note=?,updated_at=current_timestamp where id=?",r.status(),r.assignedTo(),r.expectedDate(),r.actualCost(),r.note(),id);
        db.jdbc.update("insert into notifications(user_id,type,message,reference_key) values (?,'MAINTENANCE',?,?) on conflict(user_id,reference_key) do nothing",before.get("submitted_by"),"Yêu cầu “"+before.get("title")+"” chuyển trạng thái "+r.status(),"maintenance:"+id+":"+r.status());
        audit.log("PROGRESS","maintenance_requests",id,before,r);return db.one("select * from maintenance_requests where id=?",id);
    }
    public record Schedule(@NotNull @Positive Long propertyId,@NotBlank @Size(max=150) String title,@NotNull LocalDate nextDate,@Min(1) int intervalDays,String notes) {}
    @GetMapping("/maintenance-schedules") public Object schedules() {access.manager();return db.rows("select * from maintenance_schedules where "+access.propertyScope("property_id")+" order by next_date");}
    @PostMapping("/maintenance-schedules") public Object schedule(@Valid @RequestBody Schedule r) {access.property(r.propertyId());long id=db.insert("maintenance_schedules","property_id,title,next_date,interval_days,notes",r.propertyId(),r.title(),r.nextDate(),r.intervalDays(),r.notes());audit.log("SCHEDULE","maintenance_schedules",id,null,r);return Map.of("id",id);}
    @PostMapping("/maintenance-schedules/{id}/complete") public Object complete(@PathVariable long id) {var s=db.one("select * from maintenance_schedules where id=? for update",id);access.property(id(s,"property_id"));db.jdbc.update("update maintenance_schedules set next_date=greatest(next_date,current_date)+interval_days where id=?",id);audit.log("COMPLETE_SCHEDULE","maintenance_schedules",id,s,"Done");return Map.of("id",id);}
    @GetMapping("/notifications") public Object notifications() {return db.rows("select * from notifications where user_id=? order by id desc limit 200",access.userId());}
    @PutMapping("/notifications/{id}/read") public Object read(@PathVariable long id) {db.jdbc.update("update notifications set is_read=true where id=? and user_id=?",id,access.userId());return Map.of("id",id);}
    public record Announcement(@NotBlank String message) {}
    @PostMapping("/notifications/announce") public Object announce(@Valid @RequestBody Announcement r) {access.admin();db.jdbc.update("insert into notifications(user_id,type,message,reference_key) select id,'GENERAL',?,? from users where status='ACTIVE'",r.message(),"general:"+UUID.randomUUID());audit.log("ANNOUNCE","notifications",0,null,r);return Map.of("message","Announcement sent");}
}
