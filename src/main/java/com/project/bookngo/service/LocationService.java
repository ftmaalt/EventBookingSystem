package com.project.bookngo.service;

import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.model.Category;
import com.project.bookngo.model.Location;
import com.project.bookngo.model.User;
import com.project.bookngo.model.request.LocationRequest;
import com.project.bookngo.model.response.LocationResponse;
import com.project.bookngo.repository.LocationRepository;
import com.project.bookngo.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class LocationService {

    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private GeocodingService geocodingService;

    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);

    public LocationResponse create(LocationRequest request) {
        logger.info("Creating location");
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user= usersRepository.findUserByEmail(email);
        String address=request.getAddress()+ ", "+ request.getCity();
        double[] coords = geocodingService.geocodeAddress(address);
        Location location= new Location();
        location.setName(request.getName());
        location.setAddress(request.getAddress());
        location.setCity(request.getCity());
        location.setProvider(user);
        location.setLatitude(coords[0]);
        location.setLongitude(coords[1]);
        location.setActive(true);

        Location savedLocation = locationRepository.save(location);
        logger.info("Location created successfully with ID: {}", savedLocation.getId());
        return toResponse(savedLocation);
    }

    public LocationResponse getById(Long id) {
        logger.info("Fetching location with ID: {}", id);
        Location spot= locationRepository.findById(id).orElseThrow(()->
                new InformationNotFoundException("Location with the id:"+ id +" does not exist, please try again with another id")
        );
        return toResponse(spot);
    }

    public List<LocationResponse> getAll() {
        logger.info("Fetching all locations");
        return locationRepository.findAll().stream().map(this::toResponse).toList();
    }

    public LocationResponse update(Long id, LocationRequest request) {
        logger.info("Updating location with ID: {}", id);
        Location location = locationRepository.findById(id).orElseThrow(() ->
                new InformationNotFoundException("Location with the id:" + id + " does not exist, please try again with another id"));
        String address = request.getAddress() + ", " + request.getCity();
        double[] coords = geocodingService.geocodeAddress(address);

        location.setName(request.getName());
        location.setAddress(request.getAddress());
        location.setCity(request.getCity());
        location.setLatitude(coords[0]);
        location.setLongitude(coords[1]);

        Location updatedLocation = locationRepository.save(location);
        logger.info("Location with ID {} updated successfully", id);

        return toResponse(updatedLocation);
    }

    public String delete(Long id) {
        logger.info("Deleting location with ID: {}", id);
        Location location = locationRepository.findById(id).orElseThrow(() ->
                new InformationNotFoundException("Location with the id:" + id + " does not exist, please try again with another id"));
        locationRepository.delete(location);
        logger.info("Location with ID {} deleted successfully", id);
        return "Location with id:"+ id +"has been deleted successfully";
    }

    private LocationResponse toResponse(Location location) {
        return new LocationResponse(
                location.getId(), location.getName(), location.getAddress(),
                location.getCity(), location.getLatitude(), location.getLongitude(),
                location.getActive()
        );
    }


}