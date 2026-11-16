package com.mindhr.repository;

import com.mindhr.entity.Organization;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    boolean existsBySlugIgnoreCase(String slug);

    /** Loads the owner in the same query, since the list shows the owner's name. */
    @Override
    @EntityGraph(attributePaths = "owner")
    List<Organization> findAll();
}
