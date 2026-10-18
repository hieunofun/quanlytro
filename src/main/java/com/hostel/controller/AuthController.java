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
import java.util.*;
import static com.hostel.service.Database.*;
@RestController @RequestMapping("/api")
public class AuthController {
 private final Database db;private final Access access;private final JwtFilter jwt;private final PasswordEncoder encoder;private final SessionRegistry sessions;private final long duration;
 public AuthController(Database db,Access access,JwtFilter jwt,PasswordEncoder encoder,SessionRegistry sessions,@Value("${app.jwt.access-token-expiration-ms}") long duration){this.db=db;this.access=access;this.jwt=jwt;this.encoder=encoder;this.sessions=sessions;this.duration=duration;}
 @GetMapping("/health") public Object health(){db.jdbc.queryForObject("select 1",Integer.class);return Map.of("status","UP","schemaVersion",17);}
 @PostMapping("/auth/login") public Map<String,Object> login(@Valid @RequestBody Requests.Login r){
  var rows=db.rows("select * from users where username=?",r.username());
  if(rows.isEmpty()){encoder.matches(r.password(),"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");throw unauthorized();}
  var u=rows.getFirst();long id=id(u,"id");if(!"ACTIVE".equals(u.get("status"))||sessions.locked(id))throw unauthorized();
  if(!encoder.matches(r.password(),u.get("password_hash").toString())){sessions.failed(id);throw unauthorized();}
  sessions.clear(id);return tokens(u);
 }
 private Map<String,Object> tokens(Map<String,Object> u){long id=id(u,"id");String fp=JwtFilter.fingerprint(u);
  String token=Jwts.builder().subject(Long.toString(id)).claim("fp",fp).claim("epoch",sessions.epoch(id)).id(UUID.randomUUID().toString()).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+duration)).signWith(jwt.key).compact();
  return sensitive(Map.of("accessToken",token,"refreshToken",sessions.refresh(id,fp),"expiresIn",duration/1000,"user",Map.of("id",id,"name",u.get("username"),"role",u.get("role"))));
 }
 public record Refresh(@NotBlank String refreshToken){@Override public String toString(){return "Refresh[redacted]";}}
 @PostMapping("/auth/refresh") public Map<String,Object> refresh(@Valid @RequestBody Refresh r){var s=sessions.consume(r.refreshToken());if(s==null||sessions.locked(s.user()))throw unauthorized();var u=db.one("select * from users where id=? and status='ACTIVE'",s.user());if(!JwtFilter.fingerprint(u).equals(s.fingerprint()))throw unauthorized();return tokens(u);}
 @GetMapping("/auth/me") public Object me(){return db.one("select id,username,username full_name,role from users where id=?",access.userId());}
 @PostMapping("/auth/logout") public Object logout(){sessions.revoke(access.userId());return Map.of("message","Logged out");}
 public record Password(@NotBlank String oldPassword,@NotBlank @Size(min=12,max=72) String newPassword){@Override public String toString(){return "Password[redacted]";}}
 @PostMapping("/auth/change-password") @Transactional public Object change(@Valid @RequestBody Password r){var u=db.one("select * from users where id=? for update",access.userId());require(encoder.matches(r.oldPassword(),u.get("password_hash").toString()),"Current password is incorrect");db.jdbc.update("update users set password_hash=? where id=?",encoder.encode(r.newPassword()),access.userId());sessions.revoke(access.userId());return Map.of("message","Password changed; sign in again");}
 static ResponseStatusException unauthorized(){return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials or account temporarily locked");}
 static <V> Map<String,V> sensitive(Map<String,V> values){var response=new LinkedHashMap<String,V>(){@Override public String toString(){return "[Authentication data redacted]";}};response.putAll(values);return response;}
}
