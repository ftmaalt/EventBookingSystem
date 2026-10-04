package com.project.bookngo.repository;

import com.project.bookngo.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category,Long> {
    Category findByName(String categoryName);
    Category findByNameAndDescription(String categoryName, String descriptionName);
    Category findByUserIdAndName(Long userId, String categoryName);
    Category findByUserIdAndId(Long userId, Long id);
    List<Category> findByUserId(Long userId);
}
