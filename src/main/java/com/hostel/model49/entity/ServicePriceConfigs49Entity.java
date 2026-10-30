package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="service_price_configs") @Getter
public class ServicePriceConfigs49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="property_id",nullable=false) private Long propertyId;
 @Column(name="service_item_id",nullable=false) private Long serviceItemId;
 @Column(name="effective_from",nullable=false) private java.time.LocalDate effectiveFrom;
 @Column(name="unit_price",nullable=false,precision=14,scale=2) private java.math.BigDecimal unitPrice;
 @Column(name="recorded_by",nullable=true) private Long recordedBy;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
