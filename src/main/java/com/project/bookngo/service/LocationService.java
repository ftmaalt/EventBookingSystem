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
        System.out.println("SERVICE: Calling create ===>");
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
        return toResponse(savedLocation);
    }

    public LocationResponse getById(Long id) {
        System.out.println("SERVICE: Calling getById ===>");
        Location spot= locationRepository.findById(id).orElseThrow(()->
                new InformationNotFoundException("Location with the id:"+ id +" does not exist, please try again with another id")
        );
        return toResponse(spot);
    }

    public List<LocationResponse> getAll() {
        System.out.println("SERVICE: Calling getAll ===>");
        return locationRepository.findAll().stream().map(this::toResponse).toList();
    }

    public LocationResponse update(Long id, LocationRequest request) {
        System.out.println("SERVICE: Calling update ===>");
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

        return toResponse(updatedLocation);
    }

    public void delete(Long id) {
        System.out.println("SERVICE: Calling update ===>");
        Location location = locationRepository.findById(id).orElseThrow(() ->
                new InformationNotFoundException("Location with the id:" + id + " does not exist, please try again with another id"));
        locationRepository.delete(location);
    }

    private LocationResponse toResponse(Location location) {
        return new LocationResponse(
                location.getId(), location.getName(), location.getAddress(),
                location.getCity(), location.getLatitude(), location.getLongitude(),
                location.getActive()
        );
    }


}