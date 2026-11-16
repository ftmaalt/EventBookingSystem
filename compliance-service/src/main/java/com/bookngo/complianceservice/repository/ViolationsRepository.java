package com.bookngo.complianceservice.repository;

import com.bookngo.complianceservice.model.Violation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViolationsRepository extends JpaRepository<Violation, Long> {
}
