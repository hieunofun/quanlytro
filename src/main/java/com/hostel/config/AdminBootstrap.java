package com.hostel.config;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
public class AdminBootstrap {
    @Bean ApplicationRunner bootstrap(JdbcTemplate db, PasswordEncoder encoder, @Value("${ADMIN_PASSWORD:}") String password) {
        return args -> {
            if (password.isBlank()) return;
            if (password.length() < 12) throw new IllegalArgumentException("ADMIN_PASSWORD requires at least 12 characters");
            db.update("insert into users(username,password_hash,role) values ('admin',?,'ADMIN') on conflict (username) do nothing", encoder.encode(password));
        };
    }
}
