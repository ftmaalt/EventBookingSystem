package com.project.bookngo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeocodingServiceTest {
    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> response;

    private GeocodingService geocodingService;

    @BeforeEach
    void setUp() {

        geocodingService = new GeocodingService(JsonMapper.builder().build(), "fake-test-api-key", httpClient);
    }

    /**
     * Simulates Google's HTTP response without making
     * an actual external API request.
     *
     * @param statusCode the simulated HTTP response code
     * @param body       the simulated JSON response body
     */
    private void mockGoogleResponse(int statusCode, String body) throws Exception {
        when(response.statusCode()).thenReturn(statusCode);

        if (statusCode == 200) {
            when(response.body()).thenReturn(body);
        }

        when(httpClient.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);
    }

    /**
     * Demonstrates that a successful Google Geocoding
     * response is correctly converted into latitude
     * and longitude values.
     */
    @Test
    @DisplayName("Geocoding: valid address returns coordinates")
    void shouldReturnCoordinatesForValidAddress()
            throws Exception {
        String json = """
                {
                  "status": "OK",
                  "results": [
                    {
                      "geometry": {
                        "location": {
                          "lat": 26.2376177,
                          "lng": 50.5734496
                        }
                      }
                    }
                  ]
                }
                """;

        mockGoogleResponse(200, json);

        double[] coordinates =
                geocodingService.geocodeAddress("Bahrain Financial Harbour, Manama");

        assertEquals(26.2376177, coordinates[0], 0.0000001);
        assertEquals(50.5734496, coordinates[1], 0.0000001);

        verify(httpClient, times(1)).send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }

    /**
     * Demonstrates that BookNGo rejects addresses
     * which Google cannot locate.
     */
    @Test
    @DisplayName("Geocoding: unknown address is rejected")
    void shouldRejectUnknownAddress() throws Exception {

        String json = """
                {
                  "status": "ZERO_RESULTS",
                  "results": []
                }
                """;

        mockGoogleResponse(200, json);

        assertThrows(IllegalArgumentException.class, () -> geocodingService.geocodeAddress("Unknown Test Address"));
    }

    /**
     * Demonstrates that Google API authorization
     * failures are handled without saving coordinates.
     */
    @Test
    @DisplayName("Geocoding: Google rejects API request")
    void shouldHandleGoogleRequestDenied() throws Exception {

        String json = """
                {
                  "status": "REQUEST_DENIED",
                  "results": []
                }
                """;

        mockGoogleResponse(200, json);

        assertThrows(IllegalStateException.class, () -> geocodingService.geocodeAddress("Manama, Bahrain"));
    }

    /**
     * Demonstrates that an unsuccessful HTTP response
     * from Google is handled as a service failure.
     */
    @Test
    @DisplayName("Geocoding: HTTP server error is handled")
    void shouldHandleGoogleServerError() throws Exception {

        mockGoogleResponse(503, "");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> geocodingService.geocodeAddress("Manama, Bahrain"));

        assertTrue(exception.getMessage().contains("503"));
    }

    /**
     * Demonstrates that blank addresses are rejected
     * before an external API request is attempted.
     */
    @Test
    @DisplayName("Geocoding: empty address is rejected")
    void shouldRejectEmptyAddress() {

        assertThrows(
                IllegalArgumentException.class,
                () -> geocodingService.geocodeAddress(" ")
        );

        verifyNoInteractions(httpClient);
    }

    /**
     * Demonstrates that missing API configuration
     * prevents Google requests from being executed.
     */
    @Test
    @DisplayName("Geocoding: missing API key is rejected")
    void shouldRejectMissingApiKey() {

        GeocodingService service =
                new GeocodingService(JsonMapper.builder().build(), "", httpClient);

        assertThrows(IllegalStateException.class, () -> service.geocodeAddress("Manama, Bahrain"));
        verifyNoInteractions(httpClient);
    }
}