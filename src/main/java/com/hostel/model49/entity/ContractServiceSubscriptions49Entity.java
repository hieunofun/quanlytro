package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="contract_service_subscriptions") @Getter
public class ContractServiceSubscriptions49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="contract_id",nullable=false) private Long contractId;
 @Column(name="service_item_id",nullable=false) private Long serviceItemId;
 @Column(name="effective_from",nullable=false) private java.time.LocalDate effectiveFrom;
 @Column(name="effective_to",nullable=true) private java.time.LocalDate effectiveTo;
 @Column(name="quantity",nullable=false) private Integer quantity;
 @Column(name="charge_mode",nullable=false,length=20) private String chargeMode;
 @Column(name="recorded_by",nullable=true) private Long recordedBy;
}
