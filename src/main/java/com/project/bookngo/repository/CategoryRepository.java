package com.project.bookngo.repository;

import com.project.bookngo.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category,Long> {
    Category findByCategoryName(String categoryName);
    Category findByCategoryNameAndDescription(String categoryName, String descriptionName);
    List<Category> findByActiveTrue();
    Optional<Category> findByIdAndActiveTrue(Long id);

}
