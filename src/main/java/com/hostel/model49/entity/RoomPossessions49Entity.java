package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="room_possessions") @Getter
public class RoomPossessions49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="contract_id",nullable=false) private Long contractId;
 @Column(name="taken_at",nullable=false) private java.time.Instant takenAt;
 @Column(name="returned_at",nullable=true) private java.time.Instant returnedAt;
 @Column(name="recorded_by",nullable=true) private Long recordedBy;
 @Column(name="note",nullable=true,columnDefinition="text") private String note;
}
