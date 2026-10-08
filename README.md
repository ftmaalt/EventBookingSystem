# BookNGo — Activities & Experiences Booking Platform

A Spring Boot REST API and React frontend for discovering experiences, managing provider listings, and reserving places in scheduled activity sessions. BookNGo supports customer (`USER`), `PROVIDER`, and `ADMIN` roles, with email verification, JWT authentication, bookings, notifications, and administration workflows.

Built for **Java-FT-01-Bahrain — Project 02: Build a Secure Spring Boot REST API**.

## Purpose & key features

- **Customers:** register, verify their email, sign in, browse and filter activities, manage their profiles and profile images, reserve session capacity, view and cancel bookings.
- **Providers:** apply for provider access, maintain a provider profile, and manage activities, locations, and scheduled sessions subject to authorization.
- **Administrators:** review provider applications, manage roles and account status, process violations and penalties, and inspect audit logs.
- **Platform:** JWT, email/password recovery, status tracking, pagination and sorting, scheduled reminders, SSE notifications, file upload, rate limiting, and Swagger/OpenAPI documentation.

## Technology stack

| Area | Technology |
|---|---|
| Language/runtime | Java 17, Spring Boot 4.1.1, embedded Tomcat |
| API | REST, Spring Web MVC, Bean Validation, DTOs |
| Persistence | PostgreSQL, Spring Data JPA, Hibernate |
| Security | Spring Security, JWT, BCrypt, role checks |
| Documentation | Springdoc OpenAPI / Swagger UI |
| Messaging | Spring Mail, scheduled email reminders, SSE |
| Web application | React, Vite |
| Testing | JUnit/Mockito service tests, Postman |
| Tools | Maven, Git, GitHub, Trello, dbdiagram.io |

## Architecture & implementation approach

The backend follows a layered design: **Controller → Service → Repository → PostgreSQL**. Controllers define the REST contracts and validate input DTOs; services coordinate authorization and business workflows; repositories provide persistence via Spring Data JPA; entities model the database. Separate configuration, security, exception, request/response, and notification packages keep concerns isolated.

Implementation is divided by domain (accounts and security, catalog and providers, sessions and bookings, violations and penalties, messaging and administration). The frontend consumes the REST endpoints. Data is seeded through `DataSeeder.java`, and sensitive responses use DTOs instead of serializing password hashes or JPA entities directly. This description is based on the source structure; individual security and business-rule guarantees must be confirmed by tests.

### Repository structure

```text
frontend/                       React/Vite application
src/main/java/com/project/bookngo/
  config/                       Security, Swagger, seeding, rate-limit setup
  controller/                   REST endpoints
  service/                      Business logic and integrations
  repository/                   Spring Data JPA repositories
  model/                        JPA entities, DTOs, enums
  security/                     JWT processing
  exception/                    Domain exceptions
src/main/resources/             Environment configuration
src/test/java/                  Automated tests
```

## Database & ERD

The persistence model includes 13 mapped entities: `User`, `ProviderProfile`, `ProviderApplication`, `Category`, `Location`, `Activities`, `Sessions`, `Bookings`, `Tokens`, `Notification`, `Violations`, `Penalties`, and `AuditLog`. Foreign keys connect bookings to users/sessions, sessions to activities, activities to categories/providers/locations, and associated records to their owners.

<img width="3372" height="2163" alt="Untitled" src="https://github.com/user-attachments/assets/72df4831-430d-49db-b971-24b623dc3c09" />

## Installation & local setup

### Requirements

- JDK 17; Maven or the bundled Maven Wrapper
- PostgreSQL with a database named `bookngo` (or configure another database name)
- Node.js and npm for the optional frontend
- SMTP credentials for real verification and recovery emails; integration-specific credentials where applicable

### Clone and configure

```bash
git clone https://github.com/ftmaalt/EventBookingSystem.git
cd EventBookingSystem
```

Set the **development profile** and provide database, JWT, and SMTP configuration. Current source uses `src/main/resources/application.properties` plus `application-dev.properties`; create a dedicated `application-test.properties` for tests when needed. Recommended environment-backed configuration examples (update properties to reference these variables if not already configured):

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/bookngo}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}
jwt-secret=${JWT_SECRET}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
```

### Run backend

```bash
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run`. The API runs on **http://localhost:8080** by default. `DataSeeder.java` provides initial records; check its behavior before assuming demo account credentials or seeded bookings.

### Run frontend

```bash
cd frontend
npm install
npm run dev
```

Vite typically serves locally on port 5173; use the URL displayed by Vite. Frontend and backend must both be running for the complete app.

## Swagger/OpenAPI documentation

- **Interactive UI (locally):** http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON (locally):** http://localhost:8080/v3/api-docs
- Swagger's **Authorize** control accepts a login JWT for authenticated requests. Send `Authorization: Bearer <token>` in Postman.
- These localhost URLs work only while a local backend is running; reviewers must start the project or use a deployed instance.
- The current API definition provides paths, DTO schemas, and the JWT security scheme. Endpoint-specific descriptions, error-response codes, and examples should be expanded before final evaluation.

## API endpoint reference

Endpoints below were extracted from the controller mappings in the uploaded source. Access labels describe typical protection,

### Authentication & account recovery

| Method | Path | Access (summary) |
|---|---|---|
| `POST` | `/api/auth/register` | Public |
| `GET` | `/api/auth/verify` | Public |
| `POST` | `/api/auth/login` | Public |
| `POST` | `/api/auth/forgotPassword` | Public |
| `POST` | `/api/auth/resetPassword` | Public |
| `PUT` | `/api/auth/changePassword` | Authenticated; service/method authorization applies |

### User profile

| Method | Path | Access (summary) |
|---|---|---|
| `GET` | `/api/users/me` | Authenticated user |
| `PUT` | `/api/users/me` | Authenticated user |
| `POST` | `/api/users/me/profile-picture` | Authenticated user |

### Provider profile

| Method | Path | Access (summary) |
|---|---|---|
| `GET` | `/api/provider-profile/me` | Authenticated; service/method authorization applies |
| `PUT` | `/api/provider-profile/me` | Authenticated; service/method authorization applies |

### Provider applications

| Method | Path | Access (summary) |
|---|---|---|
| `POST` | `/api/applications` | Authenticated; service/method authorization applies |
| `GET` | `/api/applications/mine` | Authenticated; service/method authorization applies |
| `GET` | `/api/applications/all` | Authenticated; service/method authorization applies |
| `GET` | `/api/applications/{application_id}` | Authenticated; service/method authorization applies |
| `PATCH` | `/api/applications/{application_id}/status?action=approve` | Authenticated; service/method authorization applies |
| `PATCH` | `/api/applications/{application_id}/status?action=reject` | Authenticated; service/method authorization applies |

### Categories

| Method | Path | Access (summary) |
|---|---|---|
| `POST` | `/api/categories` | Authenticated; service/method authorization applies |
| `GET` | `/api/categories/{category_id}` | Public |
| `GET` | `/api/categories` | Public |
| `PUT` | `/api/categories/{category_id}` | Authenticated; service/method authorization applies |
| `DELETE` | `/api/categories/{category_id}` | Authenticated; service/method authorization applies |

### Locations

| Method | Path | Access (summary) |
|---|---|---|
| `POST` | `/api/locations` | Authenticated; service/method authorization applies |
| `GET` | `/api/locations/{id}` | Public |
| `GET` | `/api/locations` | Public |
| `PUT` | `/api/locations/{id}` | Authenticated; service/method authorization applies |
| `DELETE` | `/api/locations/{id}` | Authenticated; service/method authorization applies |

### Activities

| Method | Path | Access (summary) |
|---|---|---|
| `POST` | `/api/activities` | Authenticated; service/method authorization applies |
| `GET` | `/api/activities/{activity_id}` | Public |
| `GET` | `/api/activities` | Public |
| `PUT` | `/api/activities/{activity_id}` | Authenticated; service/method authorization applies |
| `DELETE` | `/api/activities/{activity_id}` | Authenticated; service/method authorization applies |

### Sessions

| Method | Path | Access (summary) |
|---|---|---|
| `POST` | `/api/sessions` | Authenticated; service/method authorization applies |
| `GET` | `/api/sessions/{id}` | Public |
| `GET` | `/api/sessions/activity/{activityId}` | Public |
| `PUT` | `/api/sessions/{id}` | Authenticated; service/method authorization applies |
| `PATCH` | `/api/sessions/{id}/status` | Authenticated; service/method authorization applies |
| `DELETE` | `/api/sessions/{id}` | Authenticated; service/method authorization applies |

### Bookings

| Method | Path | Access (summary) |
|---|---|---|
| `POST` | `/api/bookings` | Authenticated user (ownership rules) |
| `GET` | `/api/bookings/{id}` | Authenticated user (ownership rules) |
| `GET` | `/api/bookings` | Authenticated user (ownership rules) |
| `DELETE` | `/api/bookings/{id}` | Authenticated user (ownership rules) |

### Notifications & SSE

| Method | Path | Access (summary) |
|---|---|---|
| `GET` | `/api/notifications` | Authenticated user |
| `PATCH` | `/api/notifications/read-all` | Authenticated user |
| `PATCH` | `/api/notifications/{id}/read` | Authenticated user |
| `DELETE` | `/api/notifications/{id}` | Authenticated user |
| `DELETE` | `/api/notifications` | Authenticated user |
| `GET` | `/api/notifications/subscribe` | Authenticated user |

### Administration

| Method | Path | Access (summary) |
|---|---|---|
| `PUT` | `/api/admin/{id}/role` | ADMIN |
| `PUT` | `/api/admin/users/{id}/deactivate` | ADMIN |

### Audit logs

| Method | Path | Access (summary) |
|---|---|---|
| `GET` | `/api/admin/audit-logs` | ADMIN |

### Violations

| Method | Path | Access (summary) |
|---|---|---|
| `POST` | `/api/violations` | Authenticated; service/method authorization applies |

### Penalties

| Method | Path | Access (summary) |
|---|---|---|
| `PUT` | `/api/penalties/{id}/pay` | Authenticated; service/method authorization applies |

## Example workflow

1. `POST /api/auth/register` with `fullname`, `email`, `phone` (8 digits), and `password` (minimum 8 characters).
2. Complete email verification using `GET /api/auth/verify?token=...`.
3. `POST /api/auth/login`; store the returned `token` and `role`.
4. `GET /api/activities` to find an activity, then `GET /api/sessions/activity/{activityId}`.
5. `POST /api/bookings` while authenticated:

```json
{"sessionId": 1, "participants": 2, "bookingType": "INDIVIDUAL"}
```

6. `GET /api/bookings?page=0&size=10&sort=createdAt,desc` and optionally `?status=CONFIRMED`.
7. `DELETE /api/bookings/{id}` to request cancellation subject to booking rules.

The sample session ID is illustrative and must be replaced with an existing, available session.

## Booking and security business rules

The codebase implements domain services intended to enforce rules such as session capacity/remaining spots, booking statuses and cancellation, reservation ownership, provider ownership, approval of provider applications, valid account status for login, and no-show/violation consequences. Validate these behaviors with repeatable tests, especially **concurrent reservations** and invalid state transitions.

- Booking statuses: `PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `COMPLETED`.
- Session statuses: `SCHEDULED`, `FULL`, `CANCELLED`, `IN_PROGRESS`, `COMPLETED`.
- Roles: `USER`, `PROVIDER`, `ADMIN`.
- JWT-protected endpoints require a signed nonexpired token, and role-based operations require suitable authorization.
- Soft deletion is represented by account/resource status changes where implemented, rather than physical deletion.
- Validation and exceptions should produce appropriate HTTP errors; the exact returned status codes must be verified with Postman.

## Filtering, pagination and sorting

Examples:

```http
GET /api/activities?categoryId=1&locationId=1&page=0&size=10&sort=pricePerPerson,asc
GET /api/bookings?status=CONFIRMED&page=0&size=10&sort=createdAt,desc
GET /api/admin/audit-logs?page=0&size=10
```

## SSE, emails, upload, and rate limiting

- **SSE:** `GET /api/notifications/subscribe` subscribes an authenticated client to event-stream updates. Confirm an event arrives when a relevant booking/notification action occurs.
- **Email:** registration verification, password recovery, and booking reminders use configured SMTP; actual delivery requires valid credentials.
- **Upload:** `POST /api/users/me/profile-picture` accepts `multipart/form-data` with a `file` field. Current multipart request limit is 5 MB.
- **Rate limiting:** `RateLimitService` and related configuration implement throttling infrastructure for sensitive public endpoints. Verify HTTP 429 responses under repeated requests.
- **Auditing:** `AuditLog` and administrative query support tracking significant actions.

## Testing

Current automated Java tests include:

- `src/test/java/com/project/bookngo/service/AuthServiceTest.java`
- `src/test/java/com/project/bookngo/service/BookingsServiceTest.java`
- `src/test/java/com/project/bookngo/BookngoApplicationTests.java`

Run:

```bash
./mvnw test
```

## Development & planning

- [Project user stories (Trello)](https://trello.com/b/WOpNyReb/project2-user-stories)
- [Project plan (Trello)](https://trello.com/b/gYvDWs6d/project2-planning)
- [Repository and Git branch/commit history](https://github.com/ftmaalt/EventBookingSystem)

Development has used feature-specific branches (including controllers, services, models, registration, bookings, and tests); Git history provides evidence of incremental development. Review Trello for the detailed scope, deliverables, and progress.

## Known issues / unfinished verification

- A `LazyInitializationException` was observed in `ReminderService` when accessing a lazy-loaded session from scheduled work. Verify the transactional/fetch fix in the latest running build.
- OpenAPI paths and schemas are generated, but not all operations document their error codes, examples, or detailed descriptions.
- Recheck role restrictions on category and session modification endpoints and verify resource ownership.
- Validate concurrency protection for the last available session spots, cancellation behavior, and rate limiting using a running PostgreSQL instance.
- Verify the **test Spring profile** and keep production-only credentials outside committed properties files.
- Keep the DBML synchronized with schema changes and exported dbdiagram images.

## Major implementation challenges

- **JPA relationships and scheduled tasks:** lazy entity loading can fail when code accesses relationships after the persistence context closes.
- **Booking integrity:** status transitions and remaining capacity must stay consistent under concurrent requests.
- **Security:** JWT authentication must be combined with role checks and ownership validation, not just route-level authentication.
- **Configuration:** local SMTP, third-party services, and database credentials need environment-specific management.

These are documented technical challenges; completion of every corrective action is not claimed.

## Future improvements

- Activity ratings and user reviews; more advanced search and recommendations.
- Comprehensive integration tests (including concurrent booking tests), expanded OpenAPI operation documentation, and production-safe configuration.
- Deployment pipeline, monitoring, and further provider analytics.

## Credits and references

| Resource | URL | Usage |
|---|---|---|
| Project 02 requirements | https://github.com/Java-FT-01-Bahrain/JDB-Info/blob/main/Projects/Project-02/README.md | Requirements and submission checklist |
| Spring Boot documentation | https://docs.spring.io/spring-boot/ | Application configuration and development reference |
| Spring Security documentation | https://docs.spring.io/spring-security/reference/ | Authentication and access-control reference |
| Spring Data JPA auditing (community discussion) | https://stackoverflow.com/questions/29472931/how-does-createdby-work-in-spring-data-jpa | Background reading on `@CreatedBy` / auditing |
| Rate limiting algorithms (Agam Kakkar) | https://medium.com/@agamkakkar/mastering-rate-limiting-in-java-a-deep-dive-into-4-algorithms-cf618d9fbdd0 | Rate-limiting concepts and implementation reference |
| Swagger/OpenAPI | https://swagger.io/docs/ | Interactive API documentation |
| dbdiagram.io | https://dbdiagram.io/ | Editable ERD generation using DBML |
& AI tools for explaination
## Author

Fatima Abdulla — Software Engineer
