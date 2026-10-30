package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="maintenance_costs") @Getter
public class MaintenanceCosts49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="request_id",nullable=false) private Long requestId;
 @Column(name="description",nullable=false,columnDefinition="text") private String description;
 @Column(name="amount",nullable=false,precision=14,scale=2) private java.math.BigDecimal amount;
 @Column(name="incurred_on",nullable=false) private java.time.LocalDate incurredOn;
 @Column(name="advanced_by_tenant_id",nullable=true) private Long advancedByTenantId;
 @Column(name="recorded_by",nullable=false) private Long recordedBy;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
