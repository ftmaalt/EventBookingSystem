package com.project.bookngo.service;

import com.project.bookngo.model.Location;
import com.project.bookngo.model.request.LocationRequest;
import com.project.bookngo.model.response.LocationResponse;
import com.project.bookngo.repository.LocationRepository;
import com.project.bookngo.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationService {

    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private GeocodingService geocodingService;

    public LocationResponse create(LocationRequest request) {
        // needs Provider
        return null;
    }

    public LocationResponse getById(Long id) {
        // same orElseThrow pattern as Category
        return null;
    }

    public List<LocationResponse> getAll() {
        return null;
    }

    public LocationResponse update(Long id, LocationRequest request) {
        return null;
    }

    public void delete(Long id) {
        return;
    }

    private LocationResponse toResponse(Location location) {
        return null;
    }
}