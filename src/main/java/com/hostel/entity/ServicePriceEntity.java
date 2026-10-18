package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="service_price_configs") @Getter
public class ServicePriceEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="property_id",nullable=false) private Long propertyId;
 @Column(name="service_item_id",nullable=false) private Long serviceItemId;
 @Column(name="unit_price",nullable=false,precision=12,scale=2) private java.math.BigDecimal unitPrice;
 @Column(name="effective_from",nullable=false) private java.time.LocalDate effectiveFrom;
}
