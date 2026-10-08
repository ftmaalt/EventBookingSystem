package com.project.bookngo.repository;

import com.project.bookngo.model.Activities;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActivitiesRepository extends JpaRepository<Activities,Long> {
    List<Activities> findByProviderId(Long providerId);
    Optional<Activities> findByIdAndProviderId(Long id, Long providerId);
    @Query("SELECT a FROM Activities a WHERE " + "(:categoryId IS NULL OR a.category.id = :categoryId) " + "AND (:locationId IS NULL OR a.location.id = :locationId)")
    Page<Activities> search(@Param("categoryId") Long categoryId, @Param("locationId") Long locationId, Pageable pageable);
}
