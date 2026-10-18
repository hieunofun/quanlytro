package com.hostel.entity;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="invoice_items") @Getter
public class InvoiceItemEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="invoice_id",nullable=false) private Long invoiceId;
 @Column(name="service_item_id",nullable=true) private Long serviceItemId;
 @Column(name="meter_reading_id",nullable=true) private Long meterReadingId;
 @Column(name="item_name_snapshot",nullable=false) private String itemNameSnapshot;
 @Column(name="unit_name_snapshot",nullable=false) private String unitNameSnapshot;
 @Column(name="unit_price_snapshot",nullable=false,precision=12,scale=2) private java.math.BigDecimal unitPriceSnapshot;
 @Column(name="quantity",nullable=true) private Integer quantity;
}
