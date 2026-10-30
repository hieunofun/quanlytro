package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="service_items") @Getter
public class ServiceItems49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="service_code",nullable=false,length=30) private String serviceCode;
 @Column(name="name",nullable=false,length=100) private String name;
 @Column(name="unit_name",nullable=false,length=30) private String unitName;
 @Column(name="calculation_type",nullable=false,length=20) private String calculationType;
 @Column(name="is_mandatory",nullable=false) private Boolean isMandatory;
 @Column(name="is_active",nullable=false) private Boolean isActive;
}
