package com.bookngo.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoginRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpServletRequestBuilder login(String email) {
        return post("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr("10.250.0.1");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"WrongPassword123!\"}");
    }

    @Test
    void sixthLoginAttemptWithinTheWindowIsRejected() throws Exception {
        String email = "ratelimit-" + UUID.randomUUID() + "@test.com";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(login(email)).andExpect(status().isUnauthorized());
        }
        mockMvc.perform(login(email)).andExpect(status().isTooManyRequests());
    }
}
