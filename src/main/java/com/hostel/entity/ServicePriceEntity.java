package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="service_price_configs") @Getter
public class ServicePriceEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="property_id") private PropertyEntity property;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="service_item_id") private ServiceItemEntity service;
    @Column(name="unit_price") private java.math.BigDecimal price;
    @Column(name="effective_from") private java.time.LocalDate effectiveFrom;
}
