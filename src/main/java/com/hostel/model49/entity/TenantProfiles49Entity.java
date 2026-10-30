package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="tenant_profiles") @Getter
public class TenantProfiles49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="user_id",nullable=true) private Long userId;
 @Column(name="full_name",nullable=false,length=150) private String fullName;
 @Column(name="identity_number",nullable=true,length=30) private String identityNumber;
 @Column(name="phone",nullable=true,length=30) private String phone;
 @Column(name="email",nullable=true,length=150) private String email;
 @Column(name="date_of_birth",nullable=true) private java.time.LocalDate dateOfBirth;
 @Column(name="hometown",nullable=true,columnDefinition="text") private String hometown;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
