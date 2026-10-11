package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="contracts") @Getter
public class ContractEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="room_id") private RoomEntity room;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="primary_tenant_id") private TenantEntity tenant;
    @Column(name="contract_code") private String code;
    @Column(name="start_date") private java.time.LocalDate startDate;
    @Column(name="end_date") private java.time.LocalDate endDate;
    @Column(name="rental_price") private java.math.BigDecimal rentalPrice;
    private String status;
}
