package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="maintenance_schedules") @Getter
public class MaintenanceSchedules49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="asset_id",nullable=true) private Long assetId;
 @Column(name="room_id",nullable=true) private Long roomId;
 @Column(name="property_id",nullable=true) private Long propertyId;
 @Column(name="title",nullable=false,length=150) private String title;
 @Column(name="interval_days",nullable=false) private Integer intervalDays;
 @Column(name="next_due",nullable=false) private java.time.LocalDate nextDue;
 @Column(name="is_active",nullable=false) private Boolean isActive;
 @Column(name="created_by",nullable=true) private Long createdBy;
}
