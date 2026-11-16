package com.mindhr.repository;

import com.mindhr.entity.ContractHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractHistoryRepository extends JpaRepository<ContractHistory, Long> {

    List<ContractHistory> findByContractIdOrderByChangedAtDesc(Long contractId);
}
