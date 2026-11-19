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
 * One fresh customer per scenario. No proxy (proxyMode = NO): the mocked AuthClient reads and
 * updates this object from the app's HTTP threads, where a scenario-scoped proxy can't be used.
 */
@Component
@ScenarioScope(proxyMode = ScopedProxyMode.NO)
public class CustomerContext {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private final long customerId = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_000_000_000L);
    private final long adminId = customerId + 1;
    private volatile String accountStatus = "ACTIVE";
    // the last HTTP reply, shared so a @When in one step class can be checked by a @Then in another
    private Response response;

    public long customerId() {
        return customerId;
    }

    public String customerEmail() {
        return "customer-" + customerId + "@test.com";
    }

    public String accountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public Response response() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public String adminToken() {
        return token(adminId, "admin-" + adminId + "@test.com", "ADMIN");
    }

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
