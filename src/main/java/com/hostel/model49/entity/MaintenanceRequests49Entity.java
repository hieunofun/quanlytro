package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="maintenance_requests") @Getter
public class MaintenanceRequests49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="room_id",nullable=true) private Long roomId;
 @Column(name="property_id",nullable=true) private Long propertyId;
 @Column(name="asset_placement_id",nullable=true) private Long assetPlacementId;
 @Column(name="schedule_id",nullable=true) private Long scheduleId;
 @Column(name="scheduled_for",nullable=true) private java.time.LocalDate scheduledFor;
 @Column(name="submitted_by",nullable=false) private Long submittedBy;
 @Column(name="assigned_to",nullable=true) private Long assignedTo;
 @Column(name="title",nullable=false,length=150) private String title;
 @Column(name="description",nullable=false,columnDefinition="text") private String description;
 @Column(name="priority",nullable=false,length=20) private String priority;
 @Column(name="status",nullable=false,length=20) private String status;
 @Column(name="incident_type",nullable=false,length=50) private String incidentType;
 @Column(name="expected_date",nullable=false) private java.time.LocalDate expectedDate;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
