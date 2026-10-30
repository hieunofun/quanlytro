package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="rooms") @Getter
public class Rooms49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="property_id",nullable=false) private Long propertyId;
 @Column(name="floor_number",nullable=false) private Short floorNumber;
 @Column(name="room_code",nullable=false,length=30) private String roomCode;
 @Column(name="max_occupants",nullable=false) private Short maxOccupants;
 @Column(name="area_sqm",nullable=true,precision=8,scale=2) private java.math.BigDecimal areaSqm;
 @Column(name="description",nullable=false,columnDefinition="text") private String description;
 @Column(name="internal_note",nullable=false,columnDefinition="text") private String internalNote;
 @Column(name="opened_on",nullable=false) private java.time.LocalDate openedOn;
 @Column(name="retired_on",nullable=true) private java.time.LocalDate retiredOn;
}
