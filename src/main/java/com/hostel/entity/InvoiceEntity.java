package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="invoices") @Getter
public class InvoiceEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contract_id") private ContractEntity contract;
    @Column(name="invoice_code") private String code;
    @Column(name="billing_period",length=7) private String period;
    @Column(name="total_amount") private java.math.BigDecimal totalAmount;
    @Column(name="paid_amount") private java.math.BigDecimal paidAmount;
    @Column(name="balance_due") private java.math.BigDecimal balanceDue;
    private String status;
    @OneToMany(mappedBy="invoice") private java.util.List<InvoiceItemEntity> items;
}
