package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="room_images") @Getter
public class RoomImageEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="room_id") private RoomEntity room;
    @Column(columnDefinition="text") private String url;
}
