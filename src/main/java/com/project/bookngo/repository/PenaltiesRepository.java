package com.project.bookngo.repository;

import com.project.bookngo.model.Penalties;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PenaltiesRepository extends JpaRepository<Penalties, Long> {
    List<Penalties> findByUserId(Long userId);
}