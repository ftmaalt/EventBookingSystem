package com.bookngo.complianceservice.cucumber;

import com.bookngo.complianceservice.model.Penalty;
import com.bookngo.complianceservice.model.enums.PenaltyStatus;
import com.bookngo.complianceservice.repository.PenaltiesRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PenaltySteps {

    @Autowired
    private CustomerContext customer;

    @Autowired
    private PenaltiesRepository penaltiesRepository;

    @Then("the customer has a pending penalty of {bigdecimal}")
    public void theCustomerHasAPendingPenaltyOf(BigDecimal amount) {
        Penalty penalty = onlyPenalty();
        assertEquals(PenaltyStatus.PENDING, penalty.getStatus());
        assertEquals(0, amount.compareTo(penalty.getAmount()));
    }

    @Then("the customer has no penalty")
    public void theCustomerHasNoPenalty() {
        assertTrue(penalties().isEmpty());
    }

    @Given("the customer has paid the penalty")
    @When("the customer pays the penalty")
    @When("the customer pays the penalty again")
    public void theCustomerPaysThePenalty() {
        Response response = given()
                .header("Authorization", customer.customerToken())
                .pathParam("id", onlyPenalty().getId())
        .when()
                .put("/api/penalties/{id}/pay");
        customer.setResponse(response);
    }

    @Then("the penalty is marked as paid")
    public void thePenaltyIsMarkedAsPaid() {
        assertEquals(200, customer.response().getStatusCode());
        Penalty penalty = onlyPenalty();
        assertEquals(PenaltyStatus.PAID, penalty.getStatus());
        assertNotNull(penalty.getPaidAt());
    }

    @Then("the payment is rejected because the penalty is already paid")
    public void thePaymentIsRejected() {
        assertEquals(409, customer.response().getStatusCode());
        assertEquals(PenaltyStatus.PAID, onlyPenalty().getStatus());
    }

    private List<Penalty> penalties() {
        return penaltiesRepository.findByUserIdOrderByCreatedAtDesc(customer.customerId());
    }

    private Penalty onlyPenalty() {
        List<Penalty> penalties = penalties();
        assertEquals(1, penalties.size());
        return penalties.get(0);
    }
}
