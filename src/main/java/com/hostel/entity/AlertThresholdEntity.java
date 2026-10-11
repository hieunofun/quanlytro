package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="alert_thresholds") @Getter
public class AlertThresholdEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="property_id") private PropertyEntity property;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="room_id") private RoomEntity room;
    @Column(name="threshold_percentage") private java.math.BigDecimal percentage;
}
