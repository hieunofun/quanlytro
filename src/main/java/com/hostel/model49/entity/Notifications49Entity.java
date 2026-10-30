package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="notifications") @Getter
public class Notifications49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="notification_type",nullable=false,length=40) private String notificationType;
 @Column(name="title",nullable=false,length=200) private String title;
 @Column(name="body",nullable=false,columnDefinition="text") private String body;
 @Column(name="billing_document_id",nullable=true) private Long billingDocumentId;
 @Column(name="contract_id",nullable=true) private Long contractId;
 @Column(name="maintenance_request_id",nullable=true) private Long maintenanceRequestId;
 @Column(name="consumption_alert_id",nullable=true) private Long consumptionAlertId;
 @Column(name="deduplication_key",nullable=false,length=180) private String deduplicationKey;
 @Column(name="created_by",nullable=true) private Long createdBy;
 @Column(name="sent_at",nullable=false) private java.time.Instant sentAt;
}
