package com.project.bookngo.repository;

import com.project.bookngo.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category,Long> {
    Category findByCategoryName(String categoryName);
    Category findByCategoryNameAndDescription(String categoryName, String descriptionName);

}
