package com.project.bookngo.repository;

import com.project.bookngo.model.ProviderApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderApplicationRepository extends JpaRepository<ProviderApplication, Long> {
    List<ProviderApplication> findByCreatedUserId(Long userId);
}
