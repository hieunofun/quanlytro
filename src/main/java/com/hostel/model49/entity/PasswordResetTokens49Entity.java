package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="password_reset_tokens") @Getter
public class PasswordResetTokens49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="user_id",nullable=false) private Long userId;
 @Column(name="token_hash",nullable=false,length=64) private String tokenHash;
 @Column(name="issued_by",nullable=false) private Long issuedBy;
 @Column(name="expires_at",nullable=false) private java.time.Instant expiresAt;
 @Column(name="used_at",nullable=true) private java.time.Instant usedAt;
 @Column(name="attempts",nullable=false) private Short attempts;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
