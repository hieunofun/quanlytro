package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="audit_logs") @Getter
public class AuditLogEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private UserEntity user;
    private String action;
    @Column(name="resource_type") private String resourceType;
    @Column(name="created_at") private java.time.Instant createdAt;
}
