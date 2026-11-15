package com.bookngo.catalogservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "locations")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "google_place_id", columnDefinition = "text")
    private String googlePlaceId;


//    --- Relationship Mapping ---
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

}
