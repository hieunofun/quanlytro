package com.hostel.controller;
import com.hostel.dto.Requests.*;
import com.hostel.security.Access;
import com.hostel.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api")
public class InvoiceController {
    private final Database db;private final Access access;private final InvoiceService invoices;
    public InvoiceController(Database db,Access access,InvoiceService invoices) {this.db=db;this.access=access;this.invoices=invoices;}
    @GetMapping("/invoices") public Object list() {return db.rows("select i.*,r.room_code,t.full_name from v_invoice_balances i join contracts c on c.id=i.contract_id join rooms r on r.id=c.room_id join tenant_profiles t on t.id=c.primary_tenant_id where "+access.contractScope("c")+(access.role().equals("TENANT")?" and i.status='ISSUED'":"")+" order by i.id desc");}
    @GetMapping("/invoices/{id}") public Object detail(@PathVariable long id) {return invoices.detail(id);}
    @PostMapping("/invoices") public Object create(@Valid @RequestBody Invoice r) {return invoices.create(r);}
    @PostMapping("/invoices/{id}/issue") public Object issue(@PathVariable long id) {return invoices.issue(id);}
    @PostMapping("/invoices/{id}/payments") public Object pay(@PathVariable long id,@Valid @RequestBody Payment r) {return invoices.pay(id,r);}
    @DeleteMapping("/invoices/{id}") public Object remove(@PathVariable long id) {invoices.removeDraft(id);return Map.of("message","Draft deleted");}
    @GetMapping("/payments") public Object payments() {return db.rows("select p.*,i.invoice_code,r.room_code from payment_transactions p join invoices i on i.id=p.invoice_id join contracts c on c.id=i.contract_id join rooms r on r.id=c.room_id where "+access.contractScope("c")+" order by p.id desc");}
}
