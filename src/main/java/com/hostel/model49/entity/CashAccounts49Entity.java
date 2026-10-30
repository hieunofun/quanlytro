package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="cash_accounts") @Getter
public class CashAccounts49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="property_id",nullable=true) private Long propertyId;
 @Column(name="account_code",nullable=false,length=60) private String accountCode;
 @Column(name="name",nullable=false,length=150) private String name;
 @Column(name="account_type",nullable=false,length=20) private String accountType;
 @Column(name="is_active",nullable=false) private Boolean isActive;
}
