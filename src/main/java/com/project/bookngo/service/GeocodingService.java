package com.project.bookngo.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class GeocodingService {
    private static final String GEOCODING_URL =
            "https://maps.googleapis.com/maps/api/geocode/json";
    private final HttpClient httpClient;
    private final JsonMapper objectMapper;
    private final String apiKey;

    public GeocodingService(JsonMapper objectMapper, @Value("${google.maps.api-key:}") String apiKey){
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build();
    }
    /**
     * Resolves a provider-entered address into geographic coordinates
     * using the Google Maps Geocoding API.
     *
     * @param fullAddress the address and city to look up
     * @return an array containing latitude at index 0
     *         and longitude at index 1
     * @throws IllegalArgumentException if the address is empty
     *         or cannot be found
     * @throws IllegalStateException if Google Maps is unavailable,
     *         rejects the request, or returns an invalid response
     */
    public double[] geocodeAddress(String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank()) {
            throw new IllegalArgumentException("A valid address is required.");
        }

        if (apiKey.isBlank()) {
            throw new IllegalStateException("Google Maps API key is not configured.");
        }

        String encodedAddress = URLEncoder.encode(fullAddress, StandardCharsets.UTF_8);

        URI uri = URI.create(
                GEOCODING_URL
                        + "?address=" + encodedAddress
                        + "&key=" + URLEncoder.encode(
                        apiKey, StandardCharsets.UTF_8
                )
        );

        HttpRequest request = HttpRequest.newBuilder().uri(uri).timeout(Duration.ofSeconds(10)).GET().build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Google Maps returned HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());

            String status = root.path("status").asText();

            if ("ZERO_RESULTS".equals(status)) {
                throw new IllegalArgumentException("Address not found. Please verify the location.");
            }

            if (!"OK".equals(status)) {
                throw new IllegalStateException("Google Maps geocoding failed: " + status);
            }

            JsonNode results = root.path("results");

            if (!results.isArray() || results.isEmpty()) {
                throw new IllegalStateException("Google Maps returned no location data.");
            }

            JsonNode coordinates = results.get(0).path("geometry").path("location");

            if (!coordinates.hasNonNull("lat") || !coordinates.hasNonNull("lng")) {
                throw new IllegalStateException("Google Maps returned invalid coordinates.");
            }

            return new double[] {
                    coordinates.path("lat").asDouble(),
                    coordinates.path("lng").asDouble()
            };

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException("Google Maps request was interrupted.", ex);

        } catch (IOException ex) {
            throw new IllegalStateException("Unable to communicate with Google Maps.", ex);
        }
    }
}
