package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="properties") @Getter
public class PropertyEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="property_code",nullable=false) private String propertyCode;
 @Column(name="name",nullable=false) private String name;
 @Column(name="address",nullable=false,columnDefinition="text") private String address;
 @Column(name="status",nullable=false) private String status;
 @Column(name="meter_cutoff_day",nullable=false) private Short meterCutoffDay;
 @Column(name="invoice_issue_day",nullable=false) private Short invoiceIssueDay;
 @Column(name="alert_threshold_percentage",nullable=false,precision=7,scale=2) private java.math.BigDecimal alertThresholdPercentage;
 @Column(name="alert_history_months",nullable=false) private Short alertHistoryMonths;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
