package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="fund_holds") @Getter
public class FundHolds49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="fund_account_id",nullable=false) private Long fundAccountId;
 @Column(name="related_billing_item_id",nullable=true) private Long relatedBillingItemId;
 @Column(name="amount",nullable=false,precision=14,scale=2) private java.math.BigDecimal amount;
 @Column(name="reason",nullable=false,columnDefinition="text") private String reason;
 @Column(name="review_on",nullable=false) private java.time.LocalDate reviewOn;
 @Column(name="created_by",nullable=false) private Long createdBy;
 @Column(name="released_at",nullable=true) private java.time.Instant releasedAt;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
