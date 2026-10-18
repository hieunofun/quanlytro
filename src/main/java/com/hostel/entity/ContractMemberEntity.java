package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="contract_members") @Getter
public class ContractMemberEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="contract_id",nullable=false) private Long contractId;
 @Column(name="tenant_id",nullable=false) private Long tenantId;
 @Column(name="relationship",nullable=true) private String relationship;
 @Column(name="effective_from",nullable=false) private java.time.LocalDate effectiveFrom;
 @Column(name="effective_to",nullable=true) private java.time.LocalDate effectiveTo;
}
