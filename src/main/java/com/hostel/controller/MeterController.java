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
@RestController
@RequestMapping("/api")
public class MeterController {
    private final Database db;private final Access access;private final MeterService meters;private final Audit audit;
    public MeterController(Database db,Access access,MeterService meters,Audit audit) {this.db=db;this.access=access;this.meters=meters;this.audit=audit;}
    @GetMapping("/meter-readings") public Object list() {access.manager();return db.rows("select m.*,r.room_code from meter_readings m join rooms r on r.id=m.room_id where "+access.roomScope("r.id")+" order by m.billing_period desc,m.id desc");}
    @PostMapping("/meter-readings") public Object record(@Valid @RequestBody Reading r) {return meters.record(r);}
    public record Correction(@NotNull @DecimalMin("0") BigDecimal newIndex,@NotBlank String reason) {}
    @PutMapping("/meter-readings/{id}") public Object edit(@PathVariable long id,@Valid @RequestBody Correction r) {return meters.edit(id,r.newIndex(),r.reason());}
    @GetMapping("/meter-readings/{id}/history") public Object history(@PathVariable long id) {access.manager();access.contract(id(db.one("select contract_id from meter_readings where id=?",id),"contract_id"));return db.rows("select * from meter_reading_history where meter_reading_id=? order by id",id);}
    @GetMapping("/alerts") public Object alerts() {access.manager();return db.rows("select a.*,m.billing_period,m.room_id,r.room_code from consumption_alerts a join meter_readings m on m.id=a.meter_reading_id join rooms r on r.id=m.room_id where "+access.roomScope("r.id")+" order by a.id desc");}
    @PutMapping("/alerts/{id}") @Transactional public Object confirm(@PathVariable long id,@Valid @RequestBody Alert r) {
        access.manager();var alert=db.one("select a.*,m.contract_id from consumption_alerts a join meter_readings m on m.id=a.meter_reading_id where a.id=?",id);access.contract(id(alert,"contract_id"));
        db.jdbc.update("update consumption_alerts set status=?,confirmed_by=?,note=? where id=?",r.status(),access.userId(),r.note(),id);audit.log("REVIEW","consumption_alerts",id,alert,r);return Map.of("id",id);
    }
}
