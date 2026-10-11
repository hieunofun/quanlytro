package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="contract_members") @Getter
public class ContractMemberEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contract_id") private ContractEntity contract;
    @Column(name="full_name") private String fullName;
    @Column(name="effective_from") private java.time.LocalDate effectiveFrom;
    @Column(name="effective_to") private java.time.LocalDate effectiveTo;
}
