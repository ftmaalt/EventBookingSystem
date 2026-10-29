package com.project.bookngo.repository;

import com.project.bookngo.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByProviderIdAndName(Long providerId, String name);

        List<Location> findByActiveTrue();
        Optional<Location> findByIdAndActiveTrue(Long id);
    }


