package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="service_items") @Getter
public class ServiceItemEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="service_code",nullable=false) private String serviceCode;
 @Column(name="name",nullable=false) private String name;
 @Column(name="calculation_type",nullable=false) private String calculationType;
 @Column(name="unit_name",nullable=false) private String unitName;
 @Column(name="is_mandatory",nullable=false) private Boolean isMandatory;
}
