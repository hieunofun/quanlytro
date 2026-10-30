package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="audit_events") @Getter
public class AuditEvents49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="actor_user_id",nullable=true) private Long actorUserId;
 @Column(name="actor_type",nullable=false,length=10) private String actorType;
 @Column(name="action",nullable=false,length=80) private String action;
 @Column(name="resource_type",nullable=false,length=80) private String resourceType;
 @Column(name="resource_id",nullable=true,length=100) private String resourceId;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
 @Column(name="before_data",nullable=true,columnDefinition="jsonb") private String beforeData;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
 @Column(name="after_data",nullable=true,columnDefinition="jsonb") private String afterData;
 @Column(name="reason",nullable=true,columnDefinition="text") private String reason;
 @Column(name="recorded_at",nullable=false) private java.time.Instant recordedAt;
}
