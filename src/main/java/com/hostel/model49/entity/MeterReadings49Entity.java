package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="meter_readings") @Getter
public class MeterReadings49Entity {
 @Column(name="correction_item_id") private Long correctionItemId;
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="meter_id",nullable=false) private Long meterId;
 @Column(name="observed_at",nullable=false) private java.time.Instant observedAt;
 @Column(name="reading_value",nullable=false,precision=14,scale=3) private java.math.BigDecimal readingValue;
 @Column(name="time_precision",nullable=false,length=30) private String timePrecision;
 @Column(name="revision_no",nullable=false) private Integer revisionNo;
 @Column(name="supersedes_reading_id",nullable=true) private Long supersedesReadingId;
 @Column(name="handover_id",nullable=true) private Long handoverId;
 @Column(name="recorded_by",nullable=false) private Long recordedBy;
 @Column(name="reason",nullable=true,columnDefinition="text") private String reason;
 @Column(name="recorded_at",nullable=false) private java.time.Instant recordedAt;
}
