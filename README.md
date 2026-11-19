# BookNGo — Activities & Experiences Booking Platform

**Docs:**
[Architecture](docs/architecture.md) ·
[Deployment](docs/deployment.md) ·
[Kafka events](docs/kafka-events.md) ·
[Phase 1 – Project 2](docs/phase-1-project2.md) ·
[Phase 2 – Project 3](docs/phase-2-project3.md)
---
**Project:** [Trello user stories](https://trello.com/b/WOpNyReb/project2-user-stories) ·
[Trello Project 4 plan](https://trello.com/b/Pr7szEG4/project-4-plan) ·
[Jenkins setup](jenkins/README.md) ·


---

BookNGo is a full-stack platform where customers discover activities and experiences, book sessions, and join waitlists, and providers publish and manage them. It was built as General Assembly Java Development Bootcamp Projects 2, 3 and 4.

| Phase | Focus | Docs |
|---|---|---|
| 1 — Project 2 | Secure REST API, roles, bookings, provider approval, violations | [phase-1-project2.md](docs/phase-1-project2.md) |
| 2 — Project 3 | Concurrency, waitlist, async events, Google Maps | [phase-2-project3.md](docs/phase-2-project3.md) |
| 3 — Project 4 | Microservices, Docker, Kafka, Jenkins CI/CD, AWS | [architecture.md](docs/architecture.md), [deployment.md](docs/deployment.md), [kafka-events.md](docs/kafka-events.md) |

## User roles

| Role | Capabilities |
|---|---|
| `USER` (customer) | Browse activities, book sessions, cancel, join waitlists, receive notifications |
| `PROVIDER` | Manage activities, locations and sessions |
| `ADMIN` | Review provider applications, manage users, audit logs |

Browsing is public; everything else needs a JWT.

## Architecture at a glance

Five Spring Boot services behind an nginx gateway, one PostgreSQL database with one schema per service, and Kafka for asynchronous events.

```mermaid
flowchart LR
    UI["React frontend"] --> GW["nginx gateway :8080"]
    GW --> AUTH["auth :8081"]
    GW --> CAT["catalog :8082"]
    GW --> BOOK["booking :8083"]
    GW --> NOTIF["notification :8084"]
    GW --> COMP["compliance :8085"]
    BOOK ==> K[["Kafka"]]
    AUTH ==> K
    K ==> NOTIF
    AUTH & CAT & BOOK & NOTIF & COMP --> DB[("PostgreSQL<br/>schema per service")]
```

| Service | Port | Responsibility |
|---|---|---|
| auth-service | 8081 | Registration, login, roles, provider applications |
| catalog-service | 8082 | Categories, locations, activities (cached) |
| booking-service | 8083 | Sessions, bookings, waitlist, concurrency control |
| notification-service | 8084 | Kafka consumer, in-app (SSE) and email notifications |
| compliance-service | 8085 | Violations, strikes, penalties |

Full detail, diagrams and workflows: [docs/architecture.md](docs/architecture.md).

## Technology stack

| Category | Technologies |
|---|---|
| Backend | Java 17, Spring Boot, Spring Data JPA, Hibernate, Maven |
| Security | Spring Security, JWT, BCrypt, login rate limiting |
| Frontend | React 19, Vite |
| Database | PostgreSQL 16, Flyway migrations |
| Messaging | Apache Kafka 3.9 (KRaft), dead-letter topics |
| Concurrency | JPA pessimistic locking, `ThreadPoolTaskExecutor`, transactional events |
| Caching | Spring Cache with Caffeine |
| Real time & email | Server-Sent Events, Spring Mail |
| Maps | Google Maps JavaScript and Geocoding APIs |
| API docs | Springdoc OpenAPI (Swagger UI on each service) |
| Containers | Docker, Docker Compose, nginx gateway |
| CI/CD | Jenkins, Docker Hub (GitHub Actions workflow as an extra) |
| Cloud | AWS EC2, RDS PostgreSQL, CloudWatch Logs |
| Testing | JUnit 5, Mockito, Spring Boot Test, Vitest, React Testing Library |

## Quick start (local)

Prerequisites: Docker with Compose, Node.js 18+ (frontend and seed script).

1. Create a `.env` file in the repo root (never commit it):

   ```dotenv
   DB_PASSWORD=choose-a-password
   JWT_SECRET=a-long-random-string
   INTERNAL_API_KEY=another-random-string
   # optional
   GOOGLE_MAPS_API_KEY=
   MAIL_ENABLED=false
   ```

2. Start the backend:

   ```bash
   docker compose up -d --build
   ```

   The API is available through the gateway at `http://localhost:8080`. Kafka UI runs at `http://localhost:8090`.

3. Start the frontend:

   ```bash
   cd frontend
   echo VITE_API_URL=http://localhost:8080 > .env.local
   npm install
   npm run dev
   ```

4. Optional demo data (admin, providers, customers, activities, sessions, bookings):

   ```bash
   # enable the seeder once: add SEED_ENABLED=true and SEED_PASSWORD=<password> to .env,
   # then: docker compose up -d auth-service
   SEED_PASSWORD=<password> node scripts/seed-demo-data.mjs
   ```

   Set `SEED_ENABLED=false` afterwards. The script is safe to run twice. Accounts and details: [docs/seed-data.md](docs/seed-data.md).

Swagger UI per service: `http://localhost:8081/swagger-ui/index.html` (auth), `8082` (catalog), `8083` (booking), `8084` (notification), `8085` (compliance).

## Database Design & ERD

One PostgreSQL database with one schema per service. Solid lines are foreign keys inside a service. Dashed lines are ID-only references across services (no foreign key).

```mermaid
erDiagram
    USERS ||--o| PROVIDER_PROFILES : has
    USERS ||--o{ PROVIDER_APPLICATIONS : submits
    CATEGORIES ||--o{ ACTIVITIES : groups
    LOCATIONS ||--o{ ACTIVITIES : hosts
    ACTIVITIES ||..o{ SESSIONS : "scheduled as"
    SESSIONS ||--o{ BOOKINGS : receives
    SESSIONS ||--o{ WAITLIST : queues
    USERS ||..o{ BOOKINGS : "makes"
    USERS ||..o{ WAITLIST : "joins"
    BOOKINGS ||..o{ VIOLATIONS : "relates to"
    USERS ||..o{ VIOLATIONS : "commits"
    USERS ||..o{ PENALTIES : "owes"
    USERS ||..o{ NOTIFICATIONS : "receives"

    USERS {
        bigint id PK
        string email UK
        string role
        string status
    }
    PROVIDER_PROFILES {
        bigint provider_id PK
        bigint user_id FK
        string business_name
    }
    PROVIDER_APPLICATIONS {
        bigint application_id PK
        string status
        bigint reviewed_by FK
    }
    CATEGORIES {
        bigint category_id PK
        string category_name UK
    }
    LOCATIONS {
        bigint id PK
        double latitude
        double longitude
        bigint provider_id
    }
    ACTIVITIES {
        bigint id PK
        string title
        numeric price_per_person
        bigint category_id FK
        bigint location_id FK
    }
    SESSIONS {
        bigint id PK
        int capacity
        int spots_left
        string status
        bigint version
        bigint activity_id
    }
    BOOKINGS {
        bigint booking_id PK
        int participants
        string status
        bigint user_id
        bigint session_id FK
    }
    WAITLIST {
        bigint entry_id PK
        string status
        timestamp joined_at
        bigint session_id FK
    }
    VIOLATIONS {
        bigint id PK
        string type
        boolean excused
    }
    PENALTIES {
        bigint id PK
        smallint strike_number
        string status
    }
    NOTIFICATIONS {
        bigint id PK
        string type
        boolean is_read
    }
```

Key rules enforced by the schema: a partial unique index on `bookings(user_id, session_id)` for `CONFIRMED` bookings prevents duplicate bookings, and `sessions.spots_left` is protected by a row lock during booking.
## Tests

```bash
./mvnw test                      # backend (Windows: .\mvnw.cmd test)
cd frontend && npm test          # frontend
```

## Repository layout

```text
auth-service/  catalog-service/  booking-service/
notification-service/  compliance-service/   Spring Boot services
gateway/                                     nginx.conf
frontend/                                    React app
scripts/                                     demo data seeder
docs/                                        architecture, deployment, Kafka, phase docs
jenkins/  Jenkinsfile  .github/workflows/    CI/CD
docker-compose.yml                           local stack
docker-compose.prod.yml  .env.prod.example   production stack (AWS)
Dockerfile                                   shared multi-stage build (--build-arg SERVICE=...)
```

## Security

No secrets are committed. All credentials come from environment variables (`.env` locally, `/opt/bookngo/.env` on the server). See [docs/deployment.md](docs/deployment.md#4-configuration-and-secrets).

## Future improvements

- Scheduled booking reminders published as Kafka events.
- A real payment gateway (penalty payment is simulated).
- Transactional outbox for event publishing, shared cache (Redis), HTTPS and multi-host deployment.
- Rating and reviewing activities and events.
- Deploy the frontend on AWS S3.
