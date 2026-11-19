package com.bookngo.complianceservice.cucumber;

import com.bookngo.complianceservice.client.AuthClient;
import com.bookngo.complianceservice.client.BookingClient;
import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Starts the real compliance-service on a random port so REST Assured has something to hit.
 * auth-service and booking-service are other microservices, so they are replaced with mocks.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfiguration {

    @LocalServerPort
    private int port;

    @MockitoBean
    private AuthClient authClient;

    @MockitoBean
    private BookingClient bookingClient;

    @Before
    public void pointRestAssuredAtTheApp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }
}
