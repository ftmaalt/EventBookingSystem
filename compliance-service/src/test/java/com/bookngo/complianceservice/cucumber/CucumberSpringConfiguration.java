package com.bookngo.complianceservice.cucumber;

import com.bookngo.complianceservice.client.AuthClient;
import com.bookngo.complianceservice.client.BookingClient;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
public class CucumberSpringConfiguration {

    @MockitoBean
    private AuthClient authClient;

    @MockitoBean
    private BookingClient bookingClient;
}
