package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="file_attachments") @Getter
public class FileAttachments49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="file_id",nullable=false) private Long fileId;
 @Column(name="room_id",nullable=true) private Long roomId;
 @Column(name="asset_id",nullable=true) private Long assetId;
 @Column(name="handover_item_id",nullable=true) private Long handoverItemId;
 @Column(name="meter_reading_id",nullable=true) private Long meterReadingId;
 @Column(name="maintenance_request_id",nullable=true) private Long maintenanceRequestId;
 @Column(name="maintenance_cost_id",nullable=true) private Long maintenanceCostId;
 @Column(name="contract_term_id",nullable=true) private Long contractTermId;
 @Column(name="billing_document_id",nullable=true) private Long billingDocumentId;
 @Column(name="violation_id",nullable=true) private Long violationId;
 @Column(name="fund_entry_id",nullable=true) private Long fundEntryId;
 @Column(name="caption",nullable=false,columnDefinition="text") private String caption;
 @Column(name="sort_order",nullable=false) private Integer sortOrder;
 @Column(name="attached_by",nullable=false) private Long attachedBy;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
