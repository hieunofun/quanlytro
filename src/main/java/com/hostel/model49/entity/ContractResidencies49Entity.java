package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="contract_residencies") @Getter
public class ContractResidencies49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="contract_id",nullable=false) private Long contractId;
 @Column(name="tenant_id",nullable=false) private Long tenantId;
 @Column(name="effective_from",nullable=false) private java.time.LocalDate effectiveFrom;
 @Column(name="effective_to",nullable=true) private java.time.LocalDate effectiveTo;
 @Column(name="relationship",nullable=true,length=50) private String relationship;
 @Column(name="recorded_by",nullable=true) private Long recordedBy;
}
