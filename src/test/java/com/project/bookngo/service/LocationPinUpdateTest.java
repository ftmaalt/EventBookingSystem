package com.project.bookngo.service;

import com.project.bookngo.controller.LocationController;
import com.project.bookngo.exception.GlobalExceptionHandler;
import com.project.bookngo.model.Location;
import com.project.bookngo.model.User;
import com.project.bookngo.model.enums.UserRole;
import com.project.bookngo.model.request.LocationPinUpdateRequest;
import com.project.bookngo.repository.LocationRepository;
import com.project.bookngo.repository.UsersRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises pin ownership, HTTP validation and saved entity values without calling Google. */
class LocationPinUpdateTest {
    private LocationRepository locations;
    private UsersRepository users;
    private GeocodingService geocoding;
    private LocationService service;
    private MockMvc mvc;
    private User provider;
    private Location location;

    @BeforeEach
    void setUp() {
        locations = mock(LocationRepository.class);
        users = mock(UsersRepository.class);
        geocoding = mock(GeocodingService.class);
        service = new LocationService(locations, users, geocoding);
        provider = new User();
        provider.setId(7L);
        provider.setEmail("owner@example.test");
        provider.setRole(UserRole.PROVIDER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(provider.getEmail(), null, List.of()));
        when(users.findUserByEmail(provider.getEmail())).thenReturn(provider);
        location = new Location();
        location.setId(10L);
        location.setProvider(provider);
        location.setName("Activity center");
        location.setAddress("Original address");
        location.setCity("Manama");
        location.setActive(true);
        location.setLatitude(26.0);
        location.setLongitude(50.0);
        location.setGooglePlaceId("old-place-id");
        when(locations.findById(10L)).thenReturn(Optional.of(location));
        when(locations.save(any(Location.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LocationController controller = new LocationController();
        ReflectionTestUtils.setField(controller, "locationService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private LocationPinUpdateRequest pin(double latitude, double longitude) {
        LocationPinUpdateRequest request = new LocationPinUpdateRequest();
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        return request;
    }

    /** Demonstrates that an owner's PATCH saves only coordinates and clears the unverified Place ID. */
    @Test
    @DisplayName("Pin: owner saves coordinates while descriptive fields and ownership remain unchanged")
    void ownerUpdatesPin() throws Exception {
        mvc.perform(patch("/api/locations/10/pin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":26.2376177,\"longitude\":50.5734496}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(26.2376177))
                .andExpect(jsonPath("$.longitude").value(50.5734496))
                .andExpect(jsonPath("$.googlePlaceId").isEmpty())
                .andExpect(jsonPath("$.providerId").value(7));
        verify(locations).save(location);
        assertEquals("Activity center", location.getName());
        assertEquals("Original address", location.getAddress());
        assertEquals("Manama", location.getCity());
        assertSame(provider, location.getProvider());
        assertTrue(location.getActive());
        verifyNoInteractions(geocoding);
    }

    /** Demonstrates that a later repository-backed read returns the saved pin, not a fresh geocode. */
    @Test
    @DisplayName("Pin: subsequent location read returns the newly saved coordinates")
    void savedPinIsReturnedOnRead() {
        service.updatePin(10L, pin(26.3, 50.6));
        var reloaded = service.getById(10L);
        assertEquals(26.3, reloaded.getLatitude());
        assertEquals(50.6, reloaded.getLongitude());
        assertNull(reloaded.getGooglePlaceId());
        verify(locations).save(location);
        verify(locations, times(2)).findById(10L);
        verifyNoInteractions(geocoding);
    }

    /** Demonstrates that a provider cannot modify another provider's pin or trigger geocoding. */
    @Test
    @DisplayName("Pin: another provider receives HTTP 403 without changing the location")
    void rejectsAnotherProvider() throws Exception {
        User owner = new User();
        owner.setId(99L);
        location.setProvider(owner);
        mvc.perform(patch("/api/locations/10/pin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":26.3,\"longitude\":50.6}"))
                .andExpect(status().isForbidden());
        assertEquals(26.0, location.getLatitude());
        verify(locations, never()).save(any());
        verifyNoInteractions(geocoding);
    }

    /** Demonstrates that USER and ADMIN roles do not acquire provider rights, even with a matching ID. */
    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN"})
    @DisplayName("Pin: non-provider roles receive HTTP 403 even if their ID matches the owner")
    void rejectsNonProvider(String role) throws Exception {
        provider.setRole(UserRole.valueOf(role));
        mvc.perform(patch("/api/locations/10/pin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":26.3,\"longitude\":50.6}"))
                .andExpect(status().isForbidden());
        verify(locations, never()).save(any());
        verifyNoInteractions(geocoding);
    }

    /** Demonstrates required fields, ranges and numeric JSON errors become HTTP 400 before saving. */
    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"latitude\":0}", "{\"longitude\":0}",
            "{\"latitude\":null,\"longitude\":0}",
            "{\"latitude\":90.1,\"longitude\":0}", "{\"latitude\":-90.1,\"longitude\":0}",
            "{\"latitude\":0,\"longitude\":180.1}", "{\"latitude\":0,\"longitude\":-180.1}",
            "{\"latitude\":\"invalid\",\"longitude\":0}", "not-json"
    })
    @DisplayName("Pin: missing, out-of-range or malformed coordinates receive HTTP 400")
    void rejectsInvalidCoordinates(String body) throws Exception {
        mvc.perform(patch("/api/locations/10/pin").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        verify(locations, never()).save(any());
        verifyNoInteractions(geocoding);
    }

    /** Demonstrates inclusive boundaries and zero are valid manual coordinates. */
    @Test
    @DisplayName("Pin: accepts latitude/longitude boundaries and zero")
    void acceptsBoundaries() {
        assertEquals(90.0, service.updatePin(10L, pin(90, 180)).getLatitude());
        assertEquals(-180.0, service.updatePin(10L, pin(-90, -180)).getLongitude());
        assertEquals(0.0, service.updatePin(10L, pin(0, 0)).getLongitude());
        verifyNoInteractions(geocoding);
    }

    /** Demonstrates that non-finite values cannot bypass validation via direct service calls. */
    @Test
    @DisplayName("Pin: rejects non-finite values before changing the saved entity")
    void rejectsNonFiniteCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> service.updatePin(10L, pin(Double.NaN, 0)));
        assertThrows(IllegalArgumentException.class, () -> service.updatePin(10L, pin(0, Double.POSITIVE_INFINITY)));
        verify(locations, never()).save(any());
        verifyNoInteractions(geocoding);
    }

    /** Demonstrates that an absent location produces HTTP 404 rather than creating a replacement. */
    @Test
    @DisplayName("Pin: unknown location receives HTTP 404 and no new location is saved")
    void rejectsMissingLocation() throws Exception {
        when(locations.findById(10L)).thenReturn(Optional.empty());
        mvc.perform(patch("/api/locations/10/pin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":26.3,\"longitude\":50.6}"))
                .andExpect(status().isNotFound());
        verify(locations, never()).save(any());
        verifyNoInteractions(geocoding);
    }
}
