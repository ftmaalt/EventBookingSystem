package com.bookngo.catalogservice.service;

import com.bookngo.catalogservice.exception.BusinessConflictsException;
import com.bookngo.catalogservice.exception.InformationNotFoundException;
import com.bookngo.catalogservice.model.Category;
import com.bookngo.catalogservice.dto.request.CategoryRequest;
import com.bookngo.catalogservice.dto.response.CategoryResponse;
import com.bookngo.catalogservice.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Locale;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse createCategory( CategoryRequest request){
        String categoryName = normalizeName(request.getCategory_name());
        logger.info("Creating category: {}", request.getCategory_name());
        if (categoryRepository.existsByCategoryNameIgnoreCase(categoryName)) {
            throw new BusinessConflictsException("A category with this name already exists.");
        }

        Category categoryObject = new Category();
            categoryObject.setCategoryName(request.getCategory_name());
            categoryObject.setDescription(request.getDescription());
            categoryObject =categoryRepository.save(categoryObject);

            logger.info("Category created successfully with ID: {}", categoryObject.getId());
            return toResponse(categoryObject);
    }
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long category_id){
        logger.info("Fetching category with ID: {}", category_id);
        Category category= categoryRepository.findById(category_id).orElseThrow(()->
                new InformationNotFoundException("Category with the id:"+ category_id +" does not exist, please try again with another category id")
        );
        return toResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(){
        logger.info("Fetching all categories");
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse updateCategory(Long category_id, CategoryRequest request){
        logger.info("Updating category with ID: {}", category_id);
        Category category= categoryRepository.findById(category_id).orElseThrow(()->
                new InformationNotFoundException("Category with the id:"+ category_id +" does not exist, please try again with another category id")
        );
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new BusinessConflictsException("Cannot update an inactive category.");
        }
        String newName = normalizeName(request.getCategory_name());

        if (!category.getCategoryName().equalsIgnoreCase(newName)
                && categoryRepository.existsByCategoryNameIgnoreCase(newName)) {
            throw new BusinessConflictsException("A category with this name already exists.");
        }
        category.setCategoryName(request.getCategory_name());
        category.setDescription(request.getDescription());
        categoryRepository.save(category);
        logger.info("Category with ID {} updated successfully", category_id);
        return toResponse(category);
    }


    @Transactional
    public String deleteCategory(Long category_id) {
        Category category = categoryRepository.findById(category_id)
                .orElseThrow(() -> new InformationNotFoundException("Category with ID: " + category_id + " does not exist."));

        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new BusinessConflictsException("Category is already inactive.");
        }

        category.setActive(false);
        categoryRepository.save(category);

        logger.info("Category with ID {} deactivated successfully", category_id);
        return "Category with ID: " + category_id + " has been deactivated.";
    }

    private String normalizeName(String name) {
        return name.trim();
    }


    private CategoryResponse toResponse(Category categoryObject) {
        return new CategoryResponse(categoryObject.getId(), categoryObject.getCategoryName(), categoryObject.getDescription());
    }
}
