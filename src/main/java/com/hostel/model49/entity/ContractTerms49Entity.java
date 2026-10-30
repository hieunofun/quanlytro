package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="contract_terms") @Getter
public class ContractTerms49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="contract_id",nullable=false) private Long contractId;
 @Column(name="version_no",nullable=false) private Integer versionNo;
 @Column(name="effective_from",nullable=false) private java.time.LocalDate effectiveFrom;
 @Column(name="lease_end",nullable=false) private java.time.LocalDate leaseEnd;
 @Column(name="rental_price",nullable=false,precision=14,scale=2) private java.math.BigDecimal rentalPrice;
 @Column(name="deposit_required",nullable=false,precision=14,scale=2) private java.math.BigDecimal depositRequired;
 @Column(name="payment_day",nullable=false) private Short paymentDay;
 @Column(name="collection_timing",nullable=false,length=20) private String collectionTiming;
 @Column(name="terms",nullable=false,columnDefinition="text") private String terms;
 @Column(name="signed_at",nullable=true) private java.time.Instant signedAt;
 @Column(name="approved_by",nullable=true) private Long approvedBy;
 @Column(name="origin",nullable=false,length=20) private String origin;
 @Column(name="status",nullable=false,length=20) private String status;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
