package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="contracts") @Getter
public class Contracts49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="room_id",nullable=false) private Long roomId;
 @Column(name="reservation_id",nullable=true) private Long reservationId;
 @Column(name="previous_contract_id",nullable=true) private Long previousContractId;
 @Column(name="continuation_reason",nullable=true,length=30) private String continuationReason;
 @Column(name="contract_code",nullable=false,length=60) private String contractCode;
 @Column(name="start_date",nullable=false) private java.time.LocalDate startDate;
 @Column(name="reconciliation_status",nullable=false,length=20) private String reconciliationStatus;
 @Column(name="status",nullable=false,length=20) private String status;
 @Column(name="created_by",nullable=true) private Long createdBy;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
