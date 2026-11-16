package com.mindhr.repository;

import com.mindhr.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrganizationId(Long organizationId);

    List<Payment> findByInvoiceId(Long invoiceId);
}
