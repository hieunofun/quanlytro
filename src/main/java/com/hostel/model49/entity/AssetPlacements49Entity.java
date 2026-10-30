package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="asset_placements") @Getter
public class AssetPlacements49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="asset_id",nullable=false) private Long assetId;
 @Column(name="room_id",nullable=true) private Long roomId;
 @Column(name="property_id",nullable=true) private Long propertyId;
 @Column(name="valid_from",nullable=false) private java.time.Instant validFrom;
 @Column(name="valid_to",nullable=true) private java.time.Instant validTo;
 @Column(name="recorded_by",nullable=true) private Long recordedBy;
}
