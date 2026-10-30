package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="handovers") @Getter
public class Handovers49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="contract_id",nullable=false) private Long contractId;
 @Column(name="possession_id",nullable=true) private Long possessionId;
 @Column(name="handover_code",nullable=false,length=60) private String handoverCode;
 @Column(name="handover_type",nullable=false,length=20) private String handoverType;
 @Column(name="occurred_at",nullable=false) private java.time.Instant occurredAt;
 @Column(name="status",nullable=false,length=20) private String status;
 @Column(name="given_by",nullable=true) private Long givenBy;
 @Column(name="received_by_tenant_id",nullable=true) private Long receivedByTenantId;
 @Column(name="confirmed_by",nullable=true) private Long confirmedBy;
 @Column(name="confirmed_at",nullable=true) private java.time.Instant confirmedAt;
 @Column(name="note",nullable=true,columnDefinition="text") private String note;
}
