package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="meter_reading_history") @Getter
public class MeterReadingHistoryEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="meter_reading_id") private MeterReadingEntity reading;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="changed_by") private UserEntity changedBy;
    @Column(name="old_value") private java.math.BigDecimal oldValue;
    @Column(name="new_value") private java.math.BigDecimal newValue;
    @Column(columnDefinition="text") private String reason;
}
