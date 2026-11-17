package com.bookngo.complianceservice.client;

import com.bookngo.complianceservice.exception.BusinessConflictsException;
import com.bookngo.complianceservice.exception.InformationNotFoundException;
import com.bookngo.complianceservice.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/* Calls internal endpoints of auth-service */
@Component
public class AuthClient {

    private record StatusUpdate(String status) {}

    private final RestClient restClient;

    public AuthClient(@Value("${services.auth.url}") String baseUrl,
                      @Value("${app.internal-api-key:}") String internalKey) {
        this.restClient = InternalRestClients.create(baseUrl, internalKey);
    }

    public com.bookngo.complianceservice.client.UserInfo getUser(Long userId) {
        try {
            UserInfo user = restClient.get().uri("/api/internal/users/{id}", userId).retrieve().body(UserInfo.class);
            if (user == null) {
                throw new ServiceUnavailableException("auth-service returned an empty response.", null);
            }
            return user;
        } catch (HttpClientErrorException.NotFound e) {
            throw new InformationNotFoundException("User with ID: " + userId + " not found.");
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("auth-service is unavailable. Please try again later.", e);
        }
    }

    /** status: ACTIVE, DEACTIVATED or BLACKLISTED. */
    public void setStatus(Long userId, String status) {
        try {
            restClient.put().uri("/api/internal/users/{id}/status", userId)
                    .body(new StatusUpdate(status)).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new InformationNotFoundException("User with ID: " + userId + " not found.");
        } catch (HttpClientErrorException.Conflict e) {
            throw new BusinessConflictsException("auth-service refused the status change for user " + userId + ".");
        } catch (RestClientException e) {
            throw new com.bookngo.complianceservice.exception.ServiceUnavailableException("auth-service is unavailable. Please try again later.", e);
        }
    }
}
