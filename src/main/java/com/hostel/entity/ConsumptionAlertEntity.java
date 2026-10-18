package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="consumption_alerts") @Getter
public class ConsumptionAlertEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="meter_reading_id",nullable=false) private Long meterReadingId;
 @Column(name="baseline_average",nullable=false,precision=12,scale=2) private java.math.BigDecimal baselineAverage;
 @Column(name="applied_threshold_percentage",nullable=false,precision=7,scale=2) private java.math.BigDecimal appliedThresholdPercentage;
 @Column(name="history_months",nullable=false) private Short historyMonths;
 @Column(name="status",nullable=false) private String status;
 @Column(name="checked_by",nullable=true) private Long checkedBy;
 @Column(name="checked_at",nullable=true) private java.time.Instant checkedAt;
 @Column(name="note",nullable=true,columnDefinition="text") private String note;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
