package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="room_employee_assignments") @Getter
public class RoomEmployeeAssignmentEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private UserEntity user;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="room_id") private RoomEntity room;
    @Column(name="is_active") private Boolean active;
}
