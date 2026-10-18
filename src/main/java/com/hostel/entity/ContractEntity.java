package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="contracts") @Getter
public class ContractEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="contract_code",nullable=false) private String contractCode;
 @Column(name="room_id",nullable=false) private Long roomId;
 @Column(name="primary_tenant_id",nullable=false) private Long primaryTenantId;
 @Column(name="start_date",nullable=false) private java.time.LocalDate startDate;
 @Column(name="end_date",nullable=false) private java.time.LocalDate endDate;
 @Column(name="actual_checkout_date",nullable=true) private java.time.LocalDate actualCheckoutDate;
 @Column(name="rental_price",nullable=false,precision=12,scale=2) private java.math.BigDecimal rentalPrice;
 @Column(name="deposit_amount",nullable=false,precision=12,scale=2) private java.math.BigDecimal depositAmount;
 @Column(name="payment_cycle_day",nullable=false) private Short paymentCycleDay;
 @Column(name="status",nullable=false) private String status;
 @Column(name="terms",nullable=false,columnDefinition="text") private String terms;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
