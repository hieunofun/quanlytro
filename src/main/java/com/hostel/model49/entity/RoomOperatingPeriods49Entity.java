package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="room_operating_periods") @Getter
public class RoomOperatingPeriods49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="room_id",nullable=false) private Long roomId;
 @Column(name="valid_from",nullable=false) private java.time.Instant validFrom;
 @Column(name="valid_to",nullable=true) private java.time.Instant validTo;
 @Column(name="status",nullable=false,length=20) private String status;
 @Column(name="recorded_by",nullable=true) private Long recordedBy;
 @Column(name="note",nullable=true,columnDefinition="text") private String note;
}
