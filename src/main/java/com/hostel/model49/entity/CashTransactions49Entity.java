package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="cash_transactions") @Getter
public class CashTransactions49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="cash_account_id",nullable=false) private Long cashAccountId;
 @Column(name="payment_document_id") private Long paymentDocumentId;
 @Column(name="payer_or_payee_id",nullable=true) private Long payerOrPayeeId;
 @Column(name="transaction_code",nullable=false,length=80) private String transactionCode;
 @Column(name="direction",nullable=false,length=5) private String direction;
 @Column(name="amount",nullable=false,precision=14,scale=2) private java.math.BigDecimal amount;
 @Column(name="method",nullable=false,length=30) private String method;
 @Column(name="occurred_at",nullable=false) private java.time.Instant occurredAt;
 @Column(name="recorded_at",nullable=false) private java.time.Instant recordedAt;
 @Column(name="status",nullable=false,length=20) private String status;
 @Column(name="external_reference",nullable=true,length=150) private String externalReference;
 @Column(name="counterparty_note",nullable=true,columnDefinition="text") private String counterpartyNote;
 @Column(name="reason",nullable=true,columnDefinition="text") private String reason;
 @Column(name="recorded_by",nullable=false) private Long recordedBy;
 @Column(name="confirmed_by",nullable=true) private Long confirmedBy;
 @Column(name="confirmed_at",nullable=true) private java.time.Instant confirmedAt;
}
