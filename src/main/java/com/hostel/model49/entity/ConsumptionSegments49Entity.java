package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="consumption_segments") @Getter
public class ConsumptionSegments49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="start_reading_id",nullable=false) private Long startReadingId;
 @Column(name="end_reading_id",nullable=false) private Long endReadingId;
 @Column(name="contract_id",nullable=true) private Long contractId;
 @Column(name="responsibility",nullable=false,length=10) private String responsibility;
 @Column(name="supersedes_segment_id",nullable=true) private Long supersedesSegmentId;
 @Column(name="recorded_by",nullable=true) private Long recordedBy;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
