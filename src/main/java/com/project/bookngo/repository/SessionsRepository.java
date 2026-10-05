package com.project.bookngo.repository;

import com.project.bookngo.model.Activities;
import com.project.bookngo.model.Sessions;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SessionsRepository extends JpaRepository<Sessions, Long> {
    List<Sessions> findByActivityTitle(String title);
    List<Sessions> findByActivityId(Long activity_id);
// to prevent double booking:
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sessions s where s.id = :id")
    Optional<Sessions> findByIdForUpdate(@Param("id") Long id);

}
