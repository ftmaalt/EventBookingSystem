package com.project.bookngo.repository;

import com.project.bookngo.model.Activities;
import com.project.bookngo.model.enums.ActivityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActivitiesRepository extends JpaRepository<Activities,Long> {
    Optional<Activities> findByProviderIdAndTitle(Long providerId, String title);

    List<Activities> findByProviderId(Long providerId);
    List<Activities> findByStatus(ActivityStatus status);
    Optional<Activities> findByIdAndStatus(Long id, ActivityStatus status);
    @Query("""
    SELECT a FROM Activities a
    WHERE a.status = com.project.bookngo.model.enums.ActivityStatus.ACTIVE
      AND a.category.active = true
      AND a.location.active = true
      AND (:categoryId IS NULL OR a.category.id = :categoryId)
      AND (:locationId IS NULL OR a.location.id = :locationId)
    """)
    Page<Activities> search(
            @Param("categoryId") Long categoryId,
            @Param("locationId") Long locationId,
            Pageable pageable
    );
}
