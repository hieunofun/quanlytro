package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="service_items") @Getter
public class ServiceItemEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="service_code") private String code;
    private String name;
    @Column(name="calculation_type") private String calculationType;
}
