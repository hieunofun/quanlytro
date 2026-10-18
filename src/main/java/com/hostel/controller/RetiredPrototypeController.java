package com.hostel.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
/** Explicit response for prototype functions outside the approved MVP. */
@RestController @RequestMapping("/api")
public class RetiredPrototypeController {
 @RequestMapping({"/room-assignments","/room-assignments/{id}","/rooms/{id}/images","/rooms/{id}/images/{image}","/rooms/{id}/details","/rooms/{id}/price-history","/audit-logs","/maintenance-schedules","/maintenance-schedules/{id}/complete","/notifications","/notifications/{id}/read","/notifications/announce","/contracts/{id}/violations","/contracts/{id}/violations/{violation}","/users/{id}/reset-code","/auth/reset-password"})
 public Object retired(){throw new ResponseStatusException(HttpStatus.GONE,"Chức năng prototype ngoài phạm vi MVP 17 bảng; dữ liệu cũ được giữ trong prototype_v9");}
 @PostMapping("/floors") public Object floor(){throw new ResponseStatusException(HttpStatus.GONE,"Nhập floorNumber trực tiếp khi tạo hoặc sửa phòng");}
}
