Shortify — URL shortener (frontend + backend)

Overview

Shortify is a simple URL-shortening project containing two parts in this repository:

- shortify-backend: A Spring Boot backend (Java 21, Maven) that provides URL shortening, resolution, stats endpoints and uses PostgreSQL and Redis (for rate limiting / caching).
- shortify-frontend: An Angular (v21) frontend that calls the backend API and includes an SSR-capable build.

This README explains how to build, run, and test each part locally and how to configure the services they depend on.

Repository layout

- shortify-backend/ — Spring Boot application
  - pom.xml (Java 21)
  - src/main/resources/application.properties (default DB/Redis settings)
  - mvnw (Maven wrapper)

- shortify-frontend/ — Angular app
  - package.json (npm scripts: start, build, test, serve:ssr:shortify-frontend)
  - src/environments/environment.ts (default apiUrl for local dev)

Requirements

- Java 21 (project property sets java.version=21)
- Maven (you can use the provided Maven wrapper `./mvnw`)
- PostgreSQL (local or remote)
- Redis (local or remote)
- Node.js and npm (package.json lists packageManager: "npm@11.6.2" and TypeScript 5.9; Node 20+ is recommended)

Backend (shortify-backend)

Defaults (from src/main/resources/application.properties)

- server.port=8080
- spring.datasource.url=jdbc:postgresql://localhost:5432/shortify
- spring.datasource.username=shortify
- spring.datasource.password=postgres
- spring.jpa.hibernate.ddl-auto=update
- spring.redis.host=localhost
- spring.redis.port=6379

Build & run (development)

1. From the backend folder install/build/run using the Maven wrapper:

```bash
cd shortify-backend
./mvnw spring-boot:run
```

This runs the app on http://localhost:8080 by default.

Build a jar (production-like)

```bash
cd shortify-backend
./mvnw clean package -DskipTests
# then run the generated jar (name may vary):
java -jar target/*.jar
```

Tests

```bash
cd shortify-backend
./mvnw test
```

Configuration & Environment variables

Spring Boot properties in `application.properties` can be overridden with environment variables or system properties. Some common overrides (example):

- SPRING_DATASOURCE_URL (e.g. jdbc:postgresql://db:5432/shortify)
- SPRING_DATASOURCE_USERNAME
- SPRING_DATASOURCE_PASSWORD
- SPRING_REDIS_HOST
- SPRING_REDIS_PORT
- SERVER_PORT (or SPRING_APPLICATION_JSON)

Example running with env vars:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/shortify \
SPRING_DATASOURCE_USERNAME=shortify \
SPRING_DATASOURCE_PASSWORD=postgres \
SPRING_REDIS_HOST=localhost \
SPRING_REDIS_PORT=6379 \
./mvnw spring-boot:run
```

Database (PostgreSQL) local quick setup

If you use a local postgres (commands assume a default postgres superuser):

```bash
# As the postgres user (may need sudo depending on your install):
# Create user and DB with the credentials used by default app
sudo -u postgres psql -c "CREATE USER shortify WITH PASSWORD 'postgres';"
sudo -u postgres psql -c "CREATE DATABASE shortify OWNER shortify;"
```

Adjust the SQL/commands to match how Postgres is installed on your machine (Homebrew, Docker, cloud DB, etc.).

Redis local quick setup

On macOS with Homebrew:

```bash
brew install redis
brew services start redis
# or run a quick server:
redis-server
```

Frontend (shortify-frontend)

The frontend is an Angular 21 project. It uses an `environment.ts` with a default apiUrl pointing at http://localhost:8080.

Key files:

- src/environments/environment.ts — development apiUrl: http://localhost:8080
- src/environments/environment.prod.ts — production apiUrl: https://your-backend-domain.com
- src/app/core/services/url.service.ts — the frontend hits `${environment.apiUrl}/api/v1` for backend endpoints

Install, run, build

1. Install dependencies

```bash
cd shortify-frontend
npm install
```

2. Run local dev server (Angular dev server)

```bash
npm start
# or
ng serve
```

By default the dev server serves at http://localhost:4200.

3. Build for production (static site)

```bash
npm run build
# Output will be in dist/shortify-frontend
```

4. Server-Side Rendering (SSR) / production Node server

This repository already has an SSR-capable setup and a package script that can serve the SSR build:

```bash
# build production (client + server) — exact command depends on your Angular workspace configuration
npm run build
# then run the SSR entry produced under dist/
npm run serve:ssr:shortify-frontend
```

If you prefer to host the built static files from the backend, copy the `dist/shortify-frontend` output into the backend static resources (or configure your web server to serve those files).

Connecting frontend to backend

- Dev flow: run backend on port 8080 and frontend using `ng serve`. The frontend's `environment.ts` points to http://localhost:8080 already.
- Production flow: set `environment.prod.ts`'s `apiUrl` to your backend URL, or set the environment at build time.

Tests

Frontend tests are configured in package.json (ng test / vitest):

```bash
cd shortify-frontend
npm test
```

Quick local run (both services)

1. Start Postgres and create DB + user (see DB section).
2. Start Redis.
3. Start backend:

```bash
cd shortify-backend
./mvnw spring-boot:run
```

4. Start frontend (in another terminal):

```bash
cd shortify-frontend
npm install
npm start
```

Open http://localhost:4200 and use the UI. The frontend will call the backend on http://localhost:8080 by default.

Common troubleshooting

- Java version errors: make sure you run with Java 21 (the pom.xml sets java.version=21).
- DB connection errors: confirm `spring.datasource.url`, username and password are reachable and correct. Check Postgres logs for details.
- Redis connection errors: confirm `spring.redis.host`/`spring.redis.port` or configure via env vars.
- Port clashes: backend defaults to 8080, frontend dev server to 4200. If ports are in use, either change `server.port` or pass SERVER_PORT env var.
- CORS issues: During local dev `ng serve` proxies requests to backend; if you see CORS errors the backend might need CORS configuration (check `controller` or `config` packages in the backend).

Development notes & next steps

- The backend uses Spring Data JPA (Postgres) and Redis + Bucket4j for rate limiting (see pom.xml dependencies).
- The frontend uses Angular 21 and includes SSR-related files. Decide whether to deploy SSR or static build in production.
- You can containerize each service (Dockerfile) or use docker-compose to bring up Postgres + Redis + backend + frontend for full-stack local testing.

Contribution

Contributions are welcome — open issues and PRs. Add simple unit tests and follow existing code style (Lombok is used in the backend).

License

Add your preferred license file to the repo (e.g. MIT, Apache-2.0) if you want to make the project public.


