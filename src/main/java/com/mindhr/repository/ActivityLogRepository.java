package com.mindhr.repository;

import com.mindhr.entity.ActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    /** Actor and organization are fetched in the same query, so listing does not do one query per row. */
    @EntityGraph(attributePaths = {"actor", "organization"})
    List<ActivityLog> findByOrganizationIdOrderByIdDesc(Long organizationId, Pageable pageable);

    @EntityGraph(attributePaths = {"actor", "organization"})
    List<ActivityLog> findAllByOrderByIdDesc(Pageable pageable);
}
