package com.bookngo.complianceservice.cucumber;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * JUnit 4 entry point for the compliance-service Cucumber scenarios.
 *
 * <ul>
 *   <li>{@code features}: location of the Gherkin {@code .feature} files.</li>
 *   <li>{@code glue}: package containing the step definitions, hooks and Spring configuration.</li>
 *   <li>{@code plugin}: console output plus an HTML report in {@code target/cucumber-report.html}.</li>
 * </ul>
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = "com.bookngo.complianceservice.cucumber",
        plugin = {"pretty", "html:target/cucumber-report.html"}
)
public class RunCucumberTest {
}
