package com.hostel.controller;
import com.hostel.dto.Requests.*;
import com.hostel.security.Access;
import com.hostel.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static com.hostel.service.Database.*;
@RestController
@RequestMapping("/api")
@Transactional
public class CatalogController {
    private final Database db; private final Access access; private final Audit audit; private final PasswordEncoder encoder;
    public CatalogController(Database db,Access access,Audit audit,PasswordEncoder encoder) { this.db=db;this.access=access;this.audit=audit;this.encoder=encoder; }
    private Map<String,Object> created(String table,long id) { audit.log("CREATE",table,id,null,"Created");return Map.of("id",id); }
    @GetMapping("/users") public Object users() { access.admin();return db.rows("select id,username,full_name,phone,email,role,status,locked_until from users order by id"); }
    @PostMapping("/users") public Object user(@Valid @RequestBody User r) { access.admin();return created("users",db.insert("users","username,password_hash,full_name,phone,email,role",r.username(),encoder.encode(r.password()),r.fullName(),r.phone(),r.email(),r.role())); }
    public record UserStatus(@NotNull @Pattern(regexp="ACTIVE|LOCKED|INACTIVE") String status) {}
    @PutMapping("/users/{id}/status") public Object userStatus(@PathVariable long id,@Valid @RequestBody UserStatus r) {
        access.admin(); require(id!=access.userId(),"Cannot lock your own account"); var before=db.one("select status from users where id=?",id);
        db.jdbc.update("update users set status=?,failed_attempts=0,locked_until=null,token_version=token_version+1 where id=?",r.status(),id);
        db.jdbc.update("delete from auth_sessions where user_id=?",id);audit.log("STATUS","users",id,before,r.status());return Map.of("id",id);
    }
    public record Assignment(@NotNull @Positive Long userId,@NotNull @Positive Long propertyId,Long roomId,boolean active) {}
    @GetMapping("/assignments") public Object assignments() { access.admin();return db.rows("select id,user_id,property_id,null::bigint room_id,is_active from employee_assignments union all select e.id,e.user_id,r.property_id,e.room_id,e.is_active from room_employee_assignments e join rooms r on r.id=e.room_id order by user_id,property_id,room_id"); }
    @PostMapping("/assignments") public Object assignment(@Valid @RequestBody Assignment r) {
        access.admin();require("STAFF".equals(db.one("select role from users where id=?",r.userId()).get("role")),"Only staff can be assigned");
        if(r.roomId()!=null) {require(id(db.one("select property_id from rooms where id=?",r.roomId()),"property_id")==r.propertyId(),"Room and property do not match");db.jdbc.update("insert into room_employee_assignments(user_id,room_id,is_active) values (?,?,?) on conflict(user_id,room_id) do update set is_active=excluded.is_active",r.userId(),r.roomId(),r.active());} else db.jdbc.update("insert into employee_assignments(user_id,property_id,is_active) values (?,?,?) on conflict(user_id,property_id) do update set is_active=excluded.is_active",r.userId(),r.propertyId(),r.active());
        audit.log("ASSIGN","properties",r.propertyId(),null,r);return Map.of("message","Assignment saved");
    }
    @GetMapping("/properties") public Object properties() { access.manager();return db.rows("select p.*,("+access.propertyScope("p.id")+") can_manage_shared from properties p where "+access.propertyReadScope("p.id")+" order by p.id"); }
    @PostMapping("/properties") public Object property(@Valid @RequestBody Property r) { access.admin();return created("properties",db.insert("properties","property_code,name,address",r.code(),r.name(),r.address())); }
    @PutMapping("/properties/{id}") public Object propertyUpdate(@PathVariable long id,@Valid @RequestBody Property r) { access.admin();var before=db.one("select * from properties where id=?",id);db.jdbc.update("update properties set property_code=?,name=?,address=?,updated_at=current_timestamp where id=?",r.code(),r.name(),r.address(),id);audit.log("UPDATE","properties",id,before,r);return Map.of("id",id); }
    @GetMapping("/floors") public Object floors() { access.manager();return db.rows("select * from floors where id in (select floor_id from rooms where "+access.roomScope("id")+") or "+access.propertyScope("property_id")+" order by property_id,floor_number"); }
    @PostMapping("/floors") public Object floor(@Valid @RequestBody Floor r) { access.property(r.propertyId());return created("floors",db.insert("floors","property_id,floor_number,name",r.propertyId(),r.number(),r.name())); }
    @GetMapping("/rooms") public Object rooms() {
        if(access.role().equals("TENANT")) return db.rows("select r.id,r.room_code,r.base_price,r.max_occupants,r.area_sqm,r.status,r.amenities,p.name property_name,p.address from rooms r join properties p on p.id=r.property_id where r.id in (select c.room_id from contracts c where "+access.contractScope("c")+") order by r.id");
        return db.rows("select r.*,p.name property_name from rooms r join properties p on p.id=r.property_id where "+access.roomScope("r.id")+" order by r.id");
    }
    @PostMapping("/rooms") public Object room(@Valid @RequestBody Room r) { access.property(r.propertyId());return created("rooms",db.insert("rooms","property_id,floor_id,room_code,base_price,max_occupants,area_sqm",r.propertyId(),r.floorId(),r.code(),r.price(),r.maxOccupants(),r.area())); }
    @PutMapping("/rooms/{id}") public Object roomUpdate(@PathVariable long id,@Valid @RequestBody Room r) {
        var before=db.one("select * from rooms where id=? for update",id);access.manager();access.room(id);
        require(id(before,"property_id")==r.propertyId(),"Moving rooms between properties is not supported");
        // Check peak occupancy from today onwards, including future member arrivals.
        // Historical departures do not consume today's capacity.
        Integer occupancy=db.jdbc.queryForObject("""
            select coalesce(max(1+(select count(*) from contract_members m
                where m.contract_id=c.id and m.effective_from<=d.occupancy_date
                and (m.effective_to is null or m.effective_to>d.occupancy_date))),0)::int
            from contracts c cross join lateral (
                select greatest(c.start_date,current_date) as occupancy_date
                union select m.effective_from from contract_members m
                    where m.contract_id=c.id and m.effective_from>=current_date
            ) d where c.room_id=? and c.status='ACTIVE'
                and d.occupancy_date<coalesce(c.actual_checkout_date,c.end_date)
            """,Integer.class,id);
        require(r.maxOccupants()>=occupancy,"Capacity is below active occupancy");
        db.jdbc.update("update rooms set floor_id=?,room_code=?,base_price=?,max_occupants=?,area_sqm=?,updated_at=current_timestamp where id=?",r.floorId(),r.code(),r.price(),r.maxOccupants(),r.area(),id);
        if(money(before,"base_price").compareTo(r.price())!=0) db.insert("room_price_history","room_id,old_price,new_price,changed_by",id,before.get("base_price"),r.price(),access.userId());
        audit.log("UPDATE","rooms",id,before,r);return Map.of("id",id);
    }
    public record RoomDetails(@NotNull @Pattern(regexp="AVAILABLE|RENTED|MAINTENANCE") String status,@NotNull String amenities,@NotNull String internalNote,List<@NotBlank String> imageUrls) {}
    @PutMapping("/rooms/{id}/details") public Object roomDetails(@PathVariable long id,@Valid @RequestBody RoomDetails r) {
        var before=db.one("select * from rooms where id=? for update",id);access.manager();access.room(id);
        boolean occupied=db.jdbc.queryForObject("select count(*) from contracts where room_id=? and status='ACTIVE'",Long.class,id)>0;
        require(occupied?r.status().equals("RENTED"):!r.status().equals("RENTED"),"Rented room status is managed by its contract");
        db.jdbc.update("update rooms set status=?,amenities=?,internal_note=? where id=?",r.status(),r.amenities(),r.internalNote(),id);
        db.jdbc.update("delete from room_images where room_id=?",id);
        if(r.imageUrls()!=null) for(String url:r.imageUrls()) { validateRoomImage(id,url); db.insert("room_images","room_id,url",id,url); }
        audit.log("UPDATE_DETAILS","rooms",id,before,r);return Map.of("id",id);
    }
    @GetMapping("/rooms/{id}/images") public Object images(@PathVariable long id) {
        var room=db.one("select property_id from rooms where id=?",id);
        if(access.role().equals("TENANT")) require(db.jdbc.queryForObject("select count(*) from contracts c where room_id=? and "+access.contractScope("c"),Long.class,id)>0,"Room is not yours");else access.room(id);
        return db.rows("select * from room_images where room_id=?",id);
    }
    @GetMapping("/rooms/{id}/price-history") public Object priceHistory(@PathVariable long id) { access.manager();access.room(id);return db.rows("select * from room_price_history where room_id=? order by changed_at",id); }
    private void validateRoomImage(long roomId,String url) {
        if(url.matches("https?://[^\\s]+")) return;
        require(url.matches("/api/uploads/[a-f0-9-]{36}"),"Invalid image URL");
        require(db.jdbc.queryForObject("select count(*) from uploaded_images where id=?::uuid and room_id=?",Long.class,url.substring(13),roomId)>0,"Image belongs to another room");
    }
    @GetMapping("/rooms/{id}/assignees") public Object roomAssignees(@PathVariable long id) {
        access.manager();access.room(id);return db.rows("select u.id,u.full_name from users u where u.status='ACTIVE' and (u.role='ADMIN' or u.role='STAFF' and (exists(select 1 from employee_assignments e join rooms r on r.property_id=e.property_id where e.user_id=u.id and r.id=? and e.is_active) or exists(select 1 from room_employee_assignments e where e.user_id=u.id and e.room_id=? and e.is_active))) order by u.id",id,id);
    }
    @GetMapping("/tenants") public Object tenants() {
        access.manager();String scope=access.role().equals("ADMIN")?"true":"t.id in (select c.primary_tenant_id from contracts c join rooms r on r.id=c.room_id where "+access.roomScope("r.id")+") or t.id in (select resource_id from audit_logs where resource_type='tenant_profiles' and action='CREATE' and user_id="+access.userId()+")";
        return db.rows("select t.* from tenant_profiles t where "+scope+" order by t.id");
    }
    @PostMapping("/tenants") public Object tenant(@Valid @RequestBody Tenant r) { access.manager();checkTenantUser(r.userId());return created("tenant_profiles",db.insert("tenant_profiles","user_id,full_name,identity_number,phone,email",r.userId(),r.fullName(),r.identityNumber(),r.phone(),r.email())); }
    void checkTenantUser(Long userId) { if(userId!=null) require("TENANT".equals(db.one("select role from users where id=?",userId).get("role")),"Profile account must have TENANT role"); }
    void tenantAccess(long id) {
        access.manager(); if(access.role().equals("ADMIN")) return;
        require(db.jdbc.queryForObject("select count(*) from tenant_profiles t where t.id=? and (t.id in (select c.primary_tenant_id from contracts c join rooms r on r.id=c.room_id where "+access.roomScope("r.id")+") or t.id in (select resource_id from audit_logs where resource_type='tenant_profiles' and action='CREATE' and user_id="+access.userId()+"))",Long.class,id)>0,"Tenant outside assignment");
        require(db.jdbc.queryForObject("select count(*) from contracts c join rooms r on r.id=c.room_id where c.primary_tenant_id=? and c.status='ACTIVE' and not ("+access.roomScope("r.id")+")",Long.class,id)==0,"Tenant also belongs to another assigned area; ask owner to edit");
    }
    @PutMapping("/tenants/{id}") public Object tenantUpdate(@PathVariable long id,@Valid @RequestBody Tenant r) {
        tenantAccess(id);checkTenantUser(r.userId());var before=db.one("select * from tenant_profiles where id=?",id);
        db.jdbc.update("update tenant_profiles set user_id=?,full_name=?,identity_number=?,phone=?,email=?,updated_at=current_timestamp where id=?",r.userId(),r.fullName(),r.identityNumber(),r.phone(),r.email(),id);audit.log("UPDATE","tenant_profiles",id,before,r);return Map.of("id",id);
    }
    public record Profile(LocalDate dateOfBirth,@Size(max=150) String hometown) {}
    @PutMapping("/tenants/{id}/profile") public Object profile(@PathVariable long id,@Valid @RequestBody Profile r) { tenantAccess(id);db.jdbc.update("update tenant_profiles set date_of_birth=?,hometown=? where id=?",r.dateOfBirth(),r.hometown(),id);audit.log("PROFILE","tenant_profiles",id,null,r);return Map.of("id",id); }
    @GetMapping("/services") public Object services() { access.manager();return db.rows("select * from service_items order by id"); }
    public record Service(@NotBlank @Size(max=30) @Pattern(regexp="[A-Z0-9_]+") String code,@NotBlank @Size(max=100) String name,@NotNull @Pattern(regexp="PER_ROOM|PER_PERSON") String calculationType,@NotBlank @Size(max=30) String unitName) {}
    @PostMapping("/services") public Object service(@Valid @RequestBody Service r) {
        access.admin();return created("service_items",db.insert("service_items","service_code,name,service_type,calculation_type,unit_name,is_mandatory",r.code(),r.name(),"FIXED_SERVICE",r.calculationType(),r.unitName(),false));
    }
    public record Policy(@Min(1) @Max(31) int meterCutoffDay,@Min(1) @Max(28) int invoiceIssueDay,boolean lateFeeEnabled,@NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal lateFeePercentage) {}
    @PutMapping("/properties/{id}/billing-policy") public Object policy(@PathVariable long id,@Valid @RequestBody Policy r) {access.admin();var before=db.one("select * from properties where id=?",id);db.jdbc.update("update properties set meter_cutoff_day=?,invoice_issue_day=?,late_fee_enabled=?,late_fee_percentage=? where id=?",r.meterCutoffDay(),r.invoiceIssueDay(),r.lateFeeEnabled(),r.lateFeePercentage(),id);audit.log("BILLING_POLICY","properties",id,before,r);return Map.of("id",id);}
    @GetMapping("/properties/{id}/assignees") public Object assignees(@PathVariable long id) {access.property(id);return db.rows("select u.id,u.full_name from users u where status='ACTIVE' and (role='ADMIN' or role='STAFF' and exists(select 1 from employee_assignments e where e.user_id=u.id and e.property_id=? and e.is_active)) order by u.id",id);}
    @GetMapping("/prices") public Object prices() { access.manager();return db.rows("select p.*,s.name,s.service_type,s.calculation_type from service_price_configs p join service_items s on s.id=p.service_item_id where "+access.propertyReadScope("p.property_id")+" order by p.effective_from desc"); }
    @PostMapping("/prices") public Object price(@Valid @RequestBody Price r) { access.property(r.propertyId());return created("service_price_configs",db.insert("service_price_configs","property_id,service_item_id,unit_price,effective_from",r.propertyId(),r.serviceId(),r.unitPrice(),r.effectiveFrom())); }
    @GetMapping("/thresholds") public Object thresholds() { access.manager();return db.rows("select * from alert_thresholds where (room_id is null and "+access.propertyReadScope("property_id")+" or room_id is not null and "+access.roomScope("room_id")+") order by id"); }
    @PostMapping("/thresholds") public Object threshold(@Valid @RequestBody Threshold r) {
        access.manager();if(r.roomId()==null) access.property(r.propertyId());else access.room(r.roomId());if(r.roomId()!=null) require(id(db.one("select property_id from rooms where id=?",r.roomId()),"property_id")==r.propertyId(),"Room and property do not match");
        var old=db.rows("select id from alert_thresholds where property_id=? and room_id is not distinct from ? and meter_type=?",r.propertyId(),r.roomId(),r.meterType());
        long id;
        if(old.isEmpty()) id=db.insert("alert_thresholds","property_id,room_id,meter_type,threshold_percentage,calculation_window_months",r.propertyId(),r.roomId(),r.meterType(),r.percentage(),r.months());
        else { id=id(old.getFirst(),"id");db.jdbc.update("update alert_thresholds set threshold_percentage=?,calculation_window_months=?,is_active=true,updated_at=current_timestamp where id=?",r.percentage(),r.months(),id); }
        audit.log("CONFIGURE","alert_thresholds",id,old,r);return Map.of("id",id);
    }
    @GetMapping("/audit-logs") public Object logs() { access.admin();return db.rows("select a.*,u.full_name from audit_logs a left join users u on u.id=a.user_id order by a.id desc limit 500"); }
}
