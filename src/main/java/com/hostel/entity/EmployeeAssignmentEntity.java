package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="employee_assignments") @Getter
public class EmployeeAssignmentEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="user_id",nullable=false) private Long userId;
 @Column(name="property_id",nullable=false) private Long propertyId;
 @Column(name="assigned_at",nullable=false) private java.time.Instant assignedAt;
 @Column(name="is_active",nullable=false) private Boolean isActive;
}
