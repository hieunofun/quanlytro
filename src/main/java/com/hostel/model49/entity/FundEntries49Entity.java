package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="fund_entries") @Getter
public class FundEntries49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="entry_code",nullable=false,length=120) private String entryCode;
 @Column(name="entry_type",nullable=false,length=30) private String entryType;
 @Column(name="cash_transaction_id",nullable=true) private Long cashTransactionId;
 @Column(name="from_fund_id",nullable=true) private Long fromFundId;
 @Column(name="to_fund_id",nullable=true) private Long toFundId;
 @Column(name="debit_item_id",nullable=true) private Long debitItemId;
 @Column(name="credit_item_id",nullable=true) private Long creditItemId;
 @Column(name="original_entry_id",nullable=true) private Long originalEntryId;
 @Column(name="amount",nullable=false,precision=14,scale=2) private java.math.BigDecimal amount;
 @Column(name="reason",nullable=false,columnDefinition="text") private String reason;
 @Column(name="posted_by",nullable=false) private Long postedBy;
 @Column(name="posted_at",nullable=false) private java.time.Instant postedAt;
}
