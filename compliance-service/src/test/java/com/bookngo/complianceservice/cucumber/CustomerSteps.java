package com.bookngo.complianceservice.cucumber;

import com.bookngo.complianceservice.client.AuthClient;
import com.bookngo.complianceservice.client.UserInfo;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Steps that set up the customer's account in the mocked auth-service and check status changes.
 * Also removes the scenario's database rows after each scenario.
 */
public class CustomerSteps {

    @Autowired
    private CustomerContext customer;

    @Autowired
    private AuthClient authClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Makes the mocked auth-service return this scenario's customer and remember status changes.
     */
    @Given("a customer with an active account")
    public void aCustomerWithAnActiveAccount() {
        long id = customer.customerId();
        when(authClient.getUser(id))
                .thenAnswer(call -> new UserInfo(id, customer.customerEmail(), "USER", customer.accountStatus()));
        doAnswer(call -> {
            customer.setAccountStatus(call.getArgument(1));
            return null;
        }).when(authClient).setStatus(eq(id), anyString());
    }

    @Then("the customer's account status is unchanged")
    public void theAccountStatusIsUnchanged() {
        verify(authClient, never()).setStatus(eq(customer.customerId()), anyString());
        assertEquals("ACTIVE", customer.accountStatus());
    }

    @Then("the customer's account is deactivated")
    public void theAccountIsDeactivated() {
        verify(authClient).setStatus(customer.customerId(), "DEACTIVATED");
        assertEquals("DEACTIVATED", customer.accountStatus());
    }

    @Then("the customer's account is active again")
    public void theAccountIsActiveAgain() {
        verify(authClient).setStatus(customer.customerId(), "ACTIVE");
        assertEquals("ACTIVE", customer.accountStatus());
    }

    @Then("the customer is blacklisted")
    public void theCustomerIsBlacklisted() {
        verify(authClient).setStatus(customer.customerId(), "BLACKLISTED");
        assertEquals("BLACKLISTED", customer.accountStatus());
    }

    /**
     * Deletes the violations, penalties and counters created for this scenario's customer.
     */
    @After
    public void deleteScenarioData() {
        long id = customer.customerId();
        jdbcTemplate.update("delete from compliance.violations where user_id = ?", id);
        jdbcTemplate.update("delete from compliance.penalties where user_id = ?", id);
        jdbcTemplate.update("delete from compliance.violation_counters where user_id = ?", id);
    }
}
