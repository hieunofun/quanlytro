package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="employee_assignments") @Getter
public class EmployeeAssignmentEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private UserEntity user;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="property_id") private PropertyEntity property;
    @Column(name="is_active") private Boolean active;
}
