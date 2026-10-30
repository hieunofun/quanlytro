package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="properties") @Getter
public class Properties49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="property_code",nullable=false,length=30) private String propertyCode;
 @Column(name="name",nullable=false,length=150) private String name;
 @Column(name="address",nullable=false,columnDefinition="text") private String address;
 @Column(name="status",nullable=false,length=20) private String status;
 @Column(name="meter_cutoff_day",nullable=false) private Short meterCutoffDay;
 @Column(name="invoice_issue_day",nullable=false) private Short invoiceIssueDay;
 @Column(name="collection_timing",nullable=false,length=20) private String collectionTiming;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
