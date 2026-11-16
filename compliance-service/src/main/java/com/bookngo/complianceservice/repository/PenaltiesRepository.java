package com.bookngo.complianceservice.repository;

import com.bookngo.complianceservice.model.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PenaltiesRepository extends JpaRepository<Penalty, Long> {

    List<Penalty> findByUserIdOrderByCreatedAtDesc(Long userId);
}
