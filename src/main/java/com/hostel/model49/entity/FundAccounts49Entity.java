package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="fund_accounts") @Getter
public class FundAccounts49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="fund_code",nullable=false,length=100) private String fundCode;
 @Column(name="fund_type",nullable=false,length=20) private String fundType;
 @Column(name="reconciliation_status",nullable=false,length=20) private String reconciliationStatus;
 @Column(name="contract_id",nullable=true) private Long contractId;
 @Column(name="reservation_id",nullable=true) private Long reservationId;
 @Column(name="unapplied_receipt_id",nullable=true) private Long unappliedReceiptId;
 @Column(name="beneficiary_tenant_id",nullable=true) private Long beneficiaryTenantId;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
