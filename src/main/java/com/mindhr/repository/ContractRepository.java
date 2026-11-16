package com.mindhr.repository;

import com.mindhr.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByOrganizationId(Long organizationId);
}
