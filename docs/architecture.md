# BookNGo – Software Architecture

BookNGo is a booking platform for activities and experiences in Bahrain. Customers browse activities, book sessions and join waitlists. Providers publish activities and sessions. Admins approve providers.

This document describes the system as it is deployed for Project 4: five Spring Boot microservices behind an nginx gateway, one PostgreSQL database with one schema per service, and Kafka for asynchronous events.

Related: [deployment.md](deployment.md) · [kafka-events.md](kafka-events.md)

## 1. Context and goals

The original application was a single Spring Boot monolith. For Project 4 it was split by business capability so each part can be built, tested, deployed and scaled on its own.

| Goal | How it is met |
|---|---|
| Independent deployability | One Docker image per service, tagged `1.0.<build>` by Jenkins |
| Data ownership | One Postgres schema per service, no cross-schema foreign keys |
| Loose coupling for side effects | Kafka events; notifications never block a booking |
| Safe concurrency | Pessimistic locking on sessions in booking-service |
| Read performance | Caffeine cache-aside in catalog-service |
| Single entry point | nginx gateway on port 8080 |

## 2. From monolith to microservices

```mermaid
flowchart LR
    subgraph Before["Before: monolith"]
        M["Spring Boot app<br/>users, catalog, bookings,<br/>notifications, violations"]
        MDB[("One schema")]
        M --> MDB
    end
    subgraph After["After: 5 services"]
        A["auth-service"]
        C["catalog-service"]
        B["booking-service"]
        N["notification-service"]
        V["compliance-service"]
    end
    Before -. split by capability .-> After
```

## 3. System architecture

```mermaid
flowchart TB
    U["Browser<br/>React frontend"] -->|HTTP :8080| GW["nginx gateway"]

    GW -->|/api/auth, /api/users, /api/admin,<br/>/api/applications, /api/provider-profile, /uploads| AUTH["auth-service :8081"]
    GW -->|/api/activities, /api/categories,<br/>/api/locations| CAT["catalog-service :8082"]
    GW -->|/api/sessions, /api/bookings,<br/>/api/waitlist| BOOK["booking-service :8083"]
    GW -->|/api/notifications<br/>SSE| NOTIF["notification-service :8084"]
    GW -->|/api/violations, /api/penalties| COMP["compliance-service :8085"]

    BOOK -->|REST, X-Internal-Key| CAT
    COMP -->|REST, X-Internal-Key| AUTH
    COMP -->|REST, X-Internal-Key| BOOK

    BOOK ==>|booking-events| K[["Kafka"]]
    AUTH ==>|account-events| K
    K ==>|consume| NOTIF

    AUTH --> S1[("schema auth")]
    CAT --> S2[("schema catalog")]
    BOOK --> S3[("schema booking")]
    NOTIF --> S4[("schema notification")]
    COMP --> S5[("schema compliance")]

    NOTIF -->|SMTP| MAIL["Email provider"]
    CAT -->|Geocoding| GM["Google Maps API"]
```

Key points:

- **Gateway** routes by path prefix only. `/api/internal/**` is deliberately not routed, so internal endpoints are unreachable from outside.
- **Synchronous calls** (REST) are used where the caller needs an answer: booking asks catalog for activity data; compliance asks auth and booking.
- **Asynchronous calls** (Kafka) are used for side effects: emails and in-app notifications.
- **Authentication**: auth-service issues a JWT. Every service validates it with the shared `JWT_SECRET`.
- **Service-to-service security**: internal endpoints require the `X-Internal-Key` header (`INTERNAL_API_KEY`).

## 4. Services

| Service | Port | Responsibility | Schema | Tables |
|---|---|---|---|---|
| auth-service | 8081 | Registration, login (JWT), roles, provider applications, profiles, audit log | `auth` | users, tokens, audit_logs, provider_applications, provider_profiles |
| catalog-service | 8082 | Categories, locations (geocoded), activities; cached reads | `catalog` | categories, locations, activities |
| booking-service | 8083 | Sessions, bookings, waitlist, seat allocation, concurrency control | `booking` | sessions, bookings, waitlist |
| notification-service | 8084 | Kafka consumer, in-app notifications (SSE), email | `notification` | notifications, processed_events |
| compliance-service | 8085 | Violations, strikes, penalties | `compliance` | violations, penalties, violation_counters |

### Internal calls

| Caller | Callee | Endpoint | Purpose |
|---|---|---|---|
| booking-service | catalog-service | `GET /api/activities/{id}` | Read activity data when creating sessions |
| compliance-service | auth-service | `GET /api/internal/users/{id}` | Look up the offender |
| compliance-service | auth-service | `PUT /api/internal/users/{id}/status` | Deactivate or blacklist after a strike |
| compliance-service | booking-service | `GET /api/internal/sessions/{id}` | Check the reporter owns the session |
| compliance-service | booking-service | `POST /api/internal/providers/{id}/cancel-upcoming-sessions` | After a provider strike (best effort) |

## 5. Data architecture

One database `bookngo`, one schema per service. Flyway migrations live in each service and create their own schema. A service never reads another service's tables.

Columns that point to another service (for example `bookings.user_id`) are plain `BIGINT` values with no foreign key. `bookings.user_email` is a snapshot so that events carry everything a consumer needs.

```mermaid
erDiagram
    USERS ||--o{ TOKENS : has
    USERS ||--o| PROVIDER_PROFILES : "has (providers)"
    USERS ||--o{ PROVIDER_APPLICATIONS : "submits / reviews"
    USERS {
        bigint id PK
        varchar full_name
        varchar email UK
        varchar password_hash
        varchar role
        varchar status
    }
    TOKENS {
        bigint tokens_id PK
        bigint user_id FK
    }
    PROVIDER_PROFILES {
        bigint provider_id PK
        bigint user_id FK "unique"
        varchar business_name
    }
    PROVIDER_APPLICATIONS {
        bigint application_id PK
        varchar business_name
        varchar status
        bigint reviewed_by FK
        bigint created_user_id FK
    }
```

```mermaid
erDiagram
    CATEGORIES ||--o{ ACTIVITIES : groups
    LOCATIONS ||--o{ ACTIVITIES : hosts
    CATEGORIES {
        bigint category_id PK
        varchar category_name UK
        boolean active
    }
    LOCATIONS {
        bigint id PK
        varchar name
        varchar city
        double latitude
        double longitude
        bigint provider_id "auth user"
    }
    ACTIVITIES {
        bigint id PK
        varchar title
        numeric price_per_person
        int duration_minutes
        varchar status
        bigint category_id FK
        bigint location_id FK
        bigint provider_id "auth user"
    }
```

```mermaid
erDiagram
    SESSIONS ||--o{ BOOKINGS : has
    SESSIONS ||--o{ WAITLIST : has
    SESSIONS {
        bigint id PK
        timestamp start_time
        timestamp end_time
        int capacity
        int spots_left
        varchar status
        bigint version "optimistic"
        bigint activity_id "catalog"
        varchar activity_title "snapshot"
        numeric price_per_person "snapshot"
    }
    BOOKINGS {
        bigint booking_id PK
        int participants
        varchar booking_type
        numeric total_price
        varchar status
        bigint user_id "auth user"
        varchar user_email "snapshot"
        bigint session_id FK
    }
    WAITLIST {
        bigint entry_id PK
        varchar status
        timestamp joined_at
        bigint user_id "auth user"
        bigint session_id FK
    }
```

```mermaid
erDiagram
    VIOLATIONS {
        bigint id PK
        varchar type
        boolean excused
        bigint booking_id "booking"
        bigint user_id "offender"
        bigint reported_by
    }
    PENALTIES {
        bigint id PK
        smallint strike_number
        numeric amount
        varchar status
        bigint user_id
    }
    VIOLATION_COUNTERS {
        bigint user_id PK
        smallint consecutive_violations
        smallint strike_count
    }
    NOTIFICATIONS {
        bigint id PK
        bigint user_id
        varchar type
        boolean is_read
    }
    PROCESSED_EVENTS {
        varchar event_id PK
        varchar topic
        timestamp processed_at
    }
```

Database-level guarantees in booking-service:

- `ux_bookings_active`: unique `(user_id, session_id)` where status is `CONFIRMED` – a user cannot hold two active bookings for one session.
- `ux_waitlist_waiting`: unique `(user_id, session_id)` where status is `WAITING`.
- `sessions.version`: optimistic version column on top of the pessimistic lock.

## 6. Key workflows

### 6.1 Booking a session and notifying the customer

```mermaid
sequenceDiagram
    actor C as Customer
    participant GW as Gateway
    participant B as booking-service
    participant DB as Postgres (booking)
    participant K as Kafka
    participant N as notification-service
    participant UI as Browser (SSE)

    C->>GW: POST /api/bookings
    GW->>B: forward (JWT)
    B->>DB: BEGIN, lock session row (pessimistic)
    B->>DB: check spots_left, insert booking, spots_left -= n
    B->>DB: COMMIT
    B-->>C: 201 Created
    Note over B,K: AFTER_COMMIT, the HTTP call does not wait
    B-)K: BOOKING_CONFIRMED (key = bookingId)
    K-)N: consume
    N->>N: skip if eventId already processed
    N->>N: save notification + processed marker (one transaction)
    N-)UI: push over SSE
    N-)C: email (if MAIL_ENABLED)
```

### 6.2 Cancellation and waitlist allocation

```mermaid
sequenceDiagram
    actor C1 as Customer A
    participant B as booking-service
    participant K as Kafka
    participant N as notification-service
    actor C3 as Customer C (waiting)

    C1->>B: cancel booking
    B->>B: lock session, spots_left += n
    B->>B: first WAITING entry that fits becomes a booking
    B-)K: BOOKING_CANCELLED
    B-)K: WAITLIST_ALLOCATED
    K-)N: consume both
    N-)C1: cancellation notice
    N-)C3: "a spot opened, you are booked"
```

### 6.3 Violation, strike and penalty

```mermaid
sequenceDiagram
    actor P as Provider
    participant V as compliance-service
    participant A as auth-service
    participant B as booking-service

    P->>V: POST /api/violations (NO_SHOW)
    V->>B: GET /api/internal/sessions/{id}
    B-->>V: session owner
    V->>A: GET /api/internal/users/{id}
    V->>V: record violation, update counters
    alt strike earned (strike 1 or 2)
        V->>A: PUT status = DEACTIVATED
        V->>V: create PENDING penalty (fee 10.000)
    else strike 3
        V->>A: PUT status = BLACKLISTED
    end
    Note over V: Customer pays: PUT /api/penalties/{id}/pay<br/>reactivates a DEACTIVATED user
```

Rules: strikes 1 and 2 need three unexcused violations in a row; after two strikes a single violation is a strike; strike 3 blacklists with no penalty. Excused violations are recorded but do not count. Three provider cancellations in a row give a provider strike and cancel upcoming sessions.

## 7. State models

```mermaid
stateDiagram-v2
    direction LR
    state "Booking" as BS {
        [*] --> CONFIRMED
        CONFIRMED --> CANCELLED
        CONFIRMED --> COMPLETED
    }
```

```mermaid
stateDiagram-v2
    direction LR
    [*] --> SCHEDULED
    SCHEDULED --> FULL: last spot taken
    FULL --> SCHEDULED: spot freed
    SCHEDULED --> IN_PROGRESS
    FULL --> IN_PROGRESS
    IN_PROGRESS --> COMPLETED
    SCHEDULED --> CANCELLED
    FULL --> CANCELLED
```

```mermaid
stateDiagram-v2
    direction LR
    [*] --> ACTIVE
    [*] --> PENDING_VERIFICATION: registers
    PENDING_VERIFICATION --> ACTIVE: e-mail verified
    ACTIVE --> DEACTIVATED: strike 1 or 2
    DEACTIVATED --> ACTIVE: penalty paid
    ACTIVE --> BLACKLISTED: strike 3
```

Waitlist entries: `WAITING → BOOKED | CANCELLED | EXPIRED`. Penalties: `PENDING → PAID`. Provider applications: `PENDING → APPROVED | REJECTED`.

## 8. Cross-cutting concerns

| Concern | Decision |
|---|---|
| Concurrency | Pessimistic lock on the session row while seats change; unique indexes as a second line of defence |
| Caching | Caffeine cache-aside in catalog-service, TTL and size from env (`CACHE_TTL_SECONDS`, `CACHE_MAX_SIZE`); evicted after writes. Limitation: per-instance, so replicas can serve stale data until TTL expires; Redis would remove this |
| Messaging | Producers publish after commit; consumer is idempotent; failures go to dead-letter topics (see [kafka-events.md](kafka-events.md)) |
| Security | BCrypt passwords, JWT, role checks, internal key, login rate limit (5 per minute per IP), secrets only in environment variables |
| Observability | Spring Actuator health on every service; container logs shipped to CloudWatch (`awslogs` driver) |
| Schema evolution | Flyway migrations per service |
| Packaging | Multi-stage Dockerfile, runs as non-root user, JRE-only runtime image |

## 9. Technology stack

Java 17, Spring Boot, Spring Data JPA, Spring Security (JWT), Spring Kafka, Spring Cache with Caffeine, Flyway, PostgreSQL 16.4, Apache Kafka 3.9.1 (KRaft), nginx 1.27, React (Vite), Docker and Docker Compose, Jenkins, AWS (EC2, RDS, CloudWatch), Docker Hub.

## 10. Known limitations

- Kafka runs as a single broker (replication factor 1), adequate for this project but not fault tolerant.
- The cache is per-instance (see above).
- Penalty payment is simulated; there is no payment gateway.
- Provider-session cancellation after a strike is best effort and is not retried.
