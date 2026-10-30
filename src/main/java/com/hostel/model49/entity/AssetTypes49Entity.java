package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="asset_types") @Getter
public class AssetTypes49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="code",nullable=false,length=40) private String code;
 @Column(name="name",nullable=false,length=150) private String name;
 @Column(name="unit_name",nullable=false,length=30) private String unitName;
}
