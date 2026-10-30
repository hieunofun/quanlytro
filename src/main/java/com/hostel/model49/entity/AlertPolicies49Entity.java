package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="alert_policies") @Getter
public class AlertPolicies49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="service_item_id",nullable=false) private Long serviceItemId;
 @Column(name="property_id",nullable=true) private Long propertyId;
 @Column(name="room_id",nullable=true) private Long roomId;
 @Column(name="effective_from",nullable=false) private java.time.LocalDate effectiveFrom;
 @Column(name="history_months",nullable=false) private Short historyMonths;
 @Column(name="minimum_days",nullable=false) private Integer minimumDays;
 @Column(name="threshold_percentage",nullable=false,precision=9,scale=2) private java.math.BigDecimal thresholdPercentage;
 @Column(name="absolute_daily_threshold",nullable=true,precision=14,scale=3) private java.math.BigDecimal absoluteDailyThreshold;
}
