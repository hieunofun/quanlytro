package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="checkout_records") @Getter
public class CheckoutRecordEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="contract_id",nullable=false) private Long contractId;
 @Column(name="deposit_snapshot",nullable=false,precision=12,scale=2) private java.math.BigDecimal depositSnapshot;
 @Column(name="outstanding_snapshot",nullable=false,precision=14,scale=2) private java.math.BigDecimal outstandingSnapshot;
 @Column(name="damage_amount",nullable=false,precision=12,scale=2) private java.math.BigDecimal damageAmount;
 @Column(name="settled_by",nullable=false) private Long settledBy;
 @Column(name="settled_at",nullable=false) private java.time.Instant settledAt;
 @Column(name="notes",nullable=true,columnDefinition="text") private String notes;
}
