package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="reservations") @Getter
public class Reservations49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="room_id",nullable=false) private Long roomId;
 @Column(name="tenant_id",nullable=true) private Long tenantId;
 @Column(name="reservation_code",nullable=false,length=60) private String reservationCode;
 @Column(name="contact_name",nullable=true,length=150) private String contactName;
 @Column(name="contact_phone",nullable=true,length=30) private String contactPhone;
 @Column(name="start_date",nullable=false) private java.time.LocalDate startDate;
 @Column(name="end_date",nullable=false) private java.time.LocalDate endDate;
 @Column(name="expires_at",nullable=false) private java.time.Instant expiresAt;
 @Column(name="status",nullable=false,length=20) private String status;
 @Column(name="conditions",nullable=false,columnDefinition="text") private String conditions;
 @Column(name="created_by",nullable=true) private Long createdBy;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
