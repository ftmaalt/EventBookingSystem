# Event Booking System
A secure Spring Boot REST API where customers discover and book activities (kayaking, pottery, escape rooms, etc.) and providers manage their own listings. Admins oversee providers, bookings, and account standing.

Built for Project 2 of the JDB-Info Java-FT-01-Bahrain bootcamp.

## Features

**Customers**
- Register, log in, and manage their profile
- Browse and search activities by category and location
- Book activities, including group bookings (discount for groups over 15)
- Cancel bookings

**Providers**
- Request a provider account (approved or rejected by an admin)
- Manage their own activities, categories, and locations
- Change their own password

**Admins**
- Approve or reject provider requests and set provider profiles
- Excuse no-shows, and review strikes and blocked accounts

**Strike system**
- Customers: 3 consecutive no-shows lead to suspension, a strike, and a fee to reactivate. After the 3rd strike, only 1 more miss is allowed before the account is blocked.
- Providers: 3 unexcused late cancellations or reschedules within 90 days lead to deactivation (upcoming bookings cancelled and refunded, rating set to 0). A 3rd strike means a permanent block.

## Tech Stack

- Java 17, Spring Boot 4.1.1
- Spring Security with JWT authentication
- Spring Data JPA, PostgreSQL
- Spring Boot Actuator (health check)
- Maven, Postman

## Getting Started

### Prerequisites
- JDK 17
- Maven
- PostgreSQL running locally

### Setup
```bash
   git clone https://github.com/ftmaalt/EventBookingSystem.git
   cd EventBookingSystem
```

Configure `src/main/resources/application.properties` (or set environment variables):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/[db_name]
spring.datasource.username=[username]
spring.datasource.password=[password]
jwt.secret=[your-secret]
[third.party.api.key]=[your-key]
```

### Run
```bash
mvn spring-boot:run
```
The API runs at `http://localhost:8080`.

## Health Check
```
GET /actuator/health
```

## Authentication

Protected endpoints require a JWT in the header:
```
Authorization: Bearer <token>
```

| Role | Access |
|------|--------|
| CUSTOMER | Browse, book, cancel own bookings |
| PROVIDER | Manage own activities, categories, locations |
| ADMIN | Provider approvals, strikes, all resources |

## API Endpoints

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | Public | Register a customer |
| POST | `/api/auth/login` | Public | Log in, returns JWT |
| GET | `/api/activities` | Public | List/search activities |
| POST | `/api/bookings` | CUSTOMER | Create a booking |
| POST | `/api/provider-requests` | Public | Request a provider account |
| PUT | `/api/admin/provider-requests/{id}` | ADMIN | Approve or reject |
to be updated...



## Database

<img width="3367" height="1978" alt="ActivityFinder BookingSyste" src="https://github.com/user-attachments/assets/c6a931aa-b129-4472-93a1-12e5eceb0dc9" />


## Project Structure
```
src/main/java/bookngo
├── controller
├── service
├── repository
├── model
├── dto
├── security
└── exception
```
## Trello Boards
[User Stories](https://trello.com/b/WOpNyReb/project2-user-stories) <br>
[Project Planning](https://trello.com/b/gYvDWs6d/project2-planning) <br>

## Future Improvements
- Activity reviews and ratings

## Author
Fatima Abdulla
