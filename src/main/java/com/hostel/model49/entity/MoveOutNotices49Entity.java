package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="move_out_notices") @Getter
public class MoveOutNotices49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="contract_id",nullable=false) private Long contractId;
 @Column(name="supersedes_notice_id",nullable=true) private Long supersedesNoticeId;
 @Column(name="requested_date",nullable=false) private java.time.LocalDate requestedDate;
 @Column(name="reason",nullable=false,columnDefinition="text") private String reason;
 @Column(name="status",nullable=false,length=20) private String status;
 @Column(name="submitted_by",nullable=false) private Long submittedBy;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
