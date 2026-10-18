package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="tenant_profiles") @Getter
public class TenantEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="user_id",nullable=true) private Long userId;
 @Column(name="full_name",nullable=false) private String fullName;
 @Column(name="identity_number",nullable=true) private String identityNumber;
 @Column(name="phone",nullable=true) private String phone;
 @Column(name="email",nullable=true) private String email;
 @Column(name="date_of_birth",nullable=true) private java.time.LocalDate dateOfBirth;
 @Column(name="hometown",nullable=true) private String hometown;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
