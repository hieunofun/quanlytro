package com.hostel.model49;

import com.hostel.service.Database;
import com.hostel.security.SessionRegistry;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.OffsetDateTime;
import java.util.*;
import static com.hostel.service.Database.*;
import static com.hostel.model49.Input49.*;

@RestController @RequestMapping("/api") @Profile("model49") @Transactional
public class Recovery49 {
    private final Database db;private final Access49 access;private final PasswordEncoder passwords;private final SessionRegistry sessions;private final Events49 events;
    public Recovery49(Database db,Access49 access,PasswordEncoder passwords,SessionRegistry sessions,Events49 events){this.db=db;this.access=access;this.passwords=passwords;this.sessions=sessions;this.events=events;}
    static String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    @PostMapping("/users/{id}/reset-code") public Object issue(@PathVariable long id,@RequestBody Map<String,Object> m){
        access.admin();db.one("select id from users where id=? and status='ACTIVE' for update",id);required(m,"reason");
        byte[] entropy=new byte[32];new SecureRandom().nextBytes(entropy);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
        db.jdbc.update("update password_reset_tokens set used_at=clock_timestamp() where user_id=? and used_at is null",id);
        long row=db.insert("password_reset_tokens","user_id,token_hash,issued_by,expires_at",id,hash(token),access.user(),now().plusMinutes(15));events.audit("ISSUE_RESET","users",id,null,Map.of("resetId",row),required(m,"reason"));
        return ResponseEntity.ok().header("Cache-Control","no-store").body(Map.of("resetId",row,"code",token,"expiresInSeconds",900,"delivery","Chủ nhà chuyển mã riêng sau khi xác minh danh tính"));
    }
    @PostMapping("/auth/reset-password") public ResponseEntity<?> reset(@RequestBody Map<String,Object> m){
        long reset=keyId(m,"resetId");String password=required(m,"newPassword"),code=required(m,"code");require(password.length()>=12&&password.getBytes(StandardCharsets.UTF_8).length<=72,"Password must be 12 characters to 72 UTF-8 bytes");
        var rows=db.rows("select r.*,u.status from password_reset_tokens r join users u on u.id=r.user_id where r.id=? for update of r",reset);
        if(rows.isEmpty())return ResponseEntity.badRequest().body(Map.of("message","Mã khôi phục không hợp lệ hoặc đã hết hạn"));var r=rows.getFirst();
        boolean eligible=r.get("used_at")==null&&((Number)r.get("attempts")).intValue()<5&&time(r.get("expires_at")).isAfter(now())&&"ACTIVE".equals(r.get("status"));
        if(!eligible)return ResponseEntity.badRequest().body(Map.of("message","Mã khôi phục không hợp lệ hoặc đã hết hạn"));
        db.jdbc.update("update password_reset_tokens set attempts=attempts+1 where id=?",reset);
        if(!MessageDigest.isEqual(hash(code).getBytes(StandardCharsets.US_ASCII),r.get("token_hash").toString().getBytes(StandardCharsets.US_ASCII)))return ResponseEntity.badRequest().body(Map.of("message","Mã khôi phục không hợp lệ hoặc đã hết hạn"));
        db.jdbc.update("update users set password_hash=? where id=?",passwords.encode(password),r.get("user_id"));db.jdbc.update("update password_reset_tokens set used_at=clock_timestamp() where user_id=? and used_at is null",r.get("user_id"));sessions.revoke(id(r,"user_id"));
        db.jdbc.update("insert into audit_events(actor_type,action,resource_type,resource_id,reason) values('SYSTEM','RESET_PASSWORD','users',?,'Verified one-time recovery code')",r.get("user_id").toString());
        return ResponseEntity.ok().header("Cache-Control","no-store").body(Map.of("message","Mật khẩu đã đổi; đăng nhập lại"));
    }
}
