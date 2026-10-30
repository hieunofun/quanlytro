package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="contract_violations") @Getter
public class ContractViolations49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="contract_id",nullable=false) private Long contractId;
 @Column(name="violation_type",nullable=false,length=50) private String violationType;
 @Column(name="description",nullable=false,columnDefinition="text") private String description;
 @Column(name="occurred_on",nullable=false) private java.time.LocalDate occurredOn;
 @Column(name="status",nullable=false,length=20) private String status;
 @Column(name="resolution_note",nullable=true,columnDefinition="text") private String resolutionNote;
 @Column(name="created_by",nullable=false) private Long createdBy;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
