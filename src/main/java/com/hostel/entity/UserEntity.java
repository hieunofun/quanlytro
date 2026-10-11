package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="users") @Getter
public class UserEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private String username;
    @Column(name="full_name") private String fullName;
    private String role;
    private String status;
}
