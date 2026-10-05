package com.project.bookngo.service;

import org.springframework.stereotype.Service;

@Service
public class GeocodingService {

    public double[] geocodeAddress(String fullAddress) {
        // TODO: replace with real Google Geocoding API call once core features are done
        // Stub: returns Bahrain's approximate coordinates for any address
        return new double[] { 26.0667, 50.5577 };
    }
}
