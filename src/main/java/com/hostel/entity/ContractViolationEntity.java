package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="contract_violations") @Getter
public class ContractViolationEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contract_id") private ContractEntity contract;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="recorded_by") private UserEntity recordedBy;
    private String reason;
    private String status;
    @Column(name="resolution_note") private String resolutionNote;
}
