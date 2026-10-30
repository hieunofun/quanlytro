package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="contract_parties") @Getter
public class ContractParties49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="contract_id",nullable=false) private Long contractId;
 @Column(name="tenant_id",nullable=false) private Long tenantId;
 @Column(name="party_role",nullable=false,length=30) private String partyRole;
 @Column(name="effective_from",nullable=false) private java.time.LocalDate effectiveFrom;
 @Column(name="effective_to",nullable=true) private java.time.LocalDate effectiveTo;
 @Column(name="agreement_note",nullable=false,columnDefinition="text") private String agreementNote;
}
