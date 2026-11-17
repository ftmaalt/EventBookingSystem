package com.bookngo.catalogservice.repository;

import com.bookngo.catalogservice.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category,Long> {
    boolean existsByCategoryNameIgnoreCase(String categoryName);
    List<Category> findByActiveTrue();
    Optional<Category> findByIdAndActiveTrue(Long id);
}
