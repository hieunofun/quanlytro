package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="room_price_history") @Getter
public class RoomPriceHistory49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="room_id",nullable=false) private Long roomId;
 @Column(name="effective_from",nullable=false) private java.time.LocalDate effectiveFrom;
 @Column(name="amount",nullable=false,precision=14,scale=2) private java.math.BigDecimal amount;
 @Column(name="recorded_by",nullable=true) private Long recordedBy;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
