package com.project.bookngo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long category_id;

    @Column(unique = true, length = 70)
    @NotBlank
    private String categoryName;

    @Column
    private String description;

    //    --- Relationship Mapping ---
    @OneToMany(mappedBy = "category")
    private List<Activities> activities;
}
