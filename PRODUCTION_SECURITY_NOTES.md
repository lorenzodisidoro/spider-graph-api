# Production Security Notes

This document collects practical recommendations for deploying `spider-graph-api` in production.

The application is intentionally public-facing and does not require user authentication. That means the main risks are abuse, SSRF, bot traffic, resource exhaustion, and proxy misconfiguration.

## 1. Put the API Behind a Reverse Proxy

Do not expose the Spring Boot container directly to the Internet.

Recommended front layer:

- NGINX
- Traefik
- Cloudflare Tunnel / Cloudflare Proxy
- AWS ALB + WAF
- another managed reverse proxy with rate limiting and IP filtering

The reverse proxy should handle:

- TLS termination
- request size limits
- IP allow/deny rules when needed
- rate limiting before traffic reaches the application
- removal of untrusted `X-Forwarded-*` headers from external clients

Why this matters:

- Spring documents that forwarded headers are security-sensitive and should only be trusted if a proxy at the trust boundary removes untrusted ones first.

## 2. Only Trust `X-Forwarded-For` When the Proxy Is Trusted

The application already supports this through:

- `app.security.forward-headers.trust-x-forwarded-for=false` by default

Production guidance:

- keep it `false` unless traffic always comes through a trusted reverse proxy
- set it to `true` only if the proxy strips any incoming `Forwarded` and `X-Forwarded-*` headers and re-adds its own values

If you enable it without a trusted proxy, attackers can spoof client IPs and weaken rate limiting.

## 3. Add Rate Limiting at the Proxy Layer

Application-level rate limiting is useful, but it should not be your first line of defense.

Add proxy or edge rate limiting for:

- `POST /api/crawls/sync`
- `POST /api/crawls/async`

Suggested starting point:

- per-IP rate limit at the proxy
- low burst values
- stricter rules on the async endpoint if it is more expensive

Example NGINX approach:

- define a `limit_req_zone` keyed by `$binary_remote_addr`
- apply `limit_req` on `/api/crawls/`

This reduces bot traffic and protects the JVM before requests enter the application.

## 4. Use a WAF or Bot Protection at the Edge

Because these APIs are public, expect:

- scraping
- repeated crawl submissions
- cheap bot probes
- malformed payload floods

Recommended edge protections:

- Cloudflare WAF / Bot Management
- AWS WAF
- managed bot protection from your hosting provider

Useful controls:

- country filtering if your use case is geographically limited
- ASN/IP reputation blocking
- bot score challenges
- temporary IP bans on repeated `429`, `400`, or `503` responses
- request anomaly detection for high-volume identical JSON payloads

If you do not want interactive challenges for all users, apply them only after abuse thresholds are crossed.

## 5. Restrict Crawl Targets as Much as Your Use Case Allows

This application is effectively a controlled SSRF surface by design, because users submit URLs that the backend fetches.

Current code already helps by:

- allowing only `http` and `https`
- blocking private, loopback, link-local, multicast, CGNAT, and documentation IP ranges
- restricting ports to configured allowed ports

For production, prefer stronger controls:

- keep `app.security.crawl.allowed-ports=80,443` unless there is a real business need for more
- set conservative values for `max-depth`, `max-timeout`, `max-request-delay`, and `max-pages`
- if possible, allow only a known allowlist of domains or suffixes
- block redirects to destinations that would fail the same safety validation
- monitor requests attempting cloud metadata targets such as `169.254.169.254`

If your product only needs a small set of websites, an allowlist is much safer than a deny-list.

## 6. Disable Public API Docs in Production Unless Needed

The app is already configured to disable Swagger UI and API docs in `prod`:

- `springdoc.api-docs.enabled=false`
- `springdoc.swagger-ui.enabled=false`

Keep them disabled on public production deployments unless you have a real operational need.

If you do need them:

- expose them only on an internal network
- or protect them at the proxy layer by IP allowlist

## 7. Enforce Strict Request Limits

Keep request bodies small. The JSON payload for these APIs is simple and should not need large uploads.

Recommended proxy-level limits:

- low `client_max_body_size` / max request body size
- low request header size limits
- short read timeout for request bodies
- connection timeout and idle timeout limits

This helps against:

- slowloris-style abuse
- oversized payloads
- parser resource exhaustion

## 8. Make the Container and Host Network Boring

Deploy the application with the least privilege possible.

Recommended container/runtime settings:

- run as non-root
- read-only root filesystem if possible
- no host networking
- no privileged mode
- minimal outbound egress where your platform supports it

If you can control egress:

- deny access to private networks from the container
- deny access to cloud metadata endpoints
- allow only outbound ports `80` and `443`

Network-level egress controls are an important second layer in case URL validation is bypassed.

## 9. Keep `prod` CORS Strict

Only allow your real frontend origin through:

- `APP_SECURITY_CORS_ALLOWED_ORIGINS=https://your-frontend.example`

Do not leave localhost origins in production.
Do not use `*`.

If multiple frontends are needed, enumerate them explicitly.

## 10. Log for Abuse Detection

At minimum, collect:

- client IP as seen after trusted proxy handling
- request path
- submitted `startUrl`
- whether the request was blocked by validation, rate limiting, or capacity limits
- crawl completion stats such as node count and stop reason

Forward logs to a central system and alert on:

- spikes in `429`
- spikes in invalid URL errors
- repeated attempts to use blocked ports
- repeated requests toward localhost-like or metadata-like targets

## 11. Use External Monitoring

Recommended baseline:

- uptime checks on the API
- JVM/container metrics
- reverse proxy metrics
- dashboard for request rate, 4xx, 5xx, and response latency

Create alerts for:

- sustained `503` responses
- high CPU or memory
- unusual growth in request rate
- sudden increases in crawl duration

## 12. Patch Dependencies and Rebuild Frequently

This project already needed several transitive dependency updates. Keep that process operational:

- run dependency scanning in CI
- rebuild the Docker image regularly
- do not rely on an old image staying safe forever

## 13. Example Production Checklist

Before going live, confirm:

- the app runs with `SPRING_PROFILES_ACTIVE=prod`
- `APP_SECURITY_CORS_ALLOWED_ORIGINS` is set to the real frontend domain
- Swagger/OpenAPI are not publicly exposed
- the app is behind a trusted reverse proxy
- `app.security.forward-headers.trust-x-forwarded-for=true` only if the proxy is correctly configured
- edge rate limiting is enabled
- WAF/bot protections are enabled
- outbound egress to private networks and metadata endpoints is blocked
- logs and alerts are configured
- Docker image and dependencies are up to date

## Suggested NGINX Controls

If you use NGINX, start with:

- TLS only
- `real_ip_header` and trusted proxy configuration only if applicable
- `limit_req_zone` and `limit_req`
- low body size limits
- short timeouts
- header cleanup for forwarded headers

This is an inference from the current application design: the biggest security gains will come from the edge, not from additional complexity inside the controller layer.

## References

- Spring Forwarded Headers security notes: https://docs.spring.io/spring-framework/reference/web/webmvc/filters.html
- Spring `ForwardedHeaderFilter` Javadoc: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/filter/ForwardedHeaderFilter.html
- OWASP SSRF Prevention Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html
- NGINX request rate limiting: https://nginx.org/en/docs/http/ngx_http_limit_req_module.html
