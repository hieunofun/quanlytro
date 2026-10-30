package com.hostel.model49.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import lombok.Getter;

@Entity @Immutable @Table(name="stored_files") @Getter
public class StoredFiles49Entity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 @Column(name="id",nullable=false) private Long id;
 @Column(name="storage_key",nullable=false,length=255) private String storageKey;
 @Column(name="original_name",nullable=false,length=255) private String originalName;
 @Column(name="content_type",nullable=false,length=100) private String contentType;
 @Column(name="size_bytes",nullable=true) private Long sizeBytes;
 @Column(name="sha256",nullable=true,length=64) private String sha256;
 @Column(name="external_url",nullable=true,columnDefinition="text") private String externalUrl;
 @Column(name="uploaded_by",nullable=false) private Long uploadedBy;
 @Column(name="uploaded_at",nullable=false) private java.time.Instant uploadedAt;
}
