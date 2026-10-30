package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="billing_items") @Getter
public class BillingItems49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="document_id",nullable=false) private Long documentId;
 @Column(name="line_no",nullable=false) private Integer lineNo;
 @Column(name="item_kind",nullable=false,length=30) private String itemKind;
 @Column(name="direction",nullable=false,length=10) private String direction;
 @Column(name="adjusts_item_id",nullable=true) private Long adjustsItemId;
 @Column(name="contract_term_id",nullable=true) private Long contractTermId;
 @Column(name="subscription_id",nullable=true) private Long subscriptionId;
 @Column(name="segment_id",nullable=true) private Long segmentId;
 @Column(name="service_price_config_id",nullable=true) private Long servicePriceConfigId;
 @Column(name="handover_item_id",nullable=true) private Long handoverItemId;
 @Column(name="maintenance_cost_id",nullable=true) private Long maintenanceCostId;
 @Column(name="violation_id",nullable=true) private Long violationId;
 @Column(name="item_name_snapshot",nullable=false,length=150) private String itemNameSnapshot;
 @Column(name="unit_name_snapshot",nullable=false,length=40) private String unitNameSnapshot;
 @Column(name="unit_price_snapshot",nullable=false,precision=14,scale=4) private java.math.BigDecimal unitPriceSnapshot;
 @Column(name="calculation_basis",nullable=false,length=20) private String calculationBasis;
 @Column(name="quantity_snapshot",nullable=true,precision=18,scale=6) private java.math.BigDecimal quantitySnapshot;
 @Column(name="calculation_period",nullable=true) private java.time.LocalDate calculationPeriod;
 @Column(name="service_from",nullable=false) private java.time.LocalDate serviceFrom;
 @Column(name="service_to",nullable=false) private java.time.LocalDate serviceTo;
 @Column(name="verification_status",nullable=false,length=20) private String verificationStatus;
 @Column(name="basis_note",nullable=false,columnDefinition="text") private String basisNote;
 @Column(name="source_key",nullable=true,length=120) private String sourceKey;
}
