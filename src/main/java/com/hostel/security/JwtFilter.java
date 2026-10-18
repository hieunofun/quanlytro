package com.hostel.security;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKey;
@Component
public class JwtFilter extends OncePerRequestFilter {
 private final JdbcTemplate db;private final SessionRegistry sessions;public final SecretKey key;
 public JwtFilter(JdbcTemplate db,SessionRegistry sessions,@Value("${app.jwt.secret}") String secret){this.db=db;this.sessions=sessions;key=Keys.hmacShaKeyFor((secret+UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));}
 public static String fingerprint(Map<String,Object> u){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((u.get("password_hash")+":"+u.get("role")+":"+u.get("created_at")).getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  String h=req.getHeader("Authorization");
  if(h!=null && h.startsWith("Bearer "))try{
   var c=Jwts.parser().verifyWith(key).build().parseSignedClaims(h.substring(7)).getPayload();long id=Long.parseLong(c.getSubject());
   var rows=db.queryForList("select * from users where id=? and status='ACTIVE'",id);
   if(!rows.isEmpty() && !sessions.locked(id) && Long.valueOf(sessions.epoch(id)).equals(c.get("epoch",Long.class)) && fingerprint(rows.getFirst()).equals(c.get("fp",String.class)))SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(Long.toString(id),null,List.of(new SimpleGrantedAuthority("ROLE_"+rows.getFirst().get("role")))));
  }catch(io.jsonwebtoken.JwtException|IllegalArgumentException ignored){SecurityContextHolder.clearContext();}
  chain.doFilter(req,res);
 }
}
