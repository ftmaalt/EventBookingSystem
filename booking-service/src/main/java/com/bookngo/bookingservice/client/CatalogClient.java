package com.bookngo.bookingservice.client;

import com.bookngo.bookingservice.exception.InformationNotFoundException;
import com.bookngo.bookingservice.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/** Synchronous REST client for catalog-service. Only used when a session is created. */
@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(@Value("${services.catalog.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public ActivityInfo getActivity(Long activityId) {
        try {
            ActivityInfo info = restClient.get().uri("/api/activities/{id}", activityId)
                    .retrieve().body(ActivityInfo.class);
            if (info == null) {
                throw new ServiceUnavailableException("catalog-service returned an empty response.", null);
            }
            return info;
        } catch (HttpClientErrorException.NotFound e) {
            throw new InformationNotFoundException("Activity with ID: " + activityId + " not found.");
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("catalog-service is unavailable. Please try again later.", e);
        }
    }
}
