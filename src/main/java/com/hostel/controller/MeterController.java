package com.hostel.controller;
import com.hostel.dto.Requests.*;
import com.hostel.security.Access;
import com.hostel.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;
import static com.hostel.service.Database.*;
@RestController @RequestMapping("/api")
public class MeterController {
 private final Database db;private final Access access;private final MeterService meters;
 public MeterController(Database db,Access access,MeterService meters){this.db=db;this.access=access;this.meters=meters;}
 @GetMapping("/meter-readings") public Object list(){access.manager();return db.rows("select m.*,case m.service_code when 'ELEC' then 'ELECTRICITY' else m.service_code end meter_type,r.room_code,exists(select 1 from consumption_alerts a where a.meter_reading_id=m.id) has_anomaly from v_meter_consumption m join rooms r on r.id=m.room_id where "+access.roomScope("r.id")+" order by m.billing_period desc,m.id desc");}
 @PostMapping("/meter-readings") public Object record(@Valid @RequestBody Reading r){return meters.record(r);}
 @PostMapping("/meter-readings/settlement") public Object settlement(@Valid @RequestBody Reading r){return meters.recordSettlement(r);}
 public record Correction(@NotNull @DecimalMin("0") BigDecimal newIndex,@NotBlank String reason){}
 @PutMapping("/meter-readings/{id}") public Object edit(@PathVariable long id,@Valid @RequestBody Correction r){return meters.edit(id,r.newIndex(),r.reason());}
 @GetMapping("/alerts") public Object alerts(){access.manager();return db.rows("select a.*,m.billing_period,m.service_code meter_type,r.room_code from v_consumption_alerts a join v_meter_consumption m on m.id=a.meter_reading_id join rooms r on r.id=m.room_id where "+access.roomScope("r.id")+" order by a.id desc");}
 @PutMapping("/alerts/{id}") @Transactional public Object confirm(@PathVariable long id,@Valid @RequestBody Alert r){access.manager();var a=db.one("select * from v_consumption_alerts where id=?",id);access.contract(id(a,"contract_id"));db.jdbc.update("update consumption_alerts set status=?,checked_by=?,checked_at=current_timestamp,note=? where id=?",r.status(),access.userId(),r.note(),id);return Map.of("id",id);}
}
