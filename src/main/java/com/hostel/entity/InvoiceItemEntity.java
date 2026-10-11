package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="invoice_items") @Getter
public class InvoiceItemEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="invoice_id") private InvoiceEntity invoice;
    @Column(name="item_name_snapshot") private String name;
    @Column(name="unit_price_snapshot") private java.math.BigDecimal price;
    private java.math.BigDecimal quantity;
    private java.math.BigDecimal subtotal;
    @Column(name="old_index_snapshot") private java.math.BigDecimal oldIndex;
    @Column(name="new_index_snapshot") private java.math.BigDecimal newIndex;
    @Column(name="formula_description") private String formula;
}
