# Ticket Tracker

A modern, enterprise-grade ticket tracking and project management application built with **Spring Boot 4** and **PostgreSQL**. This application demonstrates advanced microservices patterns including OAuth2 authentication, event-driven architecture with RabbitMQ, transactional outbox pattern, and scheduled job processing using Quartz.

## 🎯 Overview

Ticket Tracker is a comprehensive solution for managing tickets and projects with robust authentication, real-time event processing, and automated email notifications. The application is designed following industry best practices and showcases production-ready patterns for distributed systems.

### Architecture

The application consists of two main components:

- **Demo Service**: Core ticket tracking and project management API (Spring Boot with REST endpoints)
- **Outbox Service**: Event relay and email notification processing (RabbitMQ consumer with PostgreSQL persistence)

---

## ✨ Key Features

### Authentication & Authorization

- **Google OAuth2 Integration**: Seamless login via Google accounts
- **JWT-based Sessions**: Secure token-based authentication with configurable expiration
- **Role-Based Access Control**: Three-tier permission system (ADMIN, PROJECT_MANAGER, MEMBER)
- **Token Blacklist Management**: Automatic cleanup of expired tokens via scheduled jobs

### Ticket Management

- **Complete CRUD Operations**: Create, read, update, and delete tickets with full validation
- **Status Tracking**: Multiple ticket statuses for workflow management
- **Priority Levels**: Categorize tickets by urgency
- **Comment System**: Rich collaboration through ticket-level comments
- **Smart Pagination & Sorting**: Flexible data retrieval with sorting by:
  - Due date
  - Priority
  - Creation timestamp
- **Assignee Management**: Assign tickets to team members

### Project Management

- **Project Organization**: Group related tickets into projects
- **Team Collaboration**: Manage multiple team members per project
- **Access Control**: Project-level permissions

### Email Notifications

- **HTML Email Templates**: Professional ticket digest emails using Thymeleaf
- **Scheduled Digests**: Daily ticket digest emails at 8:00 AM (configurable timezone)
- **Reliable Delivery**: Transactional outbox pattern ensuring at-least-once delivery
- **Deduplication**: Prevent duplicate emails using idempotency keys

### Event-Driven Architecture

- **Transactional Outbox Pattern**: Guarantees event publishing consistency with database transactions
- **RabbitMQ Integration**: Asynchronous event processing with exponential backoff retry logic
- **At-Least-Once Delivery Semantics**: Events are reliably delivered despite failures
- **Batch Processing**: Efficient outbox relay with pessimistic locking for safety

### Background Jobs

- **Quartz Scheduler Integration**: Persistent, cluster-safe job scheduling with JDBC JobStore
- **Token Cleanup**: Automatic removal of expired JWT blacklist entries (daily at 1:00 AM)
- **Ticket Digest Generation**: Daily digest emails for actionable tickets (8:00 AM)
- **Misfire Handling**: Configurable retry policies for missed job executions

### API Documentation

- **OpenAPI 3.0 Integration**: Auto-generated API documentation via Swagger UI
- **JWT Bearer Authentication**: Integrated security scheme in documentation
- **Live API Testing**: Interactive API exploration through Swagger UI

### Database

- **PostgreSQL**: Reliable relational database with advanced features
- **Flyway Migrations**: Version-controlled database schema evolution
- **JPA/Hibernate ORM**: Type-safe database interactions
- **Optimized Indexes**: Performance-tuned queries with proper indexing

### Caching

- **Caffeine Cache**: In-memory caching for frequently accessed data
- **Spring Cache Abstraction**: Declarative caching with `@Cacheable`, `@CacheEvict`
- **Configurable TTL**: Time-to-live settings per cache configuration

---

## 🏗️ Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Application                      │
└────────────────────────┬────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────▼────┐  ┌──────▼──────┐  ┌─────▼──────┐
    │ REST API│  │ Swagger UI  │  │Google OAuth2
    │         │  │(OpenAPI)    │  │            │
    └────┬────┘  └─────────────┘  └──────┬─────┘
         │                               │
         └───────────────┬───────────────┘
                         │
              ┌──────────▼──────────┐
              │   Demo Service      │
              │  (Spring Boot 4)    │
              │   - Authentication │
              │   - Ticket CRUD    │
              │   - Project Mgmt   │
              │   - Email Triggers │
              └──────────┬──────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────▼────┐   ┌──────▼───────┐ ┌───▼─────┐
    │PostgreSQL    │  RabbitMQ     │ │ Quartz  │
    │  Database    │  Message Bus  │ │Scheduler
    │             │              │ │         │
    └────────┘   └───────┬──────┘ └────┬────┘
                        │              │
              ┌─────────▼──────────────▼┐
              │  Outbox Service         │
              │ - Event Consumption     │
              │ - Email Processing      │
              │ - Retry Logic           │
              └─────────┬───────────────┘
                        │
              ┌─────────▼──────────┐
              │    Mailpit         │
              │ (SMTP Mock Server) │
              └────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisites

- **Java 21+**
- **Docker & Docker Compose**
- **Maven 3.9+**
- **PostgreSQL 14+** (or use Docker)
- **RabbitMQ** (or use Docker)
- **Google OAuth2 Credentials** (for authentication)

### 1. Environment Setup

Clone the repository and create `.env` files for both services:

**`demo/.env`**
```env
# Database Configuration
DB_USERNAME=ticket_tracker_user
DB_PASSWORD=secure_password_change_me
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ticket_tracker

# Google OAuth2 Configuration
GOOGLE_CLIENT_ID=your_google_client_id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your_google_client_secret

# JWT Configuration
JWT_SECRET_KEY=your_super_secret_jwt_key_minimum_256_bits_required
JWT_EXPIRATION_MS=3600000
REFRESH_TOKEN_EXP_DAYS=7

# RabbitMQ Configuration
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Security Configuration
CSRF_STATUS=false
SECURITY_PATHS_TO_SKIP=/api/auth/**,/api/login/**,/swagger-ui/**,/v3/api-docs/**
SECURITY_ADMIN_ENDPOINTS=/api/admin/**
SECURITY_PM_ENDPOINTS=/api/projects/**
SECURITY_MEMBER_ENDPOINTS=/api/tickets/**
```

**`outbox/.env`**
```env
# Database Configuration
DB_USERNAME=ticket_tracker_user
DB_PASSWORD=secure_password_change_me
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ticket_tracker

# RabbitMQ Configuration
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# Email Configuration
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=noreply@tickettracker.local
```

### 2. Docker Compose Setup

Start all infrastructure services:

```bash
docker-compose up -d
```

This will spin up:
- **PostgreSQL 14**: Main database (port 5432)
- **RabbitMQ**: Message broker with management UI (ports 5672, 15672)
- **Mailpit**: SMTP mock server (ports 1025 for SMTP, 8025 for UI)

### 3. Database Initialization

Flyway migrations run automatically on application startup. No manual setup required.

### 4. Build & Run

**Demo Service (Main Application)**
```bash
cd demo
mvn clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

**Outbox Service (Event Processor)**
```bash
cd ../outbox
mvn clean package
java -jar target/outbox-0.0.1-SNAPSHOT.jar
```

Both services will start on:
- Demo Service: `http://localhost:8080`
- Outbox Service: `http://localhost:8081` (default, configure in `application.yml`)

### 5. Access the Application

| Service | URL |
|---------|-----|
| **API Documentation** | http://localhost:8080/swagger-ui.html |
| **RabbitMQ Management** | http://localhost:15672 (guest/guest) |
| **Mailpit UI** | http://localhost:8025 |
| **Health Check** | http://localhost:8080/actuator/health |
| **API Metrics** | http://localhost:8080/actuator/metrics |

---

## 📚 API Documentation

### Authentication Endpoints

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123",
  "fullName": "John Doe",
  "role": "MEMBER"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 3600000
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

#### Google OAuth2 Login
```http
GET /api/login/oauth2/authorization/google
```

Browser will redirect to Google login, then back to `/api/login/oauth2/code/google?code=...`

#### Refresh Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

---

### Ticket Endpoints

All ticket endpoints require JWT authentication via `Authorization: Bearer <token>` header.

#### Create Ticket
```http
POST /api/tickets
Content-Type: application/json
Authorization: Bearer {token}

{
  "title": "Fix login bug",
  "description": "Users cannot login with Google OAuth",
  "projectId": "550e8400-e29b-41d4-a716-446655440000",
  "priority": "HIGH",
  "status": "OPEN",
  "dueDate": "2026-12-31T23:59:59Z",
  "assigneeId": "550e8400-e29b-41d4-a716-446655440001"
}
```

#### List Tickets (Paginated & Sortable)
```http
GET /api/tickets?page=0&size=20&sortBy=dueDate&sortDirection=asc
Authorization: Bearer {token}
```

**Query Parameters:**
- `page`: Page number (0-indexed, default: 0)
- `size`: Items per page (default: 20)
- `sortBy`: Field to sort by (`dueDate`, `priority`, `createdAt`, or unsorted)
- `sortDirection`: `asc` or `desc` (default: `asc`)

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Fix login bug",
      "description": "Users cannot login with Google OAuth",
      "status": "OPEN",
      "priority": "HIGH",
      "projectId": "550e8400-e29b-41d4-a716-446655440010",
      "assigneeId": "550e8400-e29b-41d4-a716-446655440001",
      "dueDate": "2026-12-31T23:59:59Z",
      "createdAt": "2026-10-09T10:30:00Z",
      "updatedAt": "2026-10-09T10:30:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

#### Get Ticket by ID
```http
GET /api/tickets/{ticketId}
Authorization: Bearer {token}
```

#### Update Ticket
```http
PUT /api/tickets/{ticketId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "title": "Fix login bug - URGENT",
  "status": "IN_PROGRESS",
  "priority": "CRITICAL",
  "dueDate": "2026-10-15T23:59:59Z"
}
```

#### Delete Ticket
```http
DELETE /api/tickets/{ticketId}
Authorization: Bearer {token}
```

---

### Comment Endpoints

#### Add Comment to Ticket
```http
POST /api/tickets/{ticketId}/comments
Content-Type: application/json
Authorization: Bearer {token}

{
  "body": "I've started investigating this issue. More details soon."
}
```

#### Get Ticket Comments
```http
GET /api/tickets/{ticketId}/comments
Authorization: Bearer {token}
```

#### Delete Comment
```http
DELETE /api/comments/{commentId}
Authorization: Bearer {token}
```

---

### Project Endpoints

#### Create Project
```http
POST /api/projects
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "Backend Refactoring",
  "description": "Refactor legacy authentication system"
}
```

#### List Projects
```http
GET /api/projects
Authorization: Bearer {token}
```

#### Add Team Member to Project
```http
POST /api/projects/{projectId}/members
Content-Type: application/json
Authorization: Bearer {token}

{
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "role": "MEMBER"
}
```

---

## 🧪 Testing Workflows

### 1. End-to-End Authentication Flow

```bash
# Step 1: Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "TestPassword123!",
    "fullName": "Test User",
    "role": "MEMBER"
  }'

# Response will contain accessToken and refreshToken
# Save the accessToken for subsequent requests
export TOKEN="<accessToken_from_response>"
```

### 2. Ticket Creation & Management Flow

```bash
# Step 1: Create a project
curl -X POST http://localhost:8080/api/projects \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Q4 Release",
    "description": "Final release of the year"
  }'

# Save the projectId from response
export PROJECT_ID="<projectId>"

# Step 2: Create a ticket
curl -X POST http://localhost:8080/api/tickets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Implement user dashboard",
    "description": "Create dashboard with widgets",
    "projectId": "'$PROJECT_ID'",
    "priority": "HIGH",
    "status": "OPEN",
    "dueDate": "2026-12-31T23:59:59Z"
  }'

# Save the ticketId
export TICKET_ID="<ticketId>"

# Step 3: Add a comment
curl -X POST http://localhost:8080/api/tickets/$TICKET_ID/comments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "body": "Starting work on this ticket"
  }'

# Step 4: List tickets with pagination
curl "http://localhost:8080/api/tickets?page=0&size=10&sortBy=dueDate&sortDirection=asc" \
  -H "Authorization: Bearer $TOKEN"

# Step 5: Update ticket status
curl -X PUT http://localhost:8080/api/tickets/$TICKET_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_PROGRESS"
  }'
```

### 3. Email Notification Flow

```bash
# The ticket digest job runs daily at 8:00 AM (configurable timezone)
# To trigger it manually via Quartz (if administrative access is available):

# Step 1: Create multiple tickets with due dates within next 2 days
curl -X POST http://localhost:8080/api/tickets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Urgent: Fix production bug",
    "projectId": "'$PROJECT_ID'",
    "priority": "CRITICAL",
    "status": "OPEN",
    "dueDate": "2026-10-11T23:59:59Z",
    "assigneeId": "<your_user_id>"
  }'

# Step 2: Monitor Mailpit UI at http://localhost:8025
# You should see the ticket digest email when:
# - The Quartz job triggers at 8:00 AM, OR
# - The scheduled job interval in application.yml triggers

# Step 3: View email details in Mailpit UI
# The email contains HTML-formatted ticket information and action links
```

### 4. Event Processing & Outbox Pattern Flow

```bash
# Monitor the event flow:

# Step 1: Check Outbox Service logs
# Watch for: "Outbox relay: published and removed X/Y events"

# Step 2: Monitor RabbitMQ Management UI
# http://localhost:15672 (guest/guest)
# Check:
# - Exchanges: ticket.events
# - Queues: email.queue
# - Message rates and consumers

# Step 3: Verify email was sent
# Check Mailpit UI at http://localhost:8025
# Each email represents one successfully published event

# Step 4: Test retry logic
# Outbox service implements exponential backoff:
# - Initial retry: 2 seconds
# - Max attempts: 3
# - Backoff multiplier: 2x
# Failures are logged and reattempted accordingly
```

### 5. Token Refresh Flow

```bash
# Step 1: Get initial tokens
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "TestPassword123!"
  }'

# Response contains accessToken and refreshToken
export REFRESH_TOKEN="<refreshToken>"

# Step 2: After access token expires, refresh it
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "'$REFRESH_TOKEN'"
  }'

# New accessToken will be issued
# Old accessToken is added to blacklist (expires at midnight)
```

### 6. Authorization & Role-Based Access Control

```bash
# Step 1: Register users with different roles
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "AdminPass123!",
    "fullName": "Admin User",
    "role": "ADMIN"
  }'

export ADMIN_TOKEN="<accessToken>"

# Step 2: Admin users can access admin endpoints
curl "http://localhost:8080/api/admin/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Step 3: Non-admin users will get 403 Forbidden
curl "http://localhost:8080/api/admin/users" \
  -H "Authorization: Bearer $TOKEN"
# Response: 403 Forbidden
```

### 7. API Documentation (Swagger UI)

```bash
# Navigate to: http://localhost:8080/swagger-ui.html

# Features:
# 1. Interactive API exploration
# 2. Try out requests directly in the browser
# 3. Authentication: Enter JWT token in "Authorize" button
# 4. Request/response schema inspection
# 5. Download OpenAPI specification (JSON/YAML)
```

---

## 🔧 Configuration

### Key Application Properties

**`demo/src/main/resources/application.yml`**

```yaml
# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /

# JWT Configuration
app:
  jwt:
    secret-key: ${JWT_SECRET_KEY}
    access-token-expiration-ms: 3600000  # 1 hour
    refresh-token-expiration-days: 7      # 7 days

# Quartz Job Scheduling
# Token cleanup: Daily at 1:00 AM
# Ticket digest: Daily at 8:00 AM (timezone-aware)

# Cache Configuration
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m

# RabbitMQ Retry Configuration
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          initial-interval: 2000      # Start with 2 seconds
          max-attempts: 3             # Retry up to 3 times
          multiplier: 2.0             # Double interval each time

# Database Configuration
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ticket_tracker
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate             # Never auto-modify schema
    show-sql: true                    # Log SQL statements

# Flyway Migrations
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
```

### Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_USERNAME` | PostgreSQL user | `ticket_tracker_user` |
| `DB_PASSWORD` | PostgreSQL password | `secure_password` |
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID | `xxx.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 secret | `xxxxxxxxxxxxxxx` |
| `JWT_SECRET_KEY` | JWT signing key (min 256 bits) | `your_secret_key_min_256bits` |
| `JWT_EXPIRATION_MS` | Access token TTL in ms | `3600000` |
| `REFRESH_TOKEN_EXP_DAYS` | Refresh token TTL in days | `7` |
| `RABBITMQ_HOST` | RabbitMQ host | `localhost` |
| `RABBITMQ_PORT` | RabbitMQ port | `5672` |
| `RABBITMQ_USERNAME` | RabbitMQ username | `guest` |
| `RABBITMQ_PASSWORD` | RabbitMQ password | `guest` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins (comma-separated) | `http://localhost:3000` |

---

## 📦 Docker Deployment

### Production Docker Compose

Create `docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: ticket_tracker_db
    environment:
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_DB: ticket_tracker
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
      interval: 10s
      timeout: 5s
      retries: 5

  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    container_name: ticket_tracker_mq
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USERNAME}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: rabbitmq-diagnostics -q ping
      interval: 30s
      timeout: 10s
      retries: 5

  demo:
    build:
      context: ./demo
      dockerfile: Dockerfile
    container_name: ticket_tracker_api
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    environment:
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      DB_HOST: postgres
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
      JWT_SECRET_KEY: ${JWT_SECRET_KEY}
      RABBITMQ_HOST: rabbitmq
    ports:
      - "8080:8080"
    restart: unless-stopped

  outbox:
    build:
      context: ./outbox
      dockerfile: Dockerfile
    container_name: ticket_tracker_outbox
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    environment:
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      DB_HOST: postgres
      RABBITMQ_HOST: rabbitmq
      MAIL_HOST: ${MAIL_HOST}
      MAIL_PORT: ${MAIL_PORT}
    ports:
      - "8081:8080"
    restart: unless-stopped

volumes:
  postgres_data:
  rabbitmq_data:
```

### Build and Run Production Containers

```bash
# Build custom images
docker-compose -f docker-compose.prod.yml build

# Run services
docker-compose -f docker-compose.prod.yml up -d

# View logs
docker-compose -f docker-compose.prod.yml logs -f demo
docker-compose -f docker-compose.prod.yml logs -f outbox
```

---

## 🔐 Security Considerations

### Password Hashing
- Passwords are hashed using **BCrypt** with configurable strength factors
- Plaintext passwords never stored in database

### JWT Security
- **HS256 Algorithm** with 256+ bit secret key
- Access tokens: **Short-lived** (default 1 hour)
- Refresh tokens: **Long-lived** (default 7 days)
- Token blacklist prevents reuse of revoked tokens

### OAuth2
- **Google OAuth2** supported out-of-the-box
- Secure callback URL validation
- Sub claim mapping for user identification

### Database
- **SQL Injection Prevention**: Parameterized queries via JPA
- **Row-Level Security**: Role-based access control enforced
- **HTTPS Ready**: CORS and CSRF protection built-in

### API Protection
- **JWT Bearer Authentication**: All endpoints (except auth/login)
- **Rate Limiting**: Configurable per endpoint (if needed)
- **Validation**: Input validation with Jakarta Bean Validation

---

## 📊 Monitoring & Observability

### Application Metrics

Access via **Spring Boot Actuator**:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Application metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/http.server.requests
```

### Logging

- **Log Level**: Configured in `application.yml`
- **Log Location**: `demo/logs/` (rotating daily)
- **Format**: JSON or standard format
- **Trace ID Support**: Spring Cloud Sleuth (optional)

### RabbitMQ Monitoring

Access **RabbitMQ Management UI**:
- URL: http://localhost:15672
- Credentials: guest / guest
- Monitor:
  - Queue depths
  - Consumer counts
  - Message throughput
  - Connection status

### Mailpit Monitoring

Access **Mailpit UI**:
- URL: http://localhost:8025
- Features:
  - View all sent emails
  - Inspect SMTP logs
  - Test email delivery

---

## 🚦 Health Checks

### Database Health
```bash
curl http://localhost:8080/actuator/health/db
```

### RabbitMQ Health
```bash
# Check via RabbitMQ management API
curl http://localhost:15672/api/overview -u guest:guest
```

### Application Readiness
```bash
curl http://localhost:8080/actuator/health/readiness
```

---

## 📝 Database Schema

### Core Tables

**`users`**
- `id` (UUID, PK)
- `email` (VARCHAR, UNIQUE)
- `password_hash` (VARCHAR)
- `full_name` (VARCHAR)
- `role` (ENUM: ADMIN, PROJECT_MANAGER, MEMBER)
- `auth_provider` (ENUM: LOCAL, GOOGLE)
- `google_sub` (VARCHAR, UNIQUE, nullable)
- `created_at` (TIMESTAMP)

**`projects`**
- `id` (UUID, PK)
- `name` (VARCHAR)
- `description` (TEXT)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

**`tickets`**
- `id` (UUID, PK)
- `title` (VARCHAR)
- `description` (TEXT)
- `status` (ENUM: OPEN, IN_PROGRESS, CLOSED)
- `priority` (ENUM: LOW, MEDIUM, HIGH, CRITICAL)
- `project_id` (UUID, FK to projects)
- `assignee_id` (UUID, FK to users)
- `due_date` (TIMESTAMP)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

**`comments`**
- `id` (UUID, PK)
- `ticket_id` (UUID, FK to tickets)
- `author_id` (UUID, FK to users)
- `body` (TEXT)
- `created_at` (TIMESTAMP)

**`outbox_events`**
- `id` (UUID, PK)
- `aggregate_type` (VARCHAR)
- `aggregate_id` (UUID)
- `event_type` (VARCHAR)
- `payload` (TEXT)
- `created_at` (TIMESTAMP)
- `next_attempt_at` (TIMESTAMP)

**`emails`**
- `id` (UUID, PK)
- `recipient` (VARCHAR)
- `subject` (VARCHAR)
- `body` (TEXT)
- `status` (ENUM: PENDING, SENT, FAILED)
- `dedupe_key` (VARCHAR, UNIQUE, nullable)
- `created_at` (TIMESTAMP)

**`token_blacklist`**
- `id` (UUID, PK)
- `jti` (VARCHAR, UNIQUE)
- `expires_at` (TIMESTAMP)

---

## 🐛 Troubleshooting

### Common Issues

**1. PostgreSQL Connection Refused**
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Verify credentials in .env
# Ensure DB_HOST matches service name in docker-compose
```

**2. RabbitMQ Connection Issues**
```bash
# Check RabbitMQ logs
docker logs ticket_tracker_mq

# Verify connectivity
docker exec ticket_tracker_mq rabbitmq-diagnostics -q ping
```

**3. Email Not Sending**
```bash
# Check Mailpit logs
docker logs ticket_tracker_mailpit

# Verify Outbox Service is running
docker logs ticket_tracker_outbox

# Check outbox table for pending events
# SELECT * FROM outbox_events;
```

**4. JWT Token Invalid**
- Ensure `JWT_SECRET_KEY` is at least 256 bits
- Verify token format: `Authorization: Bearer <token>`
- Check token expiration in response

**5. Google OAuth2 Not Working**
- Verify `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in .env
- Ensure callback URL is registered in Google Cloud Console
- Check OAuth2 consent screen configuration

---

## 📖 Additional Resources

### Documentation Files
- [Architecture Guide](./docs/ARCHITECTURE.md)
- [API Specification](./docs/API.md)
- [Development Guide](./docs/DEVELOPMENT.md)
- [Deployment Guide](./docs/DEPLOYMENT.md)

### External Links
- [Spring Boot 4 Documentation](https://spring.io/projects/spring-boot)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [Quartz Scheduler Guide](https://www.quartz-scheduler.org/documentation/)
- [Google OAuth2 Setup](https://developers.google.com/identity/protocols/oauth2)

---

## 🤝 Contributing

Contributions are welcome! Please ensure:

1. Code follows Spring Boot best practices
2. All new endpoints include OpenAPI documentation
3. Comprehensive error handling with meaningful messages
4. Unit and integration tests for new features
5. Database migrations for any schema changes

---

## 📄 License

This project is licensed under the MIT License - see LICENSE file for details.

---

## ✉️ Support

For issues, questions, or suggestions:
1. Check the troubleshooting section above
2. Review API documentation in Swagger UI
3. Inspect application logs: `demo/logs/app.log`
4. Check RabbitMQ and PostgreSQL health

---

## 📊 Project Statistics

- **Language**: Java 21
- **Framework**: Spring Boot 4.1.1
- **Database**: PostgreSQL 14+
- **Message Queue**: RabbitMQ
- **Scheduler**: Quartz (JDBC JobStore)
- **API Docs**: OpenAPI 3.0 (Swagger)
- **Total Services**: 2 (Demo + Outbox)
- **Deployment**: Docker-ready with Docker Compose

---

**Last Updated**: October 2026  
**Maintained By**: EmiCuciu
