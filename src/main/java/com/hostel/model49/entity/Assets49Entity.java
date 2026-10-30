package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="assets") @Getter
public class Assets49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="asset_type_id",nullable=false) private Long assetTypeId;
 @Column(name="asset_code",nullable=false,length=50) private String assetCode;
 @Column(name="quantity",nullable=false,precision=12,scale=2) private java.math.BigDecimal quantity;
 @Column(name="status",nullable=false,length=20) private String status;
 @Column(name="note",nullable=true,columnDefinition="text") private String note;
}
