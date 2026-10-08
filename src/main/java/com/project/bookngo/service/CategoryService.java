package com.project.bookngo.service;

import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.model.Category;
import com.project.bookngo.model.request.CategoryRequest;
import com.project.bookngo.model.response.CategoryResponse;
import com.project.bookngo.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;
    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    public CategoryResponse createCategory( CategoryRequest request){
        logger.info("Creating category: {}", request.getCategory_name());
        Category categoryObject = new Category();
            categoryObject.setCategoryName(request.getCategory_name());
            categoryObject.setDescription(request.getDescription());
            categoryObject =categoryRepository.save(categoryObject);
        logger.info("Category created successfully with ID: {}", categoryObject.getId());
            return toResponse(categoryObject);
    }

    public CategoryResponse getCategoryById(Long category_id){
        logger.info("Fetching category with ID: {}", category_id);
        Category category= categoryRepository.findById(category_id).orElseThrow(()->
                new InformationNotFoundException("Category with the id:"+ category_id +" does not exist, please try again with another category id")
        );
        return toResponse(category);
    }
    public List<CategoryResponse> getAllCategories(){
        logger.info("Fetching all categories");
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }
    public CategoryResponse updateCategory(Long category_id, CategoryRequest request){
        logger.info("Updating category with ID: {}", category_id);
        Category category= categoryRepository.findById(category_id).orElseThrow(()->
                new InformationNotFoundException("Category with the id:"+ category_id +" does not exist, please try again with another category id")
        );
        category.setCategoryName(request.getCategory_name());
        category.setDescription(request.getDescription());
        categoryRepository.save(category);
        logger.info("Category with ID {} updated successfully", category_id);
        return toResponse(category);
    }

    public String deleteCategory(Long category_id){
        logger.info("Deleting category with ID: {}", category_id);
        Category category= categoryRepository.findById(category_id).orElseThrow(()->
                new InformationNotFoundException("Category with the id:"+ category_id +" does not exist, please try again with another category id")
        );
        categoryRepository.delete(category);
        logger.info("Category with ID {} deleted successfully", category_id);
        return "Category with id:"+ category_id +"has been deleted successfully";
    }

    private CategoryResponse toResponse(Category categoryObject) {
        return new CategoryResponse(categoryObject.getId(), categoryObject.getCategoryName(), categoryObject.getDescription());
    }
}
