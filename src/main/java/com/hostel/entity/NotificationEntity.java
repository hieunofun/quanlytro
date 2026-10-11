package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="notifications") @Getter
public class NotificationEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private UserEntity user;
    private String type;
    @Column(columnDefinition="text") private String message;
    @Column(name="is_read") private Boolean read;
}
