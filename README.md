# Shortify

Shortify is a scalable URL shortening platform with a rich feature set for both end-users and developers. It provides fast and reliable URL redirection, custom aliases, analytics, rate limiting, AI-powered risk detection, and a responsive Angular frontend. The backend is built with Spring Boot, PostgreSQL, Redis, and Kafka for asynchronous analytics.

## Project Overview

- **Frontend:** Angular SPA for creating, managing, and analyzing short URLs.
- **Backend:** Spring Boot REST API for URL shortening, redirection, analytics, and risk detection.
- **Database:** PostgreSQL for persistent storage, Redis for caching and distributed rate limiting.
- **Analytics:** Kafka-based asynchronous event processing for high-throughput analytics.

## How It Works

1. **Shorten a URL:**
   - User submits a long URL (optionally with a custom alias) via the web UI or API.
   - Backend generates a unique short code (using encoding logic, e.g., Base62) and stores the mapping in PostgreSQL.
   - AI risk detection runs asynchronously to classify the URL.
   - The short URL is returned to the user.

2. **Redirect:**
   - User accesses the short URL (e.g., `/abc123`).
   - Backend looks up the original URL (using Redis cache for speed, falling back to PostgreSQL if needed).
   - Redirect is issued instantly (HTTP 302).
   - An analytics event is published to Kafka for asynchronous processing.

3. **Analytics & Stats:**
   - Kafka consumer updates click counts and stores access analytics (timestamp, IP, user agent) in the database.
   - Users can view statistics and trends for their short URLs via the frontend or API.

4. **Rate Limiting:**
   - All API endpoints are protected by distributed rate limiting (Bucket4j + Redis) to prevent abuse.

## Key Features

- **Fast URL Redirection:** Optimized for low-latency redirects using Redis and indexed PostgreSQL queries.
- **Custom Aliases:** Users can specify their own short codes.
- **Comprehensive Analytics:** Track clicks, referrers, user agents, and more (processed asynchronously via Kafka).
- **Rate Limiting:** Prevents abuse with configurable per-IP limits for shortening and access.
- **AI-Powered Risk Detection:** Classifies URLs as SAFE, LOW_RISK, MEDIUM_RISK, or HIGH_RISK using async processing.
- **Caching:** Redis is used for fast lookups and to reduce database load.
- **Frontend UI:** Angular SPA for easy URL management and analytics visualization.

## Architecture Overview

```
User (Web/REST) → Angular Frontend → Spring Boot Backend
                                 ├─ Shorten URL (DB/Cache, AI Risk)
                                 ├─ Redirect (DB/Cache, Kafka Event)
                                 └─ Analytics (Async via Kafka)
```

## API Endpoints

### Create Short URL
```bash
POST /api/v1/shorten
{
    "originalUrl": "https://example.com/long/url",
    "ttlInSeconds": 86400,
    "customAlias": "my-link"
}
```

### Redirect
```bash
GET /{shortCode}
# Returns 302 with Location header, publishes analytics event
```

### Get URL Statistics
```bash
GET /api/v1/stats/{shortCode}
# Returns: redirectCount, createdAt, riskLevel, etc.
```

## Analytics with Kafka (Backend)

- **Non-blocking analytics:** Redirects are never delayed by analytics processing.
- **Event-driven:** Each redirect publishes an event to Kafka.
- **Consumer:** Processes events to increment click counts and store analytics data.
- **Scalable:** Handles high throughput and decouples analytics from core redirect logic.

## Rate Limiting

- **Token Bucket Algorithm** (Bucket4j)
- **Redis-backed** for distributed enforcement
- **Configurable limits:**
  - URL shortening: 10 requests/minute per IP
  - URL access: 100 requests/minute per IP

## AI Risk Detection

- **Asynchronous:** URL risk is checked in the background after creation.
- **Risk levels:** SAFE, LOW_RISK, MEDIUM_RISK, HIGH_RISK
- **Stats available:** Users can see risk level for each short URL.

## Database Schema

### UrlEntity (Main URLs Table)
```sql
id | original_url | short_code | custom_alias | created_at | ttl_in_seconds | redirect_count | ai_checked | risk_level
```

### UrlAccessAnalytics (Analytics Table)
```sql
id | url_id | short_code | original_url | accessed_at | ip_address | user_agent | created_at
```

## Tech Stack

### Backend
- **Spring Boot 3.5.9**
- **Spring Kafka 3.3.11**
- **PostgreSQL**
- **Redis**
- **Bucket4j**
- **Kafka 3.9.1**

### Frontend
- **Angular 19+**
- **TypeScript**
- **RxJS**

## Project Structure

```
shortify/
├── README.md
├── shortify-backend/
│   ├── pom.xml
│   ├── src/main/java/com/shortify/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── kafka/
│   │   ├── event/
│   │   ├── model/
│   │   ├── repository/
│   │   └── config/
│   └── src/test/java/com/shortify/
│       └── tests/
└── shortify-frontend/
    └── src/app/
```

## Getting Started

### Prerequisites
- Java 21+
- PostgreSQL 12+
- Redis 6.0+
- Apache Kafka 3.9+

### Backend Setup

1. **Configure databases** (application.properties)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/shortify
spring.redis.host=localhost
spring.kafka.bootstrap-servers=localhost:9092
```

2. **Build and run**
```bash
cd shortify-backend
./mvnw clean install
./mvnw spring-boot:run
```

### Frontend Setup
```bash
cd shortify-frontend
npm install
ng serve --open
```

## Testing

Run all Kafka integration tests:
```bash
cd shortify-backend
./mvnw test -Dtest=KafkaIntegrationTest
```
## License

MIT License - See LICENSE file for details
