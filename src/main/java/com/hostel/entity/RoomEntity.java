package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="rooms") @Getter
public class RoomEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="property_id") private PropertyEntity property;
    @Column(name="room_code") private String code;
    @Column(name="base_price") private java.math.BigDecimal basePrice;
    private String status;
}
