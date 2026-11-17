package com.bookngo.notificationservice.repository;

import com.bookngo.notificationservice.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
