package com.bookngo.complianceservice.cucumber;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * The class JUnit launches. It hands control to Cucumber and tells it:
 * features - where the stories live,
 * glue     - where the Java lives (step definitions + Spring setup),
 * plugin   - how the results are printed.
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = "com.bookngo.complianceservice.cucumber",
        plugin = {"pretty", "html:target/cucumber-report.html"}
)
public class RunCucumberTest {
}
