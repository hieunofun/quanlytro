package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="invoices") @Getter
public class InvoiceEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="invoice_code",nullable=false) private String invoiceCode;
 @Column(name="contract_id",nullable=false) private Long contractId;
 @Column(name="billing_period",nullable=false) private java.time.LocalDate billingPeriod;
 @Column(name="issue_date",nullable=false) private java.time.LocalDate issueDate;
 @Column(name="due_date",nullable=false) private java.time.LocalDate dueDate;
 @Column(name="status",nullable=false) private String status;
 @Column(name="is_settlement",nullable=false) private Boolean isSettlement;
 @Column(name="issued_at",nullable=true) private java.time.Instant issuedAt;
}
