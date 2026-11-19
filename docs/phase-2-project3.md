# Phase 2 — Project 3: Concurrency & Multithreading

[← Back to README](../README.md)

> **Note:** written during the monolith phase. In Project 4 the application was split into five microservices, so class and package names here refer to the original single application. See [architecture.md](architecture.md) for the current structure.

## 1. Project 3 Overview

Project 3 extends the existing BookNGo application to address concurrency, multithreading, asynchronous execution, and external API integration.
The main challenge was to ensure that multiple users could interact with the booking system simultaneously without causing incorrect reservations or database inconsistencies.

### Main Additions

1. Thread-safe booking operations.
2. Prevention of booking race conditions.
3. Automatic FIFO waitlist allocation.
4. Safe handling of simultaneous booking cancellations.
5. Thread-pool-based asynchronous notification processing.
6. Transaction-safe email and SSE event handling.
7. Google Maps and Geocoding API integration.
8. Provider-controlled draggable location markers.
9. Automated concurrency and integration testing.

## 2. Concurrency Problem — Race Conditions

### Identified Problem

A race condition can occur when two customers attempt to book the last available seat in the same session.

Without appropriate synchronization, both requests could read the same capacity before either transaction updates it.

### Example

**Initial condition:**

```text
Session capacity:      1
Available spots:       1
Confirmed bookings:    0
```

Two customers send booking requests simultaneously.

**Potential behavior without concurrency control:**

```text
Customer A reads: 1 available seat
Customer B reads: 1 available seat

Customer A attempts to create a booking
Customer B attempts to create a booking

Both requests may succeed if the shared state
is not protected correctly.
```

### Potential Consequences

- Session overbooking.
- Incorrect remaining capacity.
- Duplicate allocation of a limited resource.
- Inconsistent booking records.
- Unreliable booking confirmations.

## 3. Solution — Pessimistic Database Locking

BookNGo prevents concurrent capacity updates using **PostgreSQL row-level locking through Spring Data JPA**.

### Technologies Used

- `@Transactional`
- `@Lock(LockModeType.PESSIMISTIC_WRITE)`
- PostgreSQL transactions
- Spring Data JPA
- Hibernate

The session repository provides a locking method used by booking-related operations.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Sessions s WHERE s.id = :id")
Optional<Sessions> findByIdForUpdate(@Param("id") Long id);
```

### How It Works

1. Customer A begins a booking transaction.
2. Customer A acquires a pessimistic write lock on the session row.
3. Customer B attempts to acquire the same lock.
4. Customer B waits while Customer A's transaction holds the lock.
5. Customer A validates capacity, creates the booking, and updates the session.
6. Customer A's transaction completes and releases the lock.
7. Customer B obtains the lock and evaluates the updated availability.
8. If no seats remain, Customer B's booking is rejected.

### Expected Outcome

| Request | Result |
|---|---|
| Customer A | `201 Created` |
| Customer B | `409 Conflict` |

The successful request reserves the available seat. The conflicting request cannot create an additional booking beyond the session capacity.

### Why Database Locking?

Database-level locking was selected because booking capacity is shared across application requests and must remain consistent even when multiple threads access the same session.

Unlike synchronizing only Java methods, a PostgreSQL transaction lock protects the shared database row across separate application requests and transactions.

## 4. Thread-Safe Booking Operations

The concurrency improvements were integrated into the existing booking services.

### Protected Operations

- Creating bookings.
- Cancelling bookings.
- Restoring released capacity.
- Allocating available seats to waiting customers.
- Updating and cancelling sessions.

### Transactional Consistency

Booking creation, capacity changes, and related database updates occur within transactions.

If a transaction fails, its uncommitted database changes are rolled back.

This prevents a failed booking operation from leaving partial changes to session availability.

### Duplicate Cancellation Protection

A second race condition occurs when two requests attempt to cancel the same booking simultaneously.

Without proper protection, the system could incorrectly restore the reserved seats more than once.

BookNGo handles this by locking the related session, refreshing the booking state, and validating that the booking has not already been cancelled.

**Expected result:** Only one cancellation changes the booking and restores its capacity.

## 5. Automatic Waitlist System

A significant Project 3 addition is the automatic waitlist.

Customers can join a waitlist when a session is fully booked and eligible for waiting-list registration.

### New Entity — `WaitlistEntry`

The waitlist is persisted in the `waitlist` table.

| Field | Purpose |
|---|---|
| `entry_id` | Unique waitlist entry identifier |
| `user_id` | Customer waiting for a place |
| `session_id` | Requested session |
| `status` | Current waitlist status |
| `joined_at` | Time the customer joined |

### Waitlist Statuses

- `WAITING`
- `BOOKED`
- `CANCELLED`
- `EXPIRED`

### Business Rules

- Customers cannot join the same session waitlist multiple times while already waiting.
- Customers with a confirmed booking for the session cannot create an additional waiting entry.
- Waitlist entries are processed using **FIFO** ordering.
- Booking allocation prioritizes eligible waiting customers.
- Seat allocation respects the session's remaining capacity.
- Waitlist operations use the session's database lock to protect shared state.

## 6. Automatic Seat Allocation

When a confirmed booking is cancelled, BookNGo can automatically allocate the newly available capacity to the next eligible customer in the waitlist.

### Allocation Workflow

```text
Customer cancels booking
          |
          v
Lock session inside transaction
          |
          v
Mark booking as CANCELLED
          |
          v
Restore released capacity
          |
          v
Check waiting customers in FIFO order
          |
          v
Select next eligible customer
          |
          v
Create confirmed booking
          |
          v
Update waitlist status to BOOKED
          |
          v
Update session availability
          |
          v
Commit transaction
          |
          v
Process notification asynchronously
```

### Example

A session contains one confirmed booking, with two customers waiting:

```text
1. Sara — WAITING
2. Ali  — WAITING
```

When the original customer cancels:

```text
Sara — BOOKED
Ali  — WAITING
```

Sara receives the available seat because she joined first.

If additional seats become available through separate cancellations, the allocation process continues according to eligibility and FIFO ordering.

### Concurrency Protection

Automatic allocation is coordinated with the booking cancellation transaction.

This ensures that simultaneous cancellations do not allocate the same available seat to more than one customer.

## 7. Multithreading & Thread Pooling

Project 3 also introduces asynchronous processing using a dedicated Spring-managed thread pool.

### Configuration

The application defines a `ThreadPoolTaskExecutor` named `notificationExecutor`.

```java
@Bean(name = "notificationExecutor")
public ThreadPoolTaskExecutor notificationExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("bookngo-notify-");

    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(20);

    return executor;
}
```

The configuration also enables asynchronous execution using `@EnableAsync`.

### Benefits

- Allows notification tasks to run on worker threads.
- Reduces the amount of notification work performed directly in the HTTP request flow.
- Reuses a controlled number of threads.
- Provides a queue for pending notification tasks.
- Supports asynchronous email and SSE processing.

The application uses Spring's `ThreadPoolTaskExecutor` for background notifications, while concurrency tests use Java's `ExecutorService` to simulate multiple users.

## 8. Event-Driven Booking Notifications

BookNGo uses Spring application events to separate booking operations from notification processing.

### Main Components

| Component | Responsibility |
|---|---|
| `BookingNotificationEvent` | Represents a booking-related notification event |
| `BookingNotificationType` | Defines confirmation, cancellation, and waitlist event types |
| `BookingNotificationListener` | Processes booking-related emails asynchronously |
| `NotificationCreatedEvent` | Represents a newly persisted in-app notification |
| `NotificationSseListener` | Sends SSE updates asynchronously |
| `NotificationAsyncConfig` | Configures the dedicated notification thread pool |

### Booking Event Types

- `BOOKING_CONFIRMED`
- `BOOKING_CANCELLED`
- `WAITLIST_ALLOCATED`

### Event Processing

Booking and waitlist services publish application events after performing their database operations.

The listeners use:

```java
@Async("notificationExecutor")
@TransactionalEventListener(
    phase = TransactionPhase.AFTER_COMMIT
)
```

### Why `AFTER_COMMIT`?

A notification should not be sent to a customer if the related database transaction fails.

For example, sending a booking confirmation before the transaction commits could result in a customer receiving a confirmation for a booking that was eventually rolled back.

Using `AFTER_COMMIT` ensures that the event listener is triggered only after a successful transaction commit.

### Expected Behavior

| Transaction Result | Notification Behavior |
|---|---|
| Booking transaction commits | Confirmation processing is triggered |
| Booking transaction rolls back | Confirmation processing is not triggered |
| Cancellation commits | Cancellation email processing is triggered |
| Waitlist allocation commits | Waitlist confirmation processing is triggered |

Notification delivery failures are logged by the asynchronous listeners without reversing an already committed booking transaction.

## 9. Transaction-Safe Real-Time Notifications

BookNGo uses Server-Sent Events (SSE) to communicate notification updates to connected clients.

Project 3 extends the original implementation by making SSE dispatch asynchronous and transaction-aware.

### Workflow

1. An in-app notification is created within a database transaction.
2. A `NotificationCreatedEvent` is published.
3. The transaction commits successfully.
4. `NotificationSseListener` processes the event asynchronously.
5. The SSE service sends the notification to connected clients.

### Benefits

- Prevents notifications from being broadcast for rolled-back transactions.
- Separates database persistence from network delivery.
- Allows notification updates without refreshing the frontend.
- Avoids performing SSE broadcasting directly inside the original business transaction.

## 10. Google Maps Integration

Project 3 integrates **Google Maps Platform** to improve the accuracy and usability of physical activity locations.

### Technologies

- Google Geocoding API.
- Google Maps JavaScript API.
- `@vis.gl/react-google-maps`.
- Java `HttpClient`.
- Spring Boot environment-based configuration.

### Backend Geocoding

When a provider creates a location or changes its address, the backend sends the address to Google's Geocoding API.

The response provides geographic information, including:

- Latitude.
- Longitude.
- Google Place ID, when available.

This information is stored with the corresponding location.

### Updated Location Model

Project 3 extends location information with:

```text
Location
|-- id
|-- name
|-- address
|-- city
|-- latitude
|-- longitude
|-- googlePlaceId
|-- active
|-- provider
```

### Google API Error Handling

The backend handles cases such as:

- Invalid or unresolved addresses.
- Google API request failures.
- Unexpected API responses.
- Network communication errors.
- Missing API configuration.

External API failures are handled through the application's centralized exception-handling mechanism.

## 11. Interactive Map Pin Editing

Providers can manually adjust the position of their activity center using an interactive Google Map.

This feature addresses cases where address-based geocoding places the marker at an approximate location rather than the exact activity center.

### Features

- View a saved location on Google Maps.
- Open an editable map.
- Drag a marker to the desired location.
- Zoom in to select a precise position.
- Switch between roadmap and satellite views.
- Enter coordinates manually when needed.
- Preview changes before saving.
- Save or cancel the edited position.

### Backend Endpoint

```http
PATCH /api/locations/{id}/pin
```

Example request:

```json
{
  "latitude": 26.2376177,
  "longitude": 50.5734496
}
```

### Business Rules

- Only authorized providers can update their own locations.
- The selected coordinates are saved in PostgreSQL.
- Manual pin updates do not re-run forward geocoding.
- Existing activity relationships are preserved.
- A previous Google Place ID is cleared when it no longer represents a verified match for the manually selected coordinates.

The updated coordinates are used when customers view the activity location and open directions.

### API Key Security

Separate API keys are used for frontend map rendering and backend geocoding.

- The backend key is configured outside the committed source code.
- The browser key is restricted by allowed websites and permitted APIs.
- Frontend configuration uses `VITE_GOOGLE_MAPS_API_KEY`.
- Backend configuration uses `GOOGLE_MAPS_API_KEY`.

Before production deployment, Google Maps data-storage terms, API restrictions, and usage quotas should be reviewed.

## 12. Testing Strategy

Project 3 includes automated tests covering concurrency, transaction consistency, asynchronous processing, and location management.

### Testing Technologies

- JUnit 5.
- Mockito.
- Spring Boot Test.
- PostgreSQL.
- `ExecutorService`.
- `CountDownLatch`.
- HTTP integration tests.
- Vitest and React Testing Library.

### Unit Tests

Unit tests verify isolated service behavior using mocks where appropriate.

Examples include:

- Booking validations.
- Waitlist eligibility and allocation behavior.
- Google Geocoding API response handling.
- Location creation and updates.
- Manual pin validation and authorization.
- Authentication-related service behavior.

### Integration Tests

Integration tests exercise multiple application components together, including Spring transactions and PostgreSQL.

**Main test classes:**

- `BookingConcurrencyIntegrationTest.java`
- `NotificationAfterCommitIntegrationTest.java`
- `LocationPinPersistenceTest.java`

### Concurrency Test Scenarios

| Test Scenario | Expected Result |
|---|---|
| Two customers book the last seat simultaneously | Exactly one booking succeeds |
| Two HTTP booking requests compete for one seat | One HTTP 201 and one HTTP 409 |
| Two cancellations release seats simultaneously | Capacity and waitlist allocations remain consistent |
| A booking cancellation releases one seat | The first eligible waitlisted customer receives it |
| Two requests cancel the same booking | Capacity is restored only once |
| Booking transaction rolls back | No confirmation email is triggered |
| Booking transaction commits | Confirmation email processing is triggered |
| Notification transaction rolls back | No SSE notification is broadcast |
| Notification transaction commits | SSE broadcasting is triggered |

### Concurrency Test Execution

Concurrency tests use Java's `ExecutorService` to execute multiple tasks and `CountDownLatch` to coordinate their start.

Example:

```java
ExecutorService executor =
        Executors.newFixedThreadPool(2);

CountDownLatch ready =
        new CountDownLatch(2);

CountDownLatch start =
        new CountDownLatch(1);
```

The workers signal when they are ready, then wait for the shared start signal before attempting the concurrent operation.

This approach creates a controlled contention scenario without relying on arbitrary `Thread.sleep()` delays.

### Test Results

The final automated test suite was executed successfully during development.

The main concurrency tests verify that the system prevents overbooking, preserves session capacity, respects FIFO waitlist ordering, and avoids duplicate cancellation effects.

## 13. Project 3 Summary

Project 3 extends BookNGo beyond a traditional booking REST API by introducing concurrency protection and asynchronous processing into real business workflows.

### Key Outcomes

- Prevented concurrent requests from overbooking activity sessions.
- Protected session capacity using PostgreSQL pessimistic locking.
- Implemented transaction-safe booking cancellations.
- Introduced an automatic FIFO waitlist.
- Prevented released capacity from being allocated more than once.
- Used a dedicated thread pool for asynchronous notification processing.
- Used transaction-aware listeners to prevent notifications for rolled-back operations.
- Improved real-time updates using asynchronous SSE delivery.
- Integrated Google Maps and real address geocoding.
- Added precise, provider-controlled draggable location markers.
- Verified important scenarios through automated unit and integration tests.

These additions improve the reliability, consistency, responsiveness, and functionality of BookNGo while preserving the application developed in Project 2.

---
