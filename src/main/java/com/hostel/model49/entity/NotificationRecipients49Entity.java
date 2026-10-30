package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="notification_recipients") @Getter
public class NotificationRecipients49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="notification_id",nullable=false) private Long notificationId;
 @Column(name="user_id",nullable=false) private Long userId;
 @Column(name="read_at",nullable=true) private java.time.Instant readAt;
}
