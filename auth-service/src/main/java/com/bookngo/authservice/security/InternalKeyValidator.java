package com.bookngo.authservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/* hecks the shared X-Internal-Key used for service-to-service calls. */
@Component
public class InternalKeyValidator {

    @Value("${app.internal-api-key:}")
    private String internalApiKey;

    public void check(String providedKey) {
        boolean valid = internalApiKey != null && !internalApiKey.isBlank() && providedKey != null
                && MessageDigest.isEqual(internalApiKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            throw new AccessDeniedException("Invalid internal key.");
        }
    }
}
