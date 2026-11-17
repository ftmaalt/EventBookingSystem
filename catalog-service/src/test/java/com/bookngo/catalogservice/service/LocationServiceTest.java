package com.bookngo.catalogservice.service;

import com.bookngo.catalogservice.dto.request.LocationRequest;
import com.bookngo.catalogservice.dto.response.LocationResponse;
import com.bookngo.catalogservice.model.Location;
import com.bookngo.catalogservice.repository.LocationRepository;
import com.bookngo.catalogservice.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private GeocodingService geocodingService;

    @InjectMocks
    private LocationService locationService;

    private AuthenticatedUser provider;
    private LocationRequest request;

    @BeforeEach
    void setUp() {
        provider = new AuthenticatedUser(7L, "provider@test.com", "PROVIDER");

        request = new LocationRequest();
        request.setName("Bahrain Financial Harbour");
        request.setAddress("Bahrain Financial Harbour");
        request.setCity("Manama, Bahrain");
    }

    private Location existingLocation(Long providerId) {
        Location existing = new Location();
        existing.setId(10L);
        existing.setProviderId(providerId);
        existing.setName("Old Location");
        existing.setAddress("Old Address");
        existing.setCity("Manama");
        existing.setLatitude(26.0);
        existing.setLongitude(50.0);
        existing.setGooglePlaceId("old-place-id");
        existing.setActive(true);
        return existing;
    }

    @Test
    @DisplayName("Location: saves Google coordinates and Place ID")
    void shouldSaveGoogleLocationData() {
        when(currentUserService.getCurrentUser()).thenReturn(provider);
        when(geocodingService.geocodeAddress("Bahrain Financial Harbour, Manama, Bahrain"))
                .thenReturn(new GeocodingResult(26.2376177, 50.5734496, "test-google-place-id"));
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location location = invocation.getArgument(0);
            location.setId(10L);
            return location;
        });

        LocationResponse response = locationService.create(request);

        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        verify(locationRepository).save(captor.capture());
        Location saved = captor.getValue();

        assertAll("Saved Google location data",
                () -> assertEquals(26.2376177, saved.getLatitude()),
                () -> assertEquals(50.5734496, saved.getLongitude()),
                () -> assertEquals("test-google-place-id", saved.getGooglePlaceId()),
                () -> assertEquals(provider.id(), saved.getProviderId()),
                () -> assertTrue(saved.getActive()),
                () -> assertEquals(provider.id(), response.getProviderId()),
                () -> assertEquals("test-google-place-id", response.getGooglePlaceId()));
    }

    @Test
    @DisplayName("Location: updating address refreshes Google data")
    void shouldUpdateGoogleLocationData() {
        Location existing = existingLocation(provider.id());
        when(locationRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(currentUserService.getCurrentUser()).thenReturn(provider);
        when(geocodingService.geocodeAddress(anyString()))
                .thenReturn(new GeocodingResult(26.2376177, 50.5734496, "new-place-id"));
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocationResponse result = locationService.update(10L, request);

        assertAll("Updated location",
                () -> assertEquals(request.getName(), existing.getName()),
                () -> assertEquals(request.getAddress(), existing.getAddress()),
                () -> assertEquals(request.getCity(), existing.getCity()),
                () -> assertEquals(provider.id(), result.getProviderId()),
                () -> assertEquals("new-place-id", existing.getGooglePlaceId()),
                () -> assertEquals(26.2376177, existing.getLatitude()),
                () -> assertEquals(50.5734496, existing.getLongitude()),
                () -> assertEquals("new-place-id", result.getGooglePlaceId()));
        verify(locationRepository).save(existing);
    }

    @Test
    @DisplayName("Location: another provider cannot update or geocode a location")
    void shouldRejectUpdateByAnotherProvider() {
        Location existing = existingLocation(99L);
        existing.setName("Original name");
        when(locationRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(currentUserService.getCurrentUser()).thenReturn(provider);

        assertThrows(AccessDeniedException.class, () -> locationService.update(10L, request));

        assertEquals("Original name", existing.getName());
        verifyNoInteractions(geocodingService);
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    @DisplayName("Location: name-only edit preserves the authoritative manual pin")
    void shouldUpdateNameOnly() {
        Location existing = existingLocation(provider.id());
        existing.setName("Old name");
        existing.setAddress(request.getAddress());
        existing.setCity(request.getCity());
        existing.setLatitude(26.2);
        existing.setLongitude(50.5);
        existing.setGooglePlaceId(null);
        when(locationRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(currentUserService.getCurrentUser()).thenReturn(provider);
        when(locationRepository.save(existing)).thenReturn(existing);

        LocationResponse result = locationService.update(10L, request);

        assertEquals(request.getName(), result.getName());
        assertEquals(request.getAddress(), result.getAddress());
        assertEquals(request.getCity(), result.getCity());
        assertEquals(26.2, result.getLatitude());
        assertEquals(50.5, result.getLongitude());
        assertNull(result.getGooglePlaceId());
        verifyNoInteractions(geocodingService);
    }

    @Test
    @DisplayName("Location: geocoding failure prevents saving")
    void shouldNotSaveLocationWhenGeocodingFails() {
        when(currentUserService.getCurrentUser()).thenReturn(provider);
        when(geocodingService.geocodeAddress(anyString())).thenThrow(new IllegalArgumentException("Address not found."));

        assertThrows(IllegalArgumentException.class, () -> locationService.create(request));

        verify(locationRepository, never()).save(any(Location.class));
    }
}
