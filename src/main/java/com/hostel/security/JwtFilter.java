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
import java.util.List;
import javax.crypto.SecretKey;
@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JdbcTemplate db;
    public final SecretKey key;
    public JwtFilter(JdbcTemplate db, @Value("${app.jwt.secret}") String secret) {
        this.db = db; key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(header.substring(7)).getPayload();
                String id = claims.getSubject();
                var users = db.queryForList("select id, role from users where id=? and status='ACTIVE' and token_version=? and (locked_until is null or locked_until < current_timestamp)", Long.parseLong(id), claims.get("ver", Integer.class));
                if (!users.isEmpty()) {
                    var user = users.getFirst();
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.get("id").toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.get("role")))));
                }
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ignored) { SecurityContextHolder.clearContext(); }
        }
        chain.doFilter(req, res);
    }
}
