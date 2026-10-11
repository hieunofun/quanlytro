package com.hostel.controller;
import com.hostel.dto.Requests.*;
import com.hostel.security.Access;
import com.hostel.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
import static com.hostel.service.Database.*;
@RestController
@RequestMapping("/api/contracts")
public class ContractController {
    private final Database db;private final Access access;private final ContractService contracts;private final Audit audit;private final BillingCalendar calendar;
    public ContractController(Database db,Access access,ContractService contracts,Audit audit,BillingCalendar calendar) {this.db=db;this.access=access;this.contracts=contracts;this.audit=audit;this.calendar=calendar;}
    @GetMapping public Object list() {return db.rows("select c.*,r.room_code,t.full_name from contracts c join rooms r on r.id=c.room_id join tenant_profiles t on t.id=c.primary_tenant_id where "+access.contractScope("c")+" order by c.id desc");}
    @GetMapping("/{id}") public Object detail(@PathVariable long id) {return contracts.detail(id);}
    @GetMapping("/{id}/billing-calendar") public Object calendar(@PathVariable long id,@RequestParam String period) {access.contract(id);return calendar.dates(id,period);}
    public record Violation(@NotBlank @Size(max=4000) String reason) {}
    public record Resolution(@NotNull @Pattern(regexp="HANDLING|RESOLVED|DISMISSED") String status,@NotBlank @Size(max=4000) String note) {}
    @PostMapping("/{id}/violations") @Transactional public Object violation(@PathVariable long id,@Valid @RequestBody Violation r) {
        access.manager();var c=access.contract(id);db.one("select id from contracts where id=? for update",id);
        require("ACTIVE".equals(c.get("status")),"Chỉ ghi vi phạm cho hợp đồng đang hiệu lực");
        long v=db.insert("contract_violations","contract_id,reason,recorded_by",id,r.reason(),access.userId());
        audit.log("VIOLATION_OPEN","contracts",id,null,r);return db.one("select * from contract_violations where id=?",v);
    }
    @PutMapping("/{id}/violations/{violationId}") @Transactional public Object resolve(@PathVariable long id,@PathVariable long violationId,@Valid @RequestBody Resolution r) {
        access.manager();access.contract(id);var before=db.one("select * from contract_violations where id=? and contract_id=? for update",violationId,id);
        require(List.of("OPEN","HANDLING").contains(before.get("status")),"Vi phạm đã đóng");
        db.jdbc.update("update contract_violations set status=?,resolution_note=?,resolved_by=?,updated_at=current_timestamp where id=?",r.status(),r.note(),access.userId(),violationId);
        audit.log("VIOLATION_"+r.status(),"contracts",id,before,r);return db.one("select * from contract_violations where id=?",violationId);
    }
    @PostMapping public Object create(@Valid @RequestBody Contract r) {return contracts.create(r);}
    @PostMapping("/{id}/members") public Object member(@PathVariable long id,@Valid @RequestBody Member r) {return contracts.member(id,r);}
    public record Renewal(@NotNull LocalDate endDate) {}
    public record Departure(@NotNull LocalDate effectiveTo) {}
    @PostMapping("/{id}/members/{memberId}/depart") @Transactional public Object depart(@PathVariable long id,@PathVariable long memberId,@Valid @RequestBody Departure r) {
        access.manager();access.contract(id);var c=db.one("select * from contracts where id=? for update",id);var member=db.one("select * from contract_members where id=? and contract_id=?",memberId,id);
        require("ACTIVE".equals(c.get("status")),"Chỉ ghi nhận rời phòng cho hợp đồng đang hiệu lực");
        require(member.get("effective_to")==null,"Thành viên đã được ghi nhận rời phòng");
        require(r.effectiveTo().isAfter(com.hostel.service.MeterService.date(member,"effective_from")) && !r.effectiveTo().isAfter(com.hostel.service.MeterService.date(c,"end_date")),"Invalid departure date");
        require(db.jdbc.queryForObject("select count(*) from invoices where contract_id=? and billing_period>=?",Long.class,id,java.time.YearMonth.from(r.effectiveTo()).toString())==0,"Departure cannot affect an existing invoice");
        db.jdbc.update("update contract_members set effective_to=? where id=?",r.effectiveTo(),memberId);audit.log("MEMBER_DEPART","contracts",id,member,r);return Map.of("id",memberId);
    }
    @PostMapping("/{id}/renew") public Object renew(@PathVariable long id,@Valid @RequestBody Renewal r) {return contracts.renew(id,r.endDate());}
    @PostMapping("/{id}/checkout") public Object checkout(@PathVariable long id,@Valid @RequestBody Checkout r) {return contracts.checkout(id,r);}
    @GetMapping("/{id}/checkout") public Object record(@PathVariable long id) {access.contract(id);return db.one("select * from checkout_records where contract_id=?",id);}
    public record Notes(@NotNull String terms,String violationNote) {}
    @PutMapping("/{id}/notes") @Transactional public Object notes(@PathVariable long id,@Valid @RequestBody Notes r) {access.manager();var c=access.contract(id);db.jdbc.update("update contracts set terms=?,violation_note=? where id=?",r.terms(),r.violationNote(),id);audit.log("NOTES","contracts",id,c,r);return Map.of("id",id);}
}
