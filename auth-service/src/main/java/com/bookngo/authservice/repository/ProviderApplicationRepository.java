package com.bookngo.authservice.repository;

import com.bookngo.catalogservice.model.model.ProviderApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderApplicationRepository extends JpaRepository<ProviderApplication, Long> {
    List<ProviderApplication> findByCreatedUserId(Long userId);
}
