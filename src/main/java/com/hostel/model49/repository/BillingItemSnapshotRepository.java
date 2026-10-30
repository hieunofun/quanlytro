package com.hostel.model49.repository;
import com.hostel.model49.entity.BillingItems49Entity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BillingItemSnapshotRepository extends JpaRepository<BillingItems49Entity,Long> {
 long countByDocumentId(Long documentId);
}
