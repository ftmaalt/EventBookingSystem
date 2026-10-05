package com.project.bookngo.repository;

import com.project.bookngo.model.Activities;
import com.project.bookngo.model.Sessions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionsRepository extends JpaRepository<Sessions, Long> {
    List<Sessions> findByActivityTitle(String title);
    List<Sessions> findByActivityId(Long activity_id);

}
