# Shortify - Backend

Shortify is a lightweight URL-shortening backend built with Spring Boot. It provides endpoints to shorten URLs, redirect short codes to original URLs, and fetch URL statistics. The project includes a simple AI check component (placeholder logic) that marks URLs as safe/malicious and an optional Redis cache implementation.

Key components

- `com.shortify.controller.UrlController` - HTTP API for creating short URLs and fetching stats.
- `com.shortify.controller.RedirectController` - Handles redirection for `/{shortCode}`.
- `com.shortify.service.UrlServiceImpl` - Core shortening logic (creates `UrlEntity`, persists it, and builds the `shortUrl` returned to the caller).
- `com.shortify.model.UrlEntity` - JPA entity that stores the original URL, short code and metadata. Short codes are generated in the `@PrePersist` method if not provided.
- `com.shortify.async.AiAsyncProcessor` / `com.shortify.service.AiService` - Placeholder AI logic to classify URLs.

Why `shortUrl` contains `http://localhost:8080` and how to fix it

The current implementation builds the returned `shortUrl` with a hard-coded base URL in `UrlServiceImpl`:

- Look at `UrlServiceImpl`: it sets `String shortUrl = "http://localhost:8080/" + urlEntity.getShortCode();` when returning the `ShortenResponse`.
- The short code itself is generated in `UrlEntity#prePersist()` using `RandomStringUtils.randomAlphanumeric(6)` when no custom alias is provided.

Because the base URL is hard-coded to `http://localhost:8080`, the API returns that value. In production you should not return localhost. Options to fix:

1. Use a configuration property (recommended)
   - Add a property like `shortify.base-url` in `application.properties` and inject it into `UrlServiceImpl`.
   - Build the short URL from that property instead of a hard-coded string.

2. Build the absolute URL from request context
   - If the controller has access to the incoming request, construct the base URL from `HttpServletRequest` (scheme, host, port).

3. Return only the short code and let the frontend assemble the full URL.

Project setup (local development)

Prerequisites

- Java 17+ (or required JDK specified by the project)
- Maven
- (Optional) PostgreSQL or chosen RDBMS if you want persistence beyond the embedded DB
- (Optional) Redis if you want caching/ratelimiting backed by Redis

Run locally (backend)

1. Build and run with Maven:

```bash
cd /path/to/shortify-backend
./mvnw clean package
./mvnw spring-boot:run
```

2. Run tests:

```bash
./mvnw test
```

API endpoints

- POST /api/v1/shorten
  - Request JSON example:
    ```json
    {
      "shortUrl": "http://localhost:8080/alias752",
      "originalUrl": "https://chatgpt.com/",
      "ttlInSeconds": null
    }
    ```
  - Note: The `shortUrl` value shown above is what the current code returns; change the base URL as described above before exposing to users.

- GET /{shortCode}
  - Redirects to the original URL.

- GET /api/v1/stats/{shortCode}
  - Returns statistics for a short URL (redirect count, risk level, etc.).

Configuration

- `src/main/resources/application.properties` - Spring Boot settings and placeholders. Add the following to configure a production base URL, for example:

```properties
# example
shortify.base-url=https://short.yourdomain.com
```

Deployment notes

- Make sure to set `shortify.base-url` (or the equivalent mechanism you implement) to your public domain.
- Use environment-specific application properties or Spring profiles to keep dev vs prod values separate.
- Secure the service (rate limiting, authentication for management endpoints, input validation).

Development tips

- Short code generation lives inside `UrlEntity#prePersist()`. If you want more control or to avoid duplicates you can move generation into a service and ensure uniqueness before save.
- If you change how the short URL is built, update `ShortenResponse` and any clients accordingly.

Helpful commands

```bash
# run tests
./mvnw test

# run the app
./mvnw spring-boot:run

# build an executable jar
./mvnw clean package
```

Where to look in code (quick references)

- `src/main/java/com/shortify/service/UrlServiceImpl.java` — builds the `shortUrl` returned to clients (currently hard-coded base URL).
- `src/main/java/com/shortify/model/UrlEntity.java` — short code generation logic in `@PrePersist`.
- `src/main/java/com/shortify/controller/UrlController.java` — endpoint to shorten URLs.
- `src/main/java/com/shortify/controller/RedirectController.java` — redirect handling for incoming short URLs.

Contributing

Fork, create a branch, make changes, add tests and open a PR.

License

Include your license here (e.g., MIT)

