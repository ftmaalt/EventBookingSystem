package com.project.bookngo.repository;

import com.project.bookngo.model.Activities;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivitiesRepository extends JpaRepository<Activities,Long> {
    List<Activities> findByProviderId(Long providerId);
    Optional<Activities> findByIdAndProviderId(Long id, Long providerId);
}
