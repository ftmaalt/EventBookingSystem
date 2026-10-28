package com.project.bookngo.service;

import com.project.bookngo.exception.BusinessConflictsException;
import com.project.bookngo.exception.InformationNotFoundException;
import com.project.bookngo.model.Location;
import com.project.bookngo.model.User;
import com.project.bookngo.model.request.LocationRequest;
import com.project.bookngo.model.request.LocationPinUpdateRequest;
import com.project.bookngo.model.enums.UserRole;
import com.project.bookngo.model.response.LocationResponse;
import com.project.bookngo.repository.LocationRepository;
import com.project.bookngo.repository.UsersRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.Objects;

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
        GeocodingResult coords = geocodingService.geocodeAddress(address);
        Location location= new Location();
        location.setName(request.getName());
        location.setAddress(request.getAddress());
        location.setCity(request.getCity());
        location.setProvider(user);
        location.setLatitude(coords.latitude());
        location.setLongitude(coords.longitude());
        location.setGooglePlaceId(coords.placeId());
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

        // Descriptive name edits must not overwrite a provider's authoritative pin.
        if (!Objects.equals(location.getAddress(), request.getAddress()) ||
                !Objects.equals(location.getCity(), request.getCity())) {
            GeocodingResult coords = geocodingService.geocodeAddress(request.getAddress() + ", " + request.getCity());
            location.setLatitude(coords.latitude());
            location.setLongitude(coords.longitude());
            location.setGooglePlaceId(coords.placeId());
        }
        location.setName(request.getName());
        location.setAddress(request.getAddress());
        location.setCity(request.getCity());

        Location updatedLocation = locationRepository.save(location);
        logger.info("Location with ID {} updated successfully", id);

        return toResponse(updatedLocation);
    }


    /** Saves an owner's manual pin without geocoding or changing the descriptive location. */
    @Transactional
    public LocationResponse updatePin(Long id, LocationPinUpdateRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = usersRepository.findUserByEmail(email);
        // Keep the service safe even when called outside the secured controller.
        if (user == null || user.getRole() != UserRole.PROVIDER) {
            throw new AccessDeniedException("Only providers may edit location pins.");
        }
        Location location = locationRepository.findById(id).orElseThrow(() ->
                new InformationNotFoundException("Location with ID: " + id + " does not exist."));
        if (!location.getProvider().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to modify this location.");
        }
        Double latitude = request.getLatitude();
        Double longitude = request.getLongitude();
        if (latitude == null || longitude == null || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90; longitude must be between -180 and 180.");
        }
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        // An entrance selected by a person is not a verified match for the former Google place.
        location.setGooglePlaceId(null);
        return toResponse(locationRepository.save(location));
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
                location.getGooglePlaceId(), location.getActive(), location.getProvider().getId()
        );
    }


}
