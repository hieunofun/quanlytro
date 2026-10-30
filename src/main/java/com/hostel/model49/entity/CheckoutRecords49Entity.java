package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="checkout_records") @Getter
public class CheckoutRecords49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="contract_id",nullable=false) private Long contractId;
 @Column(name="return_handover_id",nullable=true) private Long returnHandoverId;
 @Column(name="billing_end",nullable=false) private java.time.LocalDate billingEnd;
 @Column(name="status",nullable=false,length=20) private String status;
 @Column(name="deposit_snapshot",nullable=false,precision=14,scale=2) private java.math.BigDecimal depositSnapshot;
 @Column(name="advance_snapshot",nullable=false,precision=14,scale=2) private java.math.BigDecimal advanceSnapshot;
 @Column(name="outstanding_snapshot",nullable=false,precision=14,scale=2) private java.math.BigDecimal outstandingSnapshot;
 @Column(name="approved_by",nullable=true) private Long approvedBy;
 @Column(name="approved_at",nullable=true) private java.time.Instant approvedAt;
 @Column(name="notes",nullable=true,columnDefinition="text") private String notes;
}
