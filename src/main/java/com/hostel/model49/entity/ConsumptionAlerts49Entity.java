package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="consumption_alerts") @Getter
public class ConsumptionAlerts49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="segment_id",nullable=false) private Long segmentId;
 @Column(name="policy_id",nullable=false) private Long policyId;
 @Column(name="supersedes_alert_id",nullable=true) private Long supersedesAlertId;
 @Column(name="baseline_daily_average",nullable=true,precision=18,scale=6) private java.math.BigDecimal baselineDailyAverage;
 @Column(name="applied_threshold",nullable=false,precision=9,scale=2) private java.math.BigDecimal appliedThreshold;
 @Column(name="method",nullable=false,length=40) private String method;
 @Column(name="status",nullable=false,length=30) private String status;
 @Column(name="context_note",nullable=true,columnDefinition="text") private String contextNote;
 @Column(name="checked_by",nullable=true) private Long checkedBy;
 @Column(name="checked_at",nullable=true) private java.time.Instant checkedAt;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
