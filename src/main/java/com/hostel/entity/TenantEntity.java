package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="tenant_profiles") @Getter
public class TenantEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="full_name") private String fullName;
    @Column(name="identity_number") private String identityNumber;
    private String phone;
}
