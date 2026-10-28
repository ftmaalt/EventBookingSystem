package com.project.bookngo.service;
/**
 * Represents a resolved geographic location returned
 * by the Google Geocoding API.
 *
 * @param latitude geographic latitude
 * @param longitude geographic longitude
 * @param placeId Google's unique identifier for the location
 */
public record GeocodingResult(
        double latitude,
        double longitude,
        String placeId
) {}
