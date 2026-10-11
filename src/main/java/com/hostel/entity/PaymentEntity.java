package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="payment_transactions") @Getter
public class PaymentEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="invoice_id") private InvoiceEntity invoice;
    private java.math.BigDecimal amount;
    @Column(name="transaction_code") private String code;
}
