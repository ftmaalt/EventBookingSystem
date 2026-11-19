package com.bookngo.complianceservice.cucumber;

import com.bookngo.complianceservice.model.ViolationCounter;
import com.bookngo.complianceservice.repository.ViolationCounterRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;

/**
 * Steps for reporting violations through {@code POST /api/violations} and checking strike counters.
 */
public class ViolationSteps {

    @Autowired
    private CustomerContext customer;

    @Autowired
    private ViolationCounterRepository counterRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @When("an admin reports an excused no-show for the customer")
    public void anAdminReportsAnExcusedNoShow() {
        reportNoShow(true);
    }

    @When("an admin reports {int} unexcused no-show(s) for the customer")
    public void anAdminReportsUnexcusedNoShows(int count) {
        for (int i = 0; i < count; i++) {
            reportNoShow(false);
        }
    }

    @Given("the customer has received their first strike")
    public void theCustomerHasReceivedTheirFirstStrike() {
        anAdminReportsUnexcusedNoShows(3);
    }

    /**
     * Stores a strike counter directly in the database, skipping the violations that would cause it.
     *
     * @param strikes number of strikes the customer starts with
     */
    @Given("the customer already has {int} strikes")
    public void theCustomerAlreadyHasStrikes(int strikes) {
        ViolationCounter counter = new ViolationCounter(customer.customerId());
        counter.setStrikeCount((short) strikes);
        counterRepository.save(counter);
    }

    @Then("the violation is recorded")
    public void theViolationIsRecorded() {
        int violations = jdbcTemplate.queryForObject(
                "select count(*) from compliance.violations where user_id = ?", Integer.class, customer.customerId());
        assertEquals(1, violations);
    }

    @Then("the customer has {int} strike(s) and {int} violations towards the next strike")
    public void theCustomerHasStrikes(int strikes, int violations) {
        ViolationCounter counter = counterRepository.findById(customer.customerId())
                .orElse(new ViolationCounter(customer.customerId()));
        assertEquals(strikes, counter.getStrikeCount());
        assertEquals(violations, counter.getConsecutiveViolations());
    }

    /**
     * Reports a no-show for the customer as an admin and expects {@code 201 Created}.
     *
     * @param excused whether the no-show is excused
     */
    private void reportNoShow(boolean excused) {
        String body = """
                {"type": "NO_SHOW", "excused": %s, "userId": %d}""".formatted(excused, customer.customerId());
        given()
                .header("Authorization", customer.adminToken())
                .contentType(ContentType.JSON)
                .body(body)
        .when()
                .post("/api/violations")
        .then()
                .statusCode(201);
    }
}
