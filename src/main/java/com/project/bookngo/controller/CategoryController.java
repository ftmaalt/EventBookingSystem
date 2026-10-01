package com.project.bookngo.controller;



import com.project.bookngo.model.request.CategoryRequest;
import com.project.bookngo.model.response.CategoryResponse;
import com.project.bookngo.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



import java.util.List;

@RestController
@RequestMapping(path = "/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;


    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        System.out.println("Calling createCategory ===>");
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{category_id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long category_id) {
        System.out.println("Calling getCategoryById ===>");
        CategoryResponse response=categoryService.getCategoryById(category_id);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        System.out.println("Calling getAllCategories ===>");
        List<CategoryResponse> response=categoryService.getAllCategories();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{category_id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long category_id, @Valid @RequestBody CategoryRequest request) {
        System.out.println("Calling updateCategory ===>");
        CategoryResponse response=categoryService.updateCategory(category_id, request);
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/{category_id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long category_id) {
        System.out.println("Calling deleteCategory ===>");
        String message= categoryService.deleteCategory(category_id);
        return ResponseEntity.ok(message);
    }
}