package com.project.bookngo.service;

import com.project.bookngo.model.Location;
import com.project.bookngo.model.User;
import com.project.bookngo.model.enums.UserRole;
import com.project.bookngo.model.enums.UserStatus;
import com.project.bookngo.model.request.LocationPinUpdateRequest;
import com.project.bookngo.repository.LocationRepository;
import com.project.bookngo.repository.UsersRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Uses real PostgreSQL/JPA transactions in a unique schema, without starting seeders or email/SSE services. */
@SpringJUnitConfig(LocationPinPersistenceTest.PersistenceConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LocationPinPersistenceTest {
    private static final String SCHEMA = "pin_test_" + UUID.randomUUID().toString().replace("-", "");

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = LocationRepository.class)
    @PropertySource("classpath:application-test.properties")
    static class PersistenceConfig {
        @Bean
        DataSource dataSource(Environment environment) {
            return new IsolatedDataSource(environment);
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.project.bookngo.model");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop",
                    "hibernate.default_schema", SCHEMA));
            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean
        GeocodingService geocodingService() {
            return mock(GeocodingService.class);
        }

        @Bean
        LocationService locationService(LocationRepository locations, UsersRepository users, GeocodingService geocoding) {
            return new LocationService(locations, users, geocoding);
        }
    }

    /** Owns only this run's random schema; cleanup never targets an existing application schema. */
    static class IsolatedDataSource extends DriverManagerDataSource implements DisposableBean {
        IsolatedDataSource(Environment environment) {
            super(environment.getRequiredProperty("spring.datasource.url"),
                    environment.getRequiredProperty("spring.datasource.username"),
                    environment.getRequiredProperty("spring.datasource.password"));
            if (!SCHEMA.matches("pin_test_[0-9a-f]{32}")) throw new IllegalStateException("Unsafe test schema");
            new JdbcTemplate(this).execute("CREATE SCHEMA " + SCHEMA);
        }

        @Override
        public void destroy() {
            new JdbcTemplate(this).execute("DROP SCHEMA " + SCHEMA + " CASCADE");
        }
    }

    @Autowired private LocationService service;
    @Autowired private LocationRepository locations;
    @Autowired private UsersRepository users;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private GeocodingService geocoding;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    /** Proves PATCH service changes commit to PostgreSQL and survive a fresh persistence context/read. */
    @Test
    @DisplayName("Pin: committed coordinates survive a fresh PostgreSQL transaction without geocoding")
    void committedPinSurvivesReload() {
        User owner = new User();
        owner.setFullName("Pin Test Provider");
        owner.setEmail("pin-owner@example.test");
        owner.setPasswordHash("test-only");
        owner.setRole(UserRole.PROVIDER);
        owner.setStatus(UserStatus.ACTIVE);
        users.saveAndFlush(owner);
        Location location = new Location();
        location.setProvider(owner);
        location.setName("Center");
        location.setAddress("Road 1");
        location.setCity("Manama");
        location.setActive(true);
        location.setLatitude(26.0);
        location.setLongitude(50.0);
        location.setGooglePlaceId("previous-place");
        locations.saveAndFlush(location);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(owner.getEmail(), null, List.of()));
        LocationPinUpdateRequest pin = new LocationPinUpdateRequest();
        pin.setLatitude(26.2376177);
        pin.setLongitude(50.5734496);

        service.updatePin(location.getId(), pin);

        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            Location reloaded = locations.findById(location.getId()).orElseThrow();
            assertEquals(26.2376177, reloaded.getLatitude());
            assertEquals(50.5734496, reloaded.getLongitude());
            assertNull(reloaded.getGooglePlaceId());
            assertEquals("Center", reloaded.getName());
            assertEquals("Road 1", reloaded.getAddress());
            assertEquals("Manama", reloaded.getCity());
            assertEquals(owner.getId(), reloaded.getProvider().getId());
        });
        verifyNoInteractions(geocoding);
    }
}
