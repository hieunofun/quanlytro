package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="alert_baseline_sources") @Getter
public class AlertBaselineSources49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="alert_id",nullable=false) private Long alertId;
 @Column(name="source_segment_id",nullable=false) private Long sourceSegmentId;
}
