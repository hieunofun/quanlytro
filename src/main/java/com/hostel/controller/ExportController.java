package com.hostel.controller;
import com.hostel.security.Access;
import com.hostel.service.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;
import static com.hostel.service.Database.*;
@RestController
@RequestMapping("/api")
public class ExportController {
    private final PdfService pdf;private final InvoiceService invoices;private final ContractService contracts;private final Database db;private final Access access;
    public ExportController(PdfService pdf,InvoiceService invoices,ContractService contracts,Database db,Access access) {this.pdf=pdf;this.invoices=invoices;this.contracts=contracts;this.db=db;this.access=access;}
    @GetMapping("/invoices/{id}/pdf") public ResponseEntity<byte[]> invoice(@PathVariable long id) throws IOException {
        var i=invoices.detail(id);var c=contracts.detail(id(i,"contract_id"));var room=db.one("select r.room_code,p.name,p.address from rooms r join properties p on p.id=r.property_id where r.id=?",c.get("room_id"));
        var lines=new ArrayList<String>();lines.add("Khu nhà: "+room.get("name")+" · Phòng: "+room.get("room_code"));lines.add("Địa chỉ: "+room.get("address"));
        lines.add("Kỳ: "+i.get("billing_period")+" · Trạng thái: "+i.get("status"));lines.add("Ngày phát hành: "+i.get("issue_date")+" · Hạn thanh toán: "+i.get("due_date"));lines.add("");
        @SuppressWarnings("unchecked") var items=(List<Map<String,Object>>)i.get("items");
        for(var item:items) {lines.add(item.get("item_name_snapshot")+": "+item.get("subtotal")+" VND");lines.add("Cách tính: "+item.get("formula_description"));}
        lines.add("");lines.add("Tổng tiền: "+i.get("total_amount")+" VND");lines.add("Đã thanh toán: "+i.get("paid_amount")+" VND");lines.add("Còn nợ: "+i.get("balance_due")+" VND");
        return download("invoice-"+id+".pdf",pdf.render("HÓA ĐƠN "+i.get("invoice_code"),lines));
    }
    @GetMapping("/contracts/{id}/pdf") public ResponseEntity<byte[]> contract(@PathVariable long id) throws IOException {
        var c=contracts.detail(id);var tenant=db.one("select * from tenant_profiles where id=?",c.get("primary_tenant_id"));var room=db.one("select r.room_code,p.name,p.address from rooms r join properties p on p.id=r.property_id where r.id=?",c.get("room_id"));
        return download("contract-"+id+".pdf",pdf.render("HỢP ĐỒNG THUÊ PHÒNG "+c.get("contract_code"),List.of("Khu nhà: "+room.get("name"),"Địa chỉ: "+room.get("address"),"Phòng: "+room.get("room_code"),"Người đại diện: "+tenant.get("full_name"),"CCCD: "+tenant.get("identity_number"),"Điện thoại: "+tenant.get("phone"),"Thời hạn: "+c.get("start_date")+" đến "+c.get("end_date")+" (không bao gồm ngày kết thúc)","Giá thuê: "+c.get("rental_price")+" VND/tháng","Tiền cọc đã ghi nhận: "+c.get("deposit_amount")+" VND","Thành viên: "+c.get("members"),"Điều khoản bổ sung: "+c.get("terms"),"Đại diện chủ nhà: __________________","Đại diện người thuê: __________________")));
    }
    @GetMapping("/reports/pdf") public ResponseEntity<byte[]> report() throws IOException {
        access.manager();var rows=db.rows("select i.invoice_code,r.room_code,i.billing_period,i.total_amount,i.paid_amount,i.balance_due from v_invoice_balances i join contracts c on c.id=i.contract_id join rooms r on r.id=c.room_id where "+access.contractScope("c")+" and i.status='ISSUED' order by i.billing_period,i.id");
        return download("report.pdf",pdf.render("BÁO CÁO HÓA ĐƠN VÀ CÔNG NỢ",rows.stream().map(r -> r.get("invoice_code")+" · Phòng "+r.get("room_code")+" · Tổng: "+r.get("total_amount")+" · Đã thu: "+r.get("paid_amount")+" · Nợ: "+r.get("balance_due")).toList()));
    }
    private ResponseEntity<byte[]> download(String name,byte[] body) {return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+name+"\"").body(body);}
}
