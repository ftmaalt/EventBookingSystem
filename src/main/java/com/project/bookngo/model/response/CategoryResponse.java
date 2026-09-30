package com.project.bookngo.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CategoryResponse {
    private Long category_id;
    private String category_name;
    private String description;
}
