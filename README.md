# Shortify

Shortify is a full-stack URL shortener application with analytics, built with Java Spring Boot (backend) and Angular (frontend). It provides fast URL redirection, robust analytics, and scalable architecture using PostgreSQL, Redis, and Kafka.

## Features
- Shorten long URLs to compact, shareable links
- Fast redirection using Redis caching
- Analytics: track link clicks, timestamps, and user metadata
- Asynchronous analytics processing with Kafka
- Scalable and production-ready backend
- Modern Angular frontend

## Tech Stack
- **Backend:** Java 17, Spring Boot, Spring Data JPA, Spring Kafka
- **Database:** PostgreSQL
- **Cache & Rate Limiting:** Redis
- **Message Broker:** Apache Kafka
- **Frontend:** Angular, TypeScript, HTML, CSS
- **Build Tools:** Maven (backend), npm (frontend)

## Architecture Overview
- **Frontend:** Angular SPA for creating, managing, and viewing short URLs and analytics
- **Backend:** Spring Boot REST API
  - **PostgreSQL:** Persistent storage for URLs and analytics
  - **Redis:** Caching for fast short code resolution and rate limiting
  - **Kafka:** Asynchronous event processing for analytics

## Kafka Integration (Analytics)
- When a user accesses a short URL, the backend:
  1. Resolves the original URL (fast path)
  2. Publishes an access event to Kafka (non-blocking)
  3. Returns redirect immediately
- **Consumer:** Processes events to increment click counts and store analytics in the database

### Kafka Functionality
- **Retry / Failure Handling:**
  - The Kafka consumer is configured with retry logic. If processing fails, the event is retried. After maximum retries, the event is sent to a Dead Letter Queue (DLQ) for later inspection and manual intervention.
- **Idempotency:**
  - Consumer logic ensures duplicate events do not result in double-counting. This is achieved by using unique constraints or upsert logic based on event identifiers (such as a combination of shortCode and timestamp).
- **Partitioning Logic:**
  - Kafka messages are partitioned by `shortCode` (used as the message key). This ensures all events for a given short URL are processed in order and enables horizontal scalability.

## Setup Instructions

### Backend
1. `cd shortify-backend`
2. Configure `application.properties` for PostgreSQL, Redis, and Kafka
3. Build and run:
   ```sh
   ./mvnw spring-boot:run
   ```

### Frontend
1. `cd shortify-frontend`
2. Install dependencies:
   ```sh
   npm install
   ```
3. Start the app:
   ```sh
   npm start
   ```

## API Endpoints
- `POST /api/shorten` – Create a short URL
- `GET /{shortCode}` – Redirect to original URL
- `GET /api/stats/{shortCode}` – Get analytics for a short URL

## Testing
- Backend: `./mvnw test`
- Frontend: `npm test`

## Contributing
Contributions are welcome! Please open issues or pull requests for improvements.

## License
MIT License
