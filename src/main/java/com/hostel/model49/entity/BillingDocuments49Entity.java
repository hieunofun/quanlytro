package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="billing_documents") @Getter
public class BillingDocuments49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="contract_id",nullable=true) private Long contractId;
 @Column(name="reservation_id",nullable=true) private Long reservationId;
 @Column(name="checkout_id",nullable=true) private Long checkoutId;
 @Column(name="related_document_id",nullable=true) private Long relatedDocumentId;
 @Column(name="document_code",nullable=false,length=80) private String documentCode;
 @Column(name="document_type",nullable=false,length=30) private String documentType;
 @Column(name="billing_period",nullable=true) private java.time.LocalDate billingPeriod;
 @Column(name="issue_date",nullable=false) private java.time.LocalDate issueDate;
 @Column(name="due_date",nullable=false) private java.time.LocalDate dueDate;
 @Column(name="status",nullable=false,length=20) private String status;
 @Column(name="reason",nullable=false,columnDefinition="text") private String reason;
 @Column(name="created_by",nullable=true) private Long createdBy;
 @Column(name="issued_by",nullable=true) private Long issuedBy;
 @Column(name="issued_at",nullable=true) private java.time.Instant issuedAt;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
