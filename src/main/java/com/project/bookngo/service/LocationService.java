package com.project.bookngo.service;

import com.project.bookngo.exception.BusinessConflictsException;
import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.model.Location;
import com.project.bookngo.model.User;
import com.project.bookngo.model.request.LocationRequest;
import com.project.bookngo.model.response.LocationResponse;
import com.project.bookngo.repository.LocationRepository;
import com.project.bookngo.repository.UsersRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;

@Service
public class LocationService {


    private final LocationRepository locationRepository;

    private final UsersRepository usersRepository;

    private final GeocodingService geocodingService;

    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);

    public LocationService(LocationRepository locationRepository, UsersRepository usersRepository, GeocodingService geocodingService) {
        this.locationRepository = locationRepository;
        this.usersRepository = usersRepository;
        this.geocodingService = geocodingService;
    }

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

    public LocationResponse update(Long id, LocationRequest request){
        logger.info("Updating location with ID: {}", id);
        Location location = locationRepository.findById(id).orElseThrow(() ->
                new InformationNotFoundException("Location with the id:" + id + " does not exist, please try again with another id"));
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user= usersRepository.findUserByEmail(email);
        if (!location.getProvider().getId().equals(user.getId())){
            throw new AccessDeniedException("You are not authorized to modify this location");
        }

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


    @Transactional
    public String delete(Long id) throws AccessDeniedException {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Location with ID: " + id + " does not exist."));

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        User user = usersRepository.findUserByEmail(email);

        if (!location.getProvider().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to modify this location.");
        }

        if (Boolean.FALSE.equals(location.getActive())) {
            throw new BusinessConflictsException("Location is already inactive.");
        }
        location.setActive(false);
        locationRepository.save(location);
        logger.info("Location with ID {} deactivated successfully", id);
        return "Location with ID: " + id + " has been deactivated.";
    }


    private LocationResponse toResponse(Location location) {
        return new LocationResponse(
                location.getId(), location.getName(), location.getAddress(),
                location.getCity(), location.getLatitude(), location.getLongitude(),
                location.getActive()
        );
    }


}