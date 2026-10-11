package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="meter_readings") @Getter
public class MeterReadingEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contract_id") private ContractEntity contract;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="room_id") private RoomEntity room;
    @Column(name="billing_period",length=7) private String period;
    @Column(name="old_index") private java.math.BigDecimal oldIndex;
    @Column(name="new_index") private java.math.BigDecimal newIndex;
    private java.math.BigDecimal consumption;
}
