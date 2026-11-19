# Phase 1 — Project 2: Secure Spring Boot REST API

[← Back to README](../README.md)

> **Note:** written during the monolith phase. In Project 4 the application was split into five microservices, so class and package names here refer to the original single application. See [architecture.md](architecture.md) for the current structure.

## 1. Project 2 Overview

Project 2 focused on developing a secure and functional REST API using Spring Boot.

The main objective was to establish the platform's core architecture, database entities, authentication system, role-based authorization, and business workflows.

A React frontend was also developed to provide a user interface for the backend functionality.

## 2. Authentication & User Management

BookNGo implements JWT-based authentication using Spring Security.

### Implemented Features

- User registration and login.
- Password hashing using BCrypt.
- JWT generation and validation.
- Email verification following registration.
- Password change and recovery workflows.
- User profile viewing and editing.
- Profile picture uploads.
- User role and account status management.
- Role-based and method-level access restrictions.

### Security Approach

The application uses stateless authentication.

For protected endpoints, the frontend provides the JWT in the HTTP request header:

```http
Authorization: Bearer <JWT_TOKEN>
```

Spring Security processes the token and determines whether the authenticated user has permission to access the requested operation.

Public activity discovery remains available without authentication, while actions such as booking, waitlist management, and provider administration require authentication.

## 3. Activity & Category Management

Providers can manage activities available on the platform.

### Business Rules

- Only authorized providers can manage their activities.
- Activities belong to a specific provider.
- Activities are assigned to categories.
- Activities are associated with physical locations.
- Activity records can be updated or deactivated according to application rules.

Customers can browse activity listings and view activity details through the React frontend.

## 4. Session Management

An activity can contain multiple scheduled sessions.

Each session stores:

- Start and end times.
- Total capacity.
- Remaining available spots.
- Session status.
- Associated activity.
- 
### Business Rules

- End time must be later than start time.
- Providers can only manage sessions belonging to their activities.
- Session capacity cannot be modified when existing bookings make the change invalid.
- Sessions that have started or finished cannot be edited or cancelled contrary to business rules.
- Cancelled sessions cannot be edited.
- Session modifications are restricted by the configured 24-hour scheduling rule.

Project 3 later strengthened these operations using transactions, ownership checks, and pessimistic locking.

## 5. Booking Management

Customers can reserve places in scheduled activity sessions.

### Booking Information

Each booking stores:

- Customer.
- Session.
- Number of participants.
- Booking type.
- Subtotal and total price.
- Booking status.
- Creation and modification timestamps.

### Booking Rules

- A customer cannot create duplicate confirmed bookings for the same session.
- A booking cannot exceed the number of available seats.
- Sessions that have already started cannot be booked.
- Cancelled or completed sessions are unavailable for booking.
- The booking subtotal is calculated from the activity price and participant count.
- Successful bookings reduce the session's available capacity.
- Booking cancellations follow session timing and status restrictions.

Booking operations were later extended in Project 3 to protect shared capacity from concurrent requests.

## 6. Provider Registration & Approval

BookNGo supports an application workflow for customers interested in becoming activity providers.

### Workflow

1. A user submits a provider application.
2. The application is stored with a pending status.
3. Administrators can review submitted applications.
4. An administrator approves or rejects the application.
5. Approved providers can manage provider-specific resources according to their assigned permissions.

Provider applications contain contact information, business details, a description, and proposed activities.

A separate provider profile stores business-specific information.

## 7. Administration & Audit Logging

Administrators have access to platform management functionality.

### Administrative Capabilities

- Review provider applications.
- Approve or reject applications.
- Manage user roles.
- Deactivate user accounts.
- Review audit logs.
- Support violation and penalty management.

### Audit Logs

The application stores administrative and relevant business actions using audit records containing:

- Action performed.
- User responsible.
- Target entity.
- Target identifier.
- Additional details.
- Timestamp.

Audit logging helps track important application operations.

## 8. Violations, Strikes & Penalties

BookNGo includes a penalty management system for handling customer violations.

The system supports:

- Recording booking- or session-related violations.
- Maintaining user strike counts.
- Tracking consecutive violations.
- Creating penalties when applicable.
- Recording penalty status (pending or paid).
- Settling a penalty through a simulated payment (there is no payment gateway): the user or an admin marks it paid, which reactivates a deactivated account.
- Allowing authorized users to review and manage violations.

This functionality supports the platform's business policies and administrative oversight.

## 9. Notifications & Email Services

The original application includes notification and email capabilities.

### Implemented Features

- Email verification.
- Password recovery emails.
- Booking-related email notifications.
- Stored in-app notifications.
- Server-Sent Events for real-time notification updates.

Project 3 later improved the notification architecture by introducing asynchronous processing and transaction-aware event listeners.

## 10. Additional Backend Features

Other implemented functionality includes:

- DTO-based request and response handling.
- Input validation using Jakarta Bean Validation.
- Centralized exception handling.
- Pagination and sorting for supported endpoints.
- API rate limiting.
- File upload support for profile pictures.
- Default activity categories seeded by a Flyway migration.
- Swagger/OpenAPI documentation.

## 11. React Frontend

Although the primary focus of Project 2 was the backend REST API, BookNGo also includes a React frontend.

The application provides interfaces for:

- Registration and login.
- Public activity discovery.
- Activity details and available sessions.
- Booking creation and management.
- User profile management.
- Provider applications.
- Provider activity and session management.
- Administrative operations.
- In-app notifications.

The frontend communicates with Spring Boot using Axios and stores the authentication state required for protected operations.

---

