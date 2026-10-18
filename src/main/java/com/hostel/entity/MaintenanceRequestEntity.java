package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="maintenance_requests") @Getter
public class MaintenanceRequestEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="contract_id",nullable=false) private Long contractId;
 @Column(name="submitted_by",nullable=false) private Long submittedBy;
 @Column(name="assigned_to",nullable=true) private Long assignedTo;
 @Column(name="title",nullable=false) private String title;
 @Column(name="description",nullable=false,columnDefinition="text") private String description;
 @Column(name="priority",nullable=false) private String priority;
 @Column(name="status",nullable=false) private String status;
 @Column(name="expected_date",nullable=false) private java.time.LocalDate expectedDate;
 @Column(name="actual_cost",nullable=false,precision=12,scale=2) private java.math.BigDecimal actualCost;
 @Column(name="progress_note",nullable=true,columnDefinition="text") private String progressNote;
 @Column(name="created_at",nullable=false) private java.time.Instant createdAt;
}
