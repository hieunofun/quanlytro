package com.hostel.repository;
import com.hostel.entity.InvoiceItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InvoiceItemRepository extends JpaRepository<InvoiceItemEntity,Long> {
    List<InvoiceItemEntity> findByInvoiceIdOrderByIdAsc(Long invoiceId);
}
