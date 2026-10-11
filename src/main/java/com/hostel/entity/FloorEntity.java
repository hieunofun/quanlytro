package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="floors") @Getter
public class FloorEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="property_id") private PropertyEntity property;
    @Column(name="floor_number") private Integer number;
    private String name;
}
