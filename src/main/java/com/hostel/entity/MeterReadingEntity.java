package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="meter_readings") @Getter
public class MeterReadingEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="contract_id",nullable=false) private Long contractId;
 @Column(name="service_item_id",nullable=false) private Long serviceItemId;
 @Column(name="billing_period",nullable=false) private java.time.LocalDate billingPeriod;
 @Column(name="old_index",nullable=false,precision=10,scale=2) private java.math.BigDecimal oldIndex;
 @Column(name="new_index",nullable=false,precision=10,scale=2) private java.math.BigDecimal newIndex;
 @Column(name="reading_date",nullable=false) private java.time.LocalDate readingDate;
 @Column(name="recorded_by",nullable=false) private Long recordedBy;
}
