package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="meters") @Getter
public class Meters49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="room_id",nullable=false) private Long roomId;
 @Column(name="service_item_id",nullable=false) private Long serviceItemId;
 @Column(name="meter_code",nullable=false,length=60) private String meterCode;
 @Column(name="installed_at",nullable=false) private java.time.Instant installedAt;
 @Column(name="retired_at",nullable=true) private java.time.Instant retiredAt;
 @Column(name="recorded_by",nullable=true) private Long recordedBy;
}
