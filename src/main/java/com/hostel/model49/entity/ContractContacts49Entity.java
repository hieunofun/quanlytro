package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="contract_contacts") @Getter
public class ContractContacts49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="contract_id",nullable=false) private Long contractId;
 @Column(name="tenant_id",nullable=false) private Long tenantId;
 @Column(name="contact_role",nullable=false,length=20) private String contactRole;
 @Column(name="effective_from",nullable=false) private java.time.LocalDate effectiveFrom;
 @Column(name="effective_to",nullable=true) private java.time.LocalDate effectiveTo;
 @Column(name="can_view_billing",nullable=false) private Boolean canViewBilling;
 @Column(name="authorized_by",nullable=true) private Long authorizedBy;
}
