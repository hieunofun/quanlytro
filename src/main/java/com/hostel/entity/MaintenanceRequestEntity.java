package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="maintenance_requests") @Getter
public class MaintenanceRequestEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contract_id") private ContractEntity contract;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="property_id") private PropertyEntity property;
    private String title;
    private String status;
    private String priority;
    @Column(name="actual_cost") private java.math.BigDecimal actualCost;
}
