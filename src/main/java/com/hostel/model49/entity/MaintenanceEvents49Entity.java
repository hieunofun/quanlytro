package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="maintenance_events") @Getter
public class MaintenanceEvents49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="request_id",nullable=false) private Long requestId;
 @Column(name="action",nullable=false,length=30) private String action;
 @Column(name="actor_id",nullable=false) private Long actorId;
 @Column(name="note",nullable=false,columnDefinition="text") private String note;
 @Column(name="recorded_at",nullable=false) private java.time.Instant recordedAt;
}
