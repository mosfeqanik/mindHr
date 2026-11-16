package com.mindhr.repository;

import com.mindhr.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByOrganizationId(Long organizationId);

    boolean existsByInvoiceNumberIgnoreCase(String invoiceNumber);

    boolean existsByInvoiceNumberIgnoreCaseAndIdNot(String invoiceNumber, Long id);
}
