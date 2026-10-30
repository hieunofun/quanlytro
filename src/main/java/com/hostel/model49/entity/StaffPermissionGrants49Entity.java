package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="staff_permission_grants") @Getter
public class StaffPermissionGrants49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="assignment_id",nullable=false) private Long assignmentId;
 @Column(name="action_code",nullable=false,length=40) private String actionCode;
 @Column(name="amount_limit",nullable=true,precision=14,scale=2) private java.math.BigDecimal amountLimit;
 @Column(name="expires_at",nullable=true) private java.time.Instant expiresAt;
 @Column(name="granted_by",nullable=false) private Long grantedBy;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
