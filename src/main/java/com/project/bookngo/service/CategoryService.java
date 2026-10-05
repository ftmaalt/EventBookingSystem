package com.project.bookngo.service;

import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.model.Category;
import com.project.bookngo.model.request.CategoryRequest;
import com.project.bookngo.model.response.CategoryResponse;
import com.project.bookngo.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    public CategoryResponse createCategory( CategoryRequest request){
        System.out.println("SERVICE: Calling createCategory ===>");
        Category categoryObject = new Category();
            categoryObject.setCategory_name(request.getCategory_name());
            categoryObject.setDescription(request.getDescription());
            categoryObject =categoryRepository.save(categoryObject);
            return toResponse(categoryObject);
    }

    public CategoryResponse getCategoryById(Long category_id){
        System.out.println("SERVICE: Calling getCategoryById ===>");
        Category category= categoryRepository.findById(category_id).orElseThrow(()->
                new InformationNotFoundException("Category with the id:"+ category_id +" does not exist, please try again with another category id")
        );
        return toResponse(category);
    }
    public List<CategoryResponse> getAllCategories(){
        System.out.println("SERVICE: Calling getAllCategories ===>");
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }
    public CategoryResponse updateCategory(Long category_id, CategoryRequest request){
        System.out.println("SERVICE: Calling updateCategory ===>");
        Category category= categoryRepository.findById(category_id).orElseThrow(()->
                new InformationNotFoundException("Category with the id:"+ category_id +" does not exist, please try again with another category id")
        );
        category.setCategoryName(request.getCategory_name());
        category.setDescription(request.getDescription());
        categoryRepository.save(category);
        return toResponse(category);
    }

    public String deleteCategory(Long category_id){
        System.out.println("SERVICE: Calling deleteCategory ===>");
        Category category= categoryRepository.findById(category_id).orElseThrow(()->
                new InformationNotFoundException("Category with the id:"+ category_id +" does not exist, please try again with another category id")
        );
        categoryRepository.delete(category);
        return "Category with id:"+ category_id +"has been deleted successfully";
    }

    private CategoryResponse toResponse(Category categoryObject) {
        return new CategoryResponse(categoryObject.getCategory_id(), categoryObject.getCategoryName(), categoryObject.getDescription());
    }
}
