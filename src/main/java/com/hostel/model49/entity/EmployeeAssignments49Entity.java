package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="employee_assignments") @Getter
public class EmployeeAssignments49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="user_id",nullable=false) private Long userId;
 @Column(name="property_id",nullable=true) private Long propertyId;
 @Column(name="room_id",nullable=true) private Long roomId;
 @Column(name="assigned_at",nullable=false) private java.time.Instant assignedAt;
 @Column(name="revoked_at",nullable=true) private java.time.Instant revokedAt;
}
