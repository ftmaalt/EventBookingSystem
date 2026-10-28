
package com.project.bookngo.exception;

/**
 * Represents a failure when communicating with the
 * external Google Geocoding API.
 *
 * this exception indicates a problem with the external API or its response.
 */
public class GeocodingServiceException extends RuntimeException {

    public GeocodingServiceException(String message) {
        super(message);
    }

    public GeocodingServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
