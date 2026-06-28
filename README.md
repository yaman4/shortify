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
  - **Redis:** Write-through caching with per-URL configurable TTLs for fast short code resolution
  - **Kafka:** Asynchronous event processing for analytics
  - **Rate Limiting:** Token bucket algorithm (Bucket4j) with per-IP, per-endpoint limiting

## Caching Strategy
- **Type:** Write-through caching with per-URL configurable TTLs
- **Implementation:** Redis (StringRedisTemplate) backed by RedisCacheService
- **Cache Key Format:** `shortify:{shortCode}` (string key-value pairs)
- **TTL Behavior:** 
  - If `ttlInSeconds` provided on URL creation → respects that TTL
  - If no TTL provided → default 365-day cache duration
  - Redis auto-expiration matches database cleanup schedule
- **Cache Warmup:** 
  - On URL creation: immediately cached after DB persistence
  - On first redirect: cached on DB miss for future hits
- **Redirect Path:** Cache-first lookup (1-2ms hit) → DB fallback (50-100ms miss) → cache repopulation
- **Performance:** ~5-10x faster redirects for hot/cached URLs

## Rate Limiting
- **Strategy:** Hybrid local + distributed (Bucket4j + Redis)
- **Architecture:** 
  - **Fast Path:** Local in-memory Bucket4j per instance (~1-2ms)
  - **Cross-Instance Sync:** Redis tracks consumption for distributed awareness
  - **Fallback:** If local bucket exhausted, check Redis state
  - **Resilience:** Graceful degradation if Redis unavailable (fail-open)
- **Limits by Endpoint:**
  - `/api/v1/shorten`: 10 requests per minute per IP
  - All other endpoints: 100 requests per minute per IP
- **Key Format:** `rate_limit:{ip}:{path}` (RAtomicLong in Redis)
- **Expiration:** Auto-cleanup after 1 minute (no stale counters)
- **Enforcement:** Per-IP basis (different IPs have separate limits)
- **Implementation Details:**
  - Each request first checks local Bucket4j (no network I/O)
  - On token consumption, syncs to Redis (async-friendly)
  - If local bucket empty, queries Redis for cross-instance state
  - Returns 429 (Too Many Requests) when limit exceeded

## Kafka Integration (Analytics)
- When a user accesses a short URL, the backend:
  1. Resolves the original URL, low-latency redirect path (served via Redis cache)
  2. Publishes an access event to Kafka (non-blocking)
  3. Returns redirect immediately
- **Consumer:** Processes events to increment click counts and store analytics in the database

### Kafka Functionality
- **Event Type:** `UrlAccessEvent` published on every redirect
  - Payload: `shortCode`, `timestamp`, `ip`, `userAgent`, `originalUrl`, `urlId`
  - Topic: `url-access-events` (3 partitions)
  - Partitioning: By `shortCode` to ensure ordering per URL
- **Producer Config:** Acks=1 (leader write), 3x retries with 100ms backoff, Snappy compression
- **Consumer Config:** 
  - Group: `shortify-analytics-group`
  - Concurrency: 3 (processes from multiple partitions in parallel)
  - Auto-commit: Every 5 seconds
- **Retry / Failure Handling:**
  - 3 retry attempts with 1-second fixed backoff (~3 seconds total)
  - After max retries → message sent to Dead Letter Queue (DLQ)
  - DLQ Topic: `url-access-events.DLQ` (requires manual inspection/recovery)
- **Idempotency:**
  - Consumer deduplicates by `shortCode + timestamp + userAgent`
  - Prevents double-counting on retries
- **Decoupling:**
  - Events published asynchronously (non-blocking fire-and-forget)
  - Redirect response sent immediately (doesn't wait for consumer)
  - Analytics updated eventually-consistent (seconds later)
- **Scalability:**
  - Kafka consumers horizontally scalable with consumer groups
  - Message partitioning by shortCode enables parallel processing
  - Kafka doesn't impact redirect latency

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

## Docker Deployment

- The application is containerized using Docker for consistent builds and deployment across environments.
- Backend (Spring Boot) is packaged into a Docker image and runs as an independent service.
- Frontend (Angular) is built and served using an Nginx-based Docker container.

### Build & Run

Build backend image:  
docker build -t shortify-backend .

Run backend container:  
docker run -d -p 8080:8080 shortify-backend

Build frontend image:  
docker build -t shortify-frontend .

Run frontend container:  
docker run -d -p 3000:80 shortify-frontend

## AWS EC2 Deployment

- The application is deployed on an AWS EC2 instance (Ubuntu) as a cloud-hosted environment.
- Docker is installed on the EC2 instance to run containerized services.
- Backend and frontend containers are run on the instance and exposed via appropriate ports.
- Nginx is used as a reverse proxy to serve the frontend and route traffic efficiently.
- Supporting services such as PostgreSQL, Redis, Kafka, and Zookeeper are also deployed and managed within the same environment to enable full system functionality.

Access: http://<EC2_PUBLIC_IP>:3000

## Contributing
Contributions are welcome! Please open issues or pull requests for improvements.

## License
MIT License
