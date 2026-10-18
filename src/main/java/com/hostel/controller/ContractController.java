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
@RestController @RequestMapping("/api/contracts")
public class ContractController {
 private final Database db;private final Access access;private final ContractService contracts;private final BillingCalendar calendar;
 public ContractController(Database db,Access access,ContractService contracts,BillingCalendar calendar){this.db=db;this.access=access;this.contracts=contracts;this.calendar=calendar;}
 @GetMapping public Object list(){return db.rows("select c.*,r.room_code,t.full_name from contracts c join rooms r on r.id=c.room_id join tenant_profiles t on t.id=c.primary_tenant_id where "+access.contractScope("c")+" order by c.id desc");}
 @GetMapping("/{id}") public Object detail(@PathVariable long id){return contracts.detail(id);}
 @GetMapping("/{id}/billing-calendar") public Object calendar(@PathVariable long id,@RequestParam String period){access.contract(id);return calendar.dates(id,period);}
 @PostMapping public Object create(@Valid @RequestBody Contract r){return contracts.create(r);}
 @PostMapping("/{id}/members") public Object member(@PathVariable long id,@Valid @RequestBody Member r){return contracts.member(id,r);}
 public record Departure(@NotNull LocalDate effectiveTo){}
 @PostMapping("/{id}/members/{memberId}/depart") @Transactional public Object depart(@PathVariable long id,@PathVariable long memberId,@Valid @RequestBody Departure r){access.manager();access.contract(id);var c=db.one("select * from contracts where id=? for update",id);require("ACTIVE".equals(c.get("status")),"Contract is not active");var m=db.one("select * from contract_members where id=? and contract_id=?",memberId,id);require(m.get("effective_to")==null,"Member has already departed");db.jdbc.update("update contract_members set effective_to=? where id=?",r.effectiveTo(),memberId);return Map.of("id",memberId);}
 public record Renewal(@NotNull LocalDate endDate){}
 @PostMapping("/{id}/renew") public Object renew(@PathVariable long id,@Valid @RequestBody Renewal r){return contracts.renew(id,r.endDate());}
 @PostMapping("/{id}/checkout") public Object checkout(@PathVariable long id,@Valid @RequestBody Checkout r){return contracts.checkout(id,r);}
 @GetMapping("/{id}/checkout") public Object record(@PathVariable long id){access.contract(id);return db.one("select * from v_checkout_totals where contract_id=?",id);}
 public record Notes(@NotNull String terms){}
 @PutMapping("/{id}/notes") @Transactional public Object notes(@PathVariable long id,@Valid @RequestBody Notes r){access.manager();access.contract(id);db.jdbc.update("update contracts set terms=? where id=?",r.terms(),id);return Map.of("id",id);}
}
