package com.project.bookngo.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 70)
    private String name;

    @Column
    private String description;

    //    --- Relationship Mapping ---
    @OneToMany(mappedBy = "category")
    private List<Activities> activities;
}
