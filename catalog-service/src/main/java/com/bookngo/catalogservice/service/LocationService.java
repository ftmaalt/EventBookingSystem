package com.bookngo.catalogservice.service;

import com.bookngo.catalogservice.config.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import com.bookngo.catalogservice.dto.request.LocationPinUpdateRequest;
import com.bookngo.catalogservice.dto.request.LocationRequest;
import com.bookngo.catalogservice.dto.response.LocationResponse;
import com.bookngo.catalogservice.exception.BusinessConflictsException;
import com.bookngo.catalogservice.exception.InformationNotFoundException;
import com.bookngo.catalogservice.model.Location;
import com.bookngo.catalogservice.repository.LocationRepository;
import com.bookngo.catalogservice.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class LocationService {


    private final LocationRepository locationRepository;

    private final CurrentUserService currentUserService;

    private final GeocodingService geocodingService;

    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);

    public LocationService(LocationRepository locationRepository, CurrentUserService userService, GeocodingService geocodingService) {
        this.locationRepository = locationRepository;
        this.currentUserService = userService;
        this.geocodingService = geocodingService;
    }
    @Transactional
    @CacheEvict(cacheNames = CacheNames.LOCATIONS, allEntries = true)
    public LocationResponse create(LocationRequest request) {
        AuthenticatedUser user = currentUserService.getCurrentUser();
        GeocodingResult coords = geocodingService.geocodeAddress(request.getAddress() + ", " + request.getCity());

        Location location = new Location();
        location.setName(request.getName());
        location.setAddress(request.getAddress());
        location.setCity(request.getCity());
        location.setProviderId(user.id());
        location.setLatitude(coords.latitude());
        location.setLongitude(coords.longitude());
        location.setGooglePlaceId(coords.placeId());
        location.setActive(true);

        Location saved = locationRepository.save(location);
        logger.info("Location created successfully with ID: {}", saved.getId());
        return toResponse(saved);
    }
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.LOCATION_BY_ID)
    public LocationResponse getById(Long id) {
        logger.info("Fetching location with ID: {}", id);
        logger.debug("Cache miss: {} key={}", CacheNames.LOCATION_BY_ID, id);
        Location spot= locationRepository.findById(id).orElseThrow(()->
                new InformationNotFoundException("Location with the id:"+ id +" does not exist, please try again with another id")
        );
        return toResponse(spot);
    }
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.LOCATIONS)
    public List<LocationResponse> getAll() {
        logger.info("Fetching all locations");
        logger.debug("Cache miss: {}", CacheNames.LOCATIONS);
        return locationRepository.findAll().stream().map(this::toResponse).toList();
    }
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.LOCATION_BY_ID, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.LOCATIONS, allEntries = true)
    })
    public LocationResponse update(Long id, LocationRequest request){
        Location location = find(id);
        AuthenticatedUser user = currentUserService.getCurrentUser();
        assertOwner(location, currentUserService.getCurrentUser());
        assertActive(location);

        if (!Objects.equals(location.getAddress(), request.getAddress())
                || !Objects.equals(location.getCity(), request.getCity())) {
            GeocodingResult coords = geocodingService.geocodeAddress(request.getAddress() + ", " + request.getCity());
            location.setLatitude(coords.latitude());
            location.setLongitude(coords.longitude());
            location.setGooglePlaceId(coords.placeId());
        }
        location.setName(request.getName());
        location.setAddress(request.getAddress());
        location.setCity(request.getCity());
        logger.info("Location with ID {} updated successfully", id);
        return toResponse(locationRepository.save(location));

    }


    /** Saves an owner's manual pin without geocoding or changing the descriptive location. */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.LOCATION_BY_ID, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.LOCATIONS, allEntries = true)
    })
    public LocationResponse updatePin(Long id, LocationPinUpdateRequest request) {
        AuthenticatedUser user = currentUserService.getCurrentUser();
        if (!"PROVIDER".equals(user.role())) {
            throw new AccessDeniedException("Only providers may edit location pins.");
        }
        Location location = find(id);
        assertOwner(location, user);
        assertActive(location);

        Double latitude = request.getLatitude();
        Double longitude = request.getLongitude();
        if (latitude == null || longitude == null || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90; longitude must be between -180 and 180.");
        }
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setGooglePlaceId(null);
        return toResponse(locationRepository.save(location));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.LOCATION_BY_ID, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.LOCATIONS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ACTIVITY_BY_ID, allEntries = true)
    })
    public String delete(Long id) throws AccessDeniedException {
        Location location = find(id);
        assertOwner(location, currentUserService.getCurrentUser());
        assertActive(location);

        location.setActive(false);
        locationRepository.save(location);
        return "Location with ID: " + id + " has been deactivated.";
    }

    private Location find(Long id) {
        return locationRepository.findById(id).orElseThrow(() ->
                new InformationNotFoundException("Location with the id:" + id + " does not exist, please try again with another id"));
    }

    private void assertOwner(Location location, AuthenticatedUser user) {
        if (!location.getProviderId().equals(user.id())) {
            throw new AccessDeniedException("You are not authorized to modify this location.");
        }
    }
    private void assertActive(Location location) {
        if (!Boolean.TRUE.equals(location.getActive())) {
            throw new BusinessConflictsException(
                    "This location is inactive and cannot be modified.");
        }
    }

    private LocationResponse toResponse(Location l) {
        return new LocationResponse(l.getId(), l.getName(), l.getAddress(), l.getCity(),
                l.getLatitude(), l.getLongitude(), l.getGooglePlaceId(), l.getActive(), l.getProviderId());
    }



}
