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
@RestController @RequestMapping("/api") @Transactional
public class MaintenanceController {
 private final Database db;private final Access access;
 public MaintenanceController(Database db,Access access){this.db=db;this.access=access;}
 public record Request(@NotNull @Positive Long contractId,@NotBlank @Size(max=150) String title,@NotBlank String description,@NotNull @Pattern(regexp="URGENT|NORMAL|LOW") String priority,@Size(max=12) List<@Pattern(regexp="https?://[^\\s]+") String> imageUrls){}
 private Map<String,Object> detail(long id){var m=new LinkedHashMap<>(access.maintenance(id));var images=db.rows("select * from maintenance_images where maintenance_request_id=? order by id",id);m.put("images",images);m.put("image_urls",images.stream().map(x->x.get("url").toString()).reduce((a,c)->a+"\n"+c).orElse(""));return m;}
 @GetMapping("/maintenance-requests") public Object list(){return db.rows("select m.*,c.room_id,r.room_code,coalesce((select string_agg(url,E'\\n' order by id) from maintenance_images where maintenance_request_id=m.id),'') image_urls from maintenance_requests m join contracts c on c.id=m.contract_id join rooms r on r.id=c.room_id where "+access.maintenanceScope("m")+" order by m.id desc");}
 @GetMapping("/maintenance-requests/{id}") public Object get(@PathVariable long id){return detail(id);}
 @PostMapping("/maintenance-requests") public Object create(@Valid @RequestBody Request r){var c=access.contract(r.contractId());require("ACTIVE".equals(c.get("status")),"Request requires an active contract");LocalDate due=LocalDate.now( java.time.ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(r.priority().equals("URGENT")?1:r.priority().equals("NORMAL")?3:7);long id=db.insert("maintenance_requests","contract_id,submitted_by,title,description,priority,expected_date",r.contractId(),access.userId(),r.title(),r.description(),r.priority(),due);if(r.imageUrls()!=null)for(String url:r.imageUrls())db.insert("maintenance_images","maintenance_request_id,url,uploaded_by",id,url,access.userId());return detail(id);}
 public record Progress(@NotNull @Pattern(regexp="ACCEPTED|IN_PROGRESS|COMPLETED") String status,Long assignedTo,@NotNull LocalDate expectedDate,@NotNull @DecimalMin("0") BigDecimal actualCost,String note){}
 @PutMapping("/maintenance-requests/{id}") public Object progress(@PathVariable long id,@Valid @RequestBody Progress r){access.manager();var m=access.maintenance(id);db.one("select id from maintenance_requests where id=? for update",id);if(r.assignedTo()!=null)require(access.canManageRoom(r.assignedTo(),id(m,"room_id")),"Assignee outside property");db.jdbc.update("update maintenance_requests set status=?,assigned_to=?,expected_date=?,actual_cost=?,progress_note=? where id=?",r.status(),r.assignedTo(),r.expectedDate(),r.actualCost(),r.note(),id);return detail(id);}
 public record Image(@NotBlank @Pattern(regexp="https?://[^\\s]+") String url){}
 @PostMapping(value="/maintenance-requests/{id}/images",consumes="application/json") public Object image(@PathVariable long id,@Valid @RequestBody Image r){var m=access.maintenance(id);require(!"COMPLETED".equals(m.get("status")),"Request already completed");db.one("select id from maintenance_requests where id=? for update",id);require(db.jdbc.queryForObject("select count(*) from maintenance_images where maintenance_request_id=?",Long.class,id)<12,"Maximum 12 images");return Map.of("id",db.insert("maintenance_images","maintenance_request_id,url,uploaded_by",id,r.url(),access.userId()));}
 @DeleteMapping("/maintenance-requests/{id}/images/{imageId}") public Object deleteImage(@PathVariable long id,@PathVariable long imageId){access.maintenance(id);db.one("select id from maintenance_images where id=? and maintenance_request_id=?",imageId,id);db.jdbc.update("delete from maintenance_images where id=?",imageId);return Map.of("id",imageId);}
}
