package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="handover_items") @Getter
public class HandoverItems49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="handover_id",nullable=false) private Long handoverId;
 @Column(name="asset_placement_id",nullable=true) private Long assetPlacementId;
 @Column(name="line_no",nullable=false) private Integer lineNo;
 @Column(name="item_name_snapshot",nullable=false,length=150) private String itemNameSnapshot;
 @Column(name="quantity",nullable=false,precision=12,scale=2) private java.math.BigDecimal quantity;
 @Column(name="condition_note",nullable=false,columnDefinition="text") private String conditionNote;
 @Column(name="note",nullable=true,columnDefinition="text") private String note;
}
