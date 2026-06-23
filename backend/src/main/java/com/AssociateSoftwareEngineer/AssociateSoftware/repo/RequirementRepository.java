package com.AssociateSoftwareEngineer.AssociateSoftware.repo;

import com.AssociateSoftwareEngineer.AssociateSoftware.entity.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, Long> {
}
