package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="rooms") @Getter
public class RoomEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="property_id",nullable=false) private Long propertyId;
 @Column(name="floor_number",nullable=false) private Short floorNumber;
 @Column(name="room_code",nullable=false) private String roomCode;
 @Column(name="base_price",nullable=false,precision=12,scale=2) private java.math.BigDecimal basePrice;
 @Column(name="max_occupants",nullable=false) private Short maxOccupants;
 @Column(name="area_sqm",nullable=true,precision=6,scale=2) private java.math.BigDecimal areaSqm;
 @Column(name="operational_status",nullable=false) private String operationalStatus;
 @Column(name="description",nullable=false,columnDefinition="text") private String description;
 @Column(name="internal_note",nullable=false,columnDefinition="text") private String internalNote;
}
