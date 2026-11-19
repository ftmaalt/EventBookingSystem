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
 * Spring configuration shared by all Cucumber scenarios.
 *
 * <p>Starts the full compliance-service on a random port so the step definitions can call it over
 * HTTP with REST Assured. The clients for auth-service and booking-service are replaced with
 * Mockito mocks because those services are not running during the tests.</p>
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

    /**
     * Points REST Assured at the application started for this test run. Runs before every scenario.
     */
    @Before
    public void pointRestAssuredAtTheApp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }
}
