package com.bookngo.complianceservice.cucumber;

import com.bookngo.complianceservice.model.ViolationCounter;
import com.bookngo.complianceservice.repository.ViolationCounterRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ViolationSteps {

    @Autowired
    private CustomerContext customer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ViolationCounterRepository counterRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @When("an admin reports an excused no-show for the customer")
    public void anAdminReportsAnExcusedNoShow() throws Exception {
        reportNoShow(true);
    }

    @When("an admin reports {int} unexcused no-show(s) for the customer")
    public void anAdminReportsUnexcusedNoShows(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            reportNoShow(false);
        }
    }

    @Given("the customer has received their first strike")
    public void theCustomerHasReceivedTheirFirstStrike() throws Exception {
        anAdminReportsUnexcusedNoShows(3);
    }

    @Given("the customer already has {int} strikes")
    public void theCustomerAlreadyHasStrikes(int strikes) {
        ViolationCounter counter = new ViolationCounter(customer.customerId());
        counter.setStrikeCount((short) strikes);
        counterRepository.save(counter);
    }

    @Then("the violation is recorded")
    public void theViolationIsRecorded() {
        Integer violations = jdbcTemplate.queryForObject(
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

    private void reportNoShow(boolean excused) throws Exception {
        String body = """
                {"type": "NO_SHOW", "excused": %s, "userId": %d}""".formatted(excused, customer.customerId());
        mockMvc.perform(post("/api/violations")
                        .header("Authorization", customer.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}
