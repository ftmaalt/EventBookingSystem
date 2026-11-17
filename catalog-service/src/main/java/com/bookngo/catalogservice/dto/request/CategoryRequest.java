package com.bookngo.catalogservice.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {
    private String category_name;
    private String description;
}
