package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="users") @Getter
public class Users49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="username",nullable=false,length=50) private String username;
 @Column(name="password_hash",nullable=false,length=255) private String passwordHash;
 @Column(name="role",nullable=false,length=20) private String role;
 @Column(name="status",nullable=false,length=20) private String status;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
