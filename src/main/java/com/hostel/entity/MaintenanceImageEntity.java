package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="maintenance_images") @Getter
public class MaintenanceImageEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="maintenance_request_id",nullable=false) private Long maintenanceRequestId;
 @Column(name="url",nullable=false,columnDefinition="text") private String url;
 @Column(name="uploaded_by",nullable=false) private Long uploadedBy;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
