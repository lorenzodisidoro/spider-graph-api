# Spider Graph API

Spring Boot REST API that exposes the features of `spider-graph-lib` to run synchronous and asynchronous crawls on web pages.

## Requirements

- Java 17
- Maven 3.9+
- Docker 24+ or Docker Desktop

## Features

- REST endpoints for synchronous and asynchronous crawling
- input validation and basic URL safety checks
- application-level rate limiting
- configurable limits for depth, timeout, request delay, and maximum number of pages
- OpenAPI documentation and Swagger UI

## Run Locally

```bash
mvn spring-boot:run
```

By default, the application starts with the `dev` profile on `http://localhost:8080`.

## Local Build

```bash
mvn clean package
java -jar target/spider-graph-api-1.0-SNAPSHOT.jar
```

## Run with Docker

### Build the Image

```bash
docker build -t spider-graph-api:latest .
```

### Run the Container

```bash
export SPRING_PROFILES_ACTIVE=prod
export JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=30"
export APP_SECURITY_CORS_ALLOWED_ORIGINS=https://app.example.com

docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE \
  -e JAVA_TOOL_OPTIONS \
  -e APP_SECURITY_CORS_ALLOWED_ORIGINS \
  spider-graph-api:latest
```

### Run with Docker Compose

```bash
export SPRING_PROFILES_ACTIVE=prod
export JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=30"
export APP_SECURITY_CORS_ALLOWED_ORIGINS=https://app.example.com

docker compose up --build
```

You can also export all main variables before startup:

```bash
export SPRING_PROFILES_ACTIVE=prod
export JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=30"
export APP_SECURITY_CORS_ALLOWED_ORIGINS=https://app.example.com
export APP_SECURITY_RATE_LIMIT_MAX_REQUESTS_PER_MINUTE=10
export APP_SECURITY_CRAWL_MAX_CONCURRENT_REQUESTS=1
export APP_SECURITY_CRAWL_MAX_DEPTH=3
export APP_SECURITY_CRAWL_MAX_TIMEOUT=10000
export APP_SECURITY_CRAWL_MAX_REQUEST_DELAY=2000
export APP_SECURITY_CRAWL_MAX_PAGES=50
```

## Configuration

The main properties can be configured through environment variables:

| Spring Property | Environment Variable | Default |
| --- | --- | --- |
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` | `dev` |
| JVM container sizing | `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=30` |
| `app.security.cors.allowed-origins` | `APP_SECURITY_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://127.0.0.1:3000` in `dev` |
| `app.security.rate-limit.max-requests-per-minute` | `APP_SECURITY_RATE_LIMIT_MAX_REQUESTS_PER_MINUTE` | `10` |
| `app.security.crawl.max-concurrent-requests` | `APP_SECURITY_CRAWL_MAX_CONCURRENT_REQUESTS` | `1` |
| `app.security.crawl.max-depth` | `APP_SECURITY_CRAWL_MAX_DEPTH` | `3` |
| `app.security.crawl.max-timeout` | `APP_SECURITY_CRAWL_MAX_TIMEOUT` | `10000` |
| `app.security.crawl.max-request-delay` | `APP_SECURITY_CRAWL_MAX_REQUEST_DELAY` | `2000` |
| `app.security.crawl.max-pages` | `APP_SECURITY_CRAWL_MAX_PAGES` | `50` |

For a real deployment, set `APP_SECURITY_CORS_ALLOWED_ORIGINS` to your frontend domain.
If you run the application in Docker, this variable overrides the value defined in the `application-*.properties` files.
The `docker-compose.yml` file is configured to fail at startup if `APP_SECURITY_CORS_ALLOWED_ORIGINS` is not set.

## Main Endpoints

- `POST /api/crawls/sync`
- `POST /api/crawls/async`
- `GET /swagger-ui.html`
- `GET /openapi/spider-graph-api.yaml`

## Example Request

```bash
curl -X POST http://localhost:8080/api/crawls/sync \
  -H "Content-Type: application/json" \
  -d '{
    "startUrl": "https://example.com",
    "maxDepth": 1,
    "timeout": 5000,
    "requestDelay": 250,
    "userAgent": "SpiderGraphApi/1.0",
    "verifyHost": true
  }'
```

## Crawl Behavior

- crawling stops when the configured limits are reached
- when `app.security.crawl.max-pages` is reached, the application returns the partial graph collected so far
- the library uses `setCrawlStepHook(...)` to stop the crawl without losing already collected nodes

## Profiles

- `dev`: intended for local development, enables CORS for `localhost:3000`
- `prod`: intended for deployment; always update allowed origins or provide them through environment variables

## Testing

```bash
mvn test
```

## Docker Notes

- the `Dockerfile` uses a multi-stage build: Maven for compilation and a lighter Java 17 runtime image
- the container exposes port `8080`
- the process runs as a non-root user
- the Docker build requires network access to download Maven dependencies and the JitPack dependency `spider-graph-lib`

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).