package com.mindhr.repository;

import com.mindhr.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByOrganizationId(Long organizationId);

    boolean existsByTicketNumberIgnoreCase(String ticketNumber);
}
