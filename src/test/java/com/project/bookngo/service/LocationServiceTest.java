
package com.project.bookngo.service;

import com.project.bookngo.model.Location;
import com.project.bookngo.model.User;
import com.project.bookngo.model.enums.UserRole;
import com.project.bookngo.model.request.LocationRequest;
import com.project.bookngo.model.response.LocationResponse;
import com.project.bookngo.repository.LocationRepository;
import com.project.bookngo.repository.UsersRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private GeocodingService geocodingService;

    @InjectMocks
    private LocationService locationService;

    private User provider;
    private LocationRequest request;

    @BeforeEach
    void setUp() {

        provider = new User();
        provider.setId(7L);
        provider.setEmail("provider@test.com");
        provider.setRole(UserRole.PROVIDER);

        var authentication = new UsernamePasswordAuthenticationToken(provider.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_PROVIDER")));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        request = new LocationRequest();
        request.setName("Bahrain Financial Harbour");
        request.setAddress("Bahrain Financial Harbour");
        request.setCity("Manama, Bahrain");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Demonstrates that creating a provider location
     * persists the latitude, longitude and Google Place ID
     * returned by the GeocodingService.
     *
     * Google is mocked, so no external API request occurs.
     */
    @Test
    @DisplayName("Location: saves Google coordinates and Place ID")
    void shouldSaveGoogleLocationData() {

        when(usersRepository.findUserByEmail(provider.getEmail())).thenReturn(provider);

        when(geocodingService.geocodeAddress("Bahrain Financial Harbour, Manama, Bahrain")).thenReturn(
                new GeocodingResult(
                        26.2376177,
                        50.5734496,
                        "test-google-place-id"));

        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {Location location = invocation.getArgument(0);location.setId(10L);
                    return location;
        });


        LocationResponse response = locationService.create(request);

        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);

        verify(locationRepository).save(captor.capture());

        Location saved = captor.getValue();

        assertAll("Saved Google location data",
                () -> assertEquals(
                        26.2376177, saved.getLatitude()),
                () -> assertEquals(
                        50.5734496, saved.getLongitude()),
                () -> assertEquals(
                        "test-google-place-id", saved.getGooglePlaceId()),
                () -> assertEquals(provider.getId(), saved.getProvider().getId()),
                () -> assertEquals("test-google-place-id", response.getGooglePlaceId())
        );
    }

    /**
     * Demonstrates that updating an existing provider
     * location refreshes its Google coordinates and Place ID.
     *
     * The old geographic information must be replaced
     * with the latest GeocodingResult.
     */
    @Test
    @DisplayName("Location: updating address refreshes Google data")
    void shouldUpdateGoogleLocationData() {

        Location existing = new Location();
        existing.setId(10L);
        existing.setProvider(provider);
        existing.setName("Old Location");
        existing.setAddress("Old Address");
        existing.setCity("Manama");
        existing.setLatitude(26.0);
        existing.setLongitude(50.0);
        existing.setGooglePlaceId("old-place-id");
        existing.setActive(true);

        when(locationRepository.findById(10L)).thenReturn(Optional.of(existing));

        when(usersRepository.findUserByEmail(provider.getEmail())).thenReturn(provider);

        when(geocodingService.geocodeAddress(anyString())).thenReturn(
                        new GeocodingResult(
                                26.2376177,
                                50.5734496,
                                "new-place-id"));

        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LocationResponse result = locationService.update(10L, request);
        assertAll("Updated location",
                () -> assertEquals(
                        "new-place-id",
                        existing.getGooglePlaceId()),
                () -> assertEquals(
                        26.2376177,
                        existing.getLatitude()),
                () -> assertEquals(
                        50.5734496,
                        existing.getLongitude()),
                () -> assertEquals(
                        "new-place-id",
                        result.getGooglePlaceId()));
        verify(locationRepository).save(existing);
    }

    /**
     * Demonstrates that BookNGo does not save a location
     * when the external geocoding operation fails.
     *
     * This prevents incomplete or inaccurate location
     * records from being persisted.
     */
    @Test
    @DisplayName("Location: geocoding failure prevents saving")
    void shouldNotSaveLocationWhenGeocodingFails() {

        when(usersRepository.findUserByEmail(provider.getEmail())).thenReturn(provider);

        when(geocodingService.geocodeAddress(anyString())).thenThrow(
                new IllegalArgumentException("Address not found."));

        assertThrows(IllegalArgumentException.class, () -> locationService.create(request));
        verify(locationRepository, never()).save(any(Location.class));
    }
}
