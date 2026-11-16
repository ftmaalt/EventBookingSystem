package com.bookngo.complianceservice.client;

import com.bookngo.complianceservice.exception.InformationNotFoundException;
import com.bookngo.complianceservice.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/* Calls internal endpoints of booking-service */
@Component
public class BookingClient {

    private final RestClient restClient;

    public BookingClient(@Value("${services.booking.url}") String baseUrl,
                         @Value("${app.internal-api-key:}") String internalKey) {
        this.restClient = InternalRestClients.create(baseUrl, internalKey);
    }

    public SessionOwnerInfo getSessionOwner(Long sessionId) {
        try {
            SessionOwnerInfo info = restClient.get().uri("/api/internal/sessions/{id}", sessionId)
                    .retrieve().body(SessionOwnerInfo.class);
            if (info == null) {
                throw new ServiceUnavailableException("booking-service returned an empty response.", null);
            }
            return info;
        } catch (HttpClientErrorException.NotFound e) {
            throw new InformationNotFoundException("Session with ID: " + sessionId + " not found.");
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("booking-service is unavailable. Please try again later.", e);
        }
    }

    public CancelledSessionsInfo cancelUpcomingSessions(Long providerId) {
        try {
            CancelledSessionsInfo info = restClient.post()
                    .uri("/api/internal/providers/{id}/cancel-upcoming-sessions", providerId)
                    .retrieve().body(CancelledSessionsInfo.class);
            return info == null ? new CancelledSessionsInfo(0, 0) : info;
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("booking-service is unavailable. Please try again later.", e);
        }
    }
}
