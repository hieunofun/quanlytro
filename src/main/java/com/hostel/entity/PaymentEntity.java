package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="payment_transactions") @Getter
public class PaymentEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="invoice_id",nullable=false) private Long invoiceId;
 @Column(name="transaction_code",nullable=false) private String transactionCode;
 @Column(name="amount",nullable=false,precision=12,scale=2) private java.math.BigDecimal amount;
 @Column(name="payment_method",nullable=false) private String paymentMethod;
 @Column(name="payment_date",nullable=false) private java.time.Instant paymentDate;
 @Column(name="received_by",nullable=false) private Long receivedBy;
 @Column(name="notes",nullable=true,columnDefinition="text") private String notes;
}
