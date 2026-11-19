package com.bookngo.complianceservice.cucumber;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Per-scenario state shared between the step definition classes: a randomly generated customer,
 * the customer's account status as seen by the mocked auth-service, and the last HTTP response.
 *
 * <p>The bean is created without a scoped proxy ({@link ScopedProxyMode#NO}) because the mocked
 * {@code AuthClient} reads and updates it from the application's HTTP request threads, where a
 * scenario-scoped proxy cannot be resolved.</p>
 */
@Component
@ScenarioScope(proxyMode = ScopedProxyMode.NO)
public class CustomerContext {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private final long customerId = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_000_000_000L);
    private final long adminId = customerId + 1;
    private volatile String accountStatus = "ACTIVE";
    private Response response;

    /**
     * @return the random user ID of this scenario's customer
     */
    public long customerId() {
        return customerId;
    }

    /**
     * @return the email address derived from the customer ID
     */
    public String customerEmail() {
        return "customer-" + customerId + "@test.com";
    }

    /**
     * @return the account status the mocked auth-service currently reports, initially {@code ACTIVE}
     */
    public String accountStatus() {
        return accountStatus;
    }

    /**
     * Records a status change sent to the mocked auth-service.
     *
     * @param accountStatus the new status, for example {@code DEACTIVATED}
     */
    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    /**
     * @return the response of the last request stored with {@link #setResponse(Response)}
     */
    public Response response() {
        return response;
    }

    /**
     * Stores a response so that a later step, possibly in another class, can assert on it.
     *
     * @param response the REST Assured response
     */
    public void setResponse(Response response) {
        this.response = response;
    }

    /**
     * @return an {@code Authorization} header value with a signed JWT for an admin user
     */
    public String adminToken() {
        return token(adminId, "admin-" + adminId + "@test.com", "ADMIN");
    }

    /**
     * @return an {@code Authorization} header value with a signed JWT for this scenario's customer
     */
    public String customerToken() {
        return token(customerId, customerEmail(), "USER");
    }

    private String token(long userId, String email, String role) {
        return "Bearer " + Jwts.builder()
                .subject(email)
                .claim("uid", userId)
                .claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                .compact();
    }
}
