package com.hostel.controller;
import com.hostel.dto.Requests;
import com.hostel.security.*;
import com.hostel.service.Database;
import io.jsonwebtoken.Jwts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import static com.hostel.service.Database.*;
@RestController
@RequestMapping("/api")
public class AuthController {
    private final Database db; private final Access access; private final JwtFilter jwt; private final PasswordEncoder encoder;
    private final long duration;
    public AuthController(Database db,Access access,JwtFilter jwt,PasswordEncoder encoder,@Value("${app.jwt.access-token-expiration-ms}") long duration) {
        this.db=db;this.access=access;this.jwt=jwt;this.encoder=encoder;this.duration=duration;
    }
    @GetMapping("/health") public Map<String,Object> health() { db.jdbc.queryForObject("select 1",Integer.class); return Map.of("status","UP"); }
    @PostMapping("/auth/login") public Map<String,Object> login(@Valid @RequestBody Requests.Login request) {
        var rows=db.rows("select * from users where username=?",request.username());
        if(rows.isEmpty()) { encoder.matches(request.password(),"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"); throw unauthorized(); }
        var u=rows.getFirst();
        if(!"ACTIVE".equals(u.get("status")) || u.get("locked_until")!=null && ((java.sql.Timestamp)u.get("locked_until")).toInstant().isAfter(Instant.now())) throw unauthorized();
        if(!encoder.matches(request.password(),u.get("password_hash").toString())) {
            db.jdbc.update("update users set failed_attempts=case when locked_until < current_timestamp then 1 else failed_attempts+1 end, locked_until=case when (case when locked_until < current_timestamp then 1 else failed_attempts+1 end)>=5 then current_timestamp+interval '15 minutes' else null end where id=?",id(u,"id"));
            throw unauthorized();
        }
        db.jdbc.update("update users set failed_attempts=0,locked_until=null where id=?",id(u,"id"));
        return tokens(u);
    }
    private Map<String,Object> tokens(Map<String,Object> u) {
        String refresh=UUID.randomUUID()+"-"+UUID.randomUUID();
        db.insert("auth_sessions","user_id,token_hash,expires_at",id(u,"id"),hash(refresh),java.sql.Timestamp.from(Instant.now().plus(Duration.ofDays(7))));
        String token=Jwts.builder().subject(u.get("id").toString()).claim("ver",((Number)u.get("token_version")).intValue())
            .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+duration)).signWith(jwt.key).compact();
        return sensitive(Map.of("accessToken",token,"refreshToken",refresh,"expiresIn",duration/1000,"user",Map.of("id",u.get("id"),"name",u.get("full_name"),"role",u.get("role"))));
    }
    public record Refresh(@NotBlank String refreshToken) { @Override public String toString() {return "Refresh[token redacted]";} }
    @PostMapping("/auth/refresh") @Transactional public Map<String,Object> refresh(@Valid @RequestBody Refresh r) {
        var sessions=db.rows("select * from auth_sessions where token_hash=? and expires_at>current_timestamp for update",hash(r.refreshToken()));
        if(sessions.isEmpty()) throw unauthorized();
        var u=db.one("select * from users where id=? and status='ACTIVE' and (locked_until is null or locked_until<current_timestamp)",id(sessions.getFirst(),"user_id"));
        db.jdbc.update("delete from auth_sessions where id=?",id(sessions.getFirst(),"id")); return tokens(u);
    }
    @GetMapping("/auth/me") public Map<String,Object> me() { return db.one("select id,username,full_name,role,email from users where id=?",access.userId()); }
    @PostMapping("/auth/logout") @Transactional public Map<String,String> logout() { revoke(access.userId()); return Map.of("message","Logged out"); }
    public record Password(@NotBlank String oldPassword,@NotBlank @Size(min=12,max=72) String newPassword) { @Override public String toString() {return "Password[credentials redacted]";} }
    @PostMapping("/auth/change-password") @Transactional public Map<String,String> change(@Valid @RequestBody Password r) {
        var u=db.one("select * from users where id=?",access.userId());
        require(encoder.matches(r.oldPassword(),u.get("password_hash").toString()),"Current password is incorrect");
        db.jdbc.update("update users set password_hash=? where id=?",encoder.encode(r.newPassword()),access.userId()); revoke(access.userId());
        return Map.of("message","Password changed; sign in again");
    }
    @PostMapping("/users/{id}/reset-code") @Transactional public Map<String,String> resetCode(@PathVariable long id) {
        access.admin(); db.one("select id from users where id=?",id);
        String code=UUID.randomUUID().toString();
        db.jdbc.update("update password_reset_codes set used=true where user_id=?",id);
        db.insert("password_reset_codes","user_id,code_hash,expires_at",id,hash(code),java.sql.Timestamp.from(Instant.now().plusSeconds(900)));
        return sensitive(Map.of("code",code,"message","Give this one-time code to the verified account owner; expires in 15 minutes"));
    }
    public record Reset(@NotBlank String code,@NotBlank @Size(min=12,max=72) String newPassword) { @Override public String toString() {return "Reset[credentials redacted]";} }
    @PostMapping("/auth/reset-password") @Transactional public Map<String,String> reset(@Valid @RequestBody Reset r) {
        var rows=db.rows("select * from password_reset_codes where code_hash=? and not used and expires_at>current_timestamp for update",hash(r.code()));
        require(!rows.isEmpty(),"Invalid or expired reset code"); var code=rows.getFirst(); long id=id(code,"user_id");
        db.jdbc.update("update users set password_hash=?, failed_attempts=0,locked_until=null where id=?",encoder.encode(r.newPassword()),id);
        db.jdbc.update("update password_reset_codes set used=true where id=?",id(code,"id")); revoke(id);
        return Map.of("message","Password reset");
    }
    void revoke(long id) { db.jdbc.update("update users set token_version=token_version+1 where id=?",id); db.jdbc.update("delete from auth_sessions where user_id=?",id); }
    static String hash(String token) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); } catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }
    static ResponseStatusException unauthorized() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials or account temporarily locked"); }
    static <V> Map<String,V> sensitive(Map<String,V> values) {
        var response=new LinkedHashMap<String,V>() { @Override public String toString() {return "[Authentication data redacted]";} };
        response.putAll(values);return response;
    }
}
