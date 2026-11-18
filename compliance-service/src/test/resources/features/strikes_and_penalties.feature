Feature: Strikes and penalties
  Customers who break the booking rules receive strikes.
  A first or second strike deactivates the account until a penalty is paid.
  A third strike blacklists the customer permanently.

  Background:
    Given a customer with an active account

  Scenario: An excused violation does not count towards a strike
    When an admin reports an excused no-show for the customer
    Then the violation is recorded
    And the customer has 0 strikes and 0 violations towards the next strike
    And the customer's account status is unchanged

  Scenario: Three unexcused no-shows in a row give the first strike
    When an admin reports 3 unexcused no-shows for the customer
    Then the customer has 1 strike and 0 violations towards the next strike
    And the customer's account is deactivated
    And the customer has a pending penalty of 10.000

  Scenario: Paying the penalty reactivates the account
    Given the customer has received their first strike
    When the customer pays the penalty
    Then the penalty is marked as paid
    And the customer's account is active again

  Scenario: Paying the same penalty twice is rejected
    Given the customer has received their first strike
    And the customer has paid the penalty
    When the customer pays the penalty again
    Then the payment is rejected because the penalty is already paid

  Scenario: A third strike blacklists the customer and creates no penalty
    Given the customer already has 2 strikes
    When an admin reports 1 unexcused no-show for the customer
    Then the customer has 3 strikes and 0 violations towards the next strike
    And the customer is blacklisted
    And the customer has no penalty
