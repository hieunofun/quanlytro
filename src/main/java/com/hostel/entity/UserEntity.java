package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="users") @Getter
public class UserEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="username",nullable=false) private String username;
 @Column(name="password_hash",nullable=false) private String passwordHash;
 @Column(name="role",nullable=false) private String role;
 @Column(name="status",nullable=false) private String status;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
