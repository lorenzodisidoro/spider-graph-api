# 🕸️ Spider Graph API & Frontend

A secure, containerized Java Spring Boot API and React frontend deployed on modern serverless infrastructure.

This project consists of:

*   **Backend:** Java Spring Boot API providing two public endpoints
*   **Frontend:** React application consuming the backend
*   **Infrastructure:**
    *   Backend deployed on **Fly.io**
    *   Frontend deployed on **Cloudflare Pages**
    *   Security enforced via:
        *   Cloudflare‑only firewall on the backend (iptables)
        *   Secret header verification between Cloudflare and Fly.io
        *   HTTPS everywhere

***

## 🚀 Features

### 🟣 Backend (Spring Boot)

*   REST API with two public endpoints
*   Packaged as a Docker image
*   Hardened with:
    *   Cloudflare IP allow‑listing via iptables
    *   Mandatory secret header (`X-Api-Secret`) for all requests
    *   Health check endpoint (`/actuator/health`)
*   Automatically deployed via GitHub Actions to Fly.io

***

### 🟣 Frontend (React)

*   Fully static assets served via Cloudflare Pages CDN
*   Build pipeline using Cloudflare Pages
*   Injects the secret request header automatically
*   Communicates with Fly.io backend over HTTPS

***

### 🟣 Security Architecture

*   Backend accepts traffic **only** from Cloudflare IP ranges
*   Backend rejects all requests **missing or with wrong secret header**
*   Cloudflare Pages injects the correct header for every request
*   End-to-end encryption (HTTPS on both Cloudflare and Fly.io)
*   Fly.io secrets store for secure secret management

***

# 🏗️ Deployment Architecture

    User
     ↓
    Cloudflare Pages (React App)
     → Injects "X-Api-Secret" into each request
     → Sends request through Cloudflare network
     ↓
    Fly.io Backend (Spring Boot)
     → Firewall allows Cloudflare IPs only
     → Secret header validated
     → API responds

***

# 📦 Project Structure of spider-graph-api

    spider-graph-api
     ├── src/                      # Spring Boot backend source code
     ├── allow-cloudflare.sh       # Backend firewall setup script
     ├── Dockerfile                # Multi-stage Docker build
     ├── fly.toml                  # Fly.io deployment configuration
     └── .github/workflows/        # GitHub Actions CI/CD
          └── deploy.yml
***

# 🐳 Backend Deployment (Fly.io)

## Prerequisites

*   Fly CLI installed
*   Fly.io account
*   GitHub repository connected

***

### 1. Login

    fly auth login

***

### 2. Create and configure the application

    fly launch

***

### 3. Set secret header on Fly.io

    fly secrets set API_SECRET_HEADER=YourSuperSecretValue

***

### 4. Deploy (manual)

    fly deploy

***

# 🔥 Backend Security Components

## 1. **Cloudflare Firewall Script**

The `allow-cloudflare.sh` script:

*   Fetches Cloudflare IPv4 + IPv6 ranges
*   Configures iptables to allow incoming traffic **only** from Cloudflare
*   Blocks all other sources
*   Starts the Spring Boot application

This ensures only Cloudflare → Fly.io communication is possible.

***

## 2. **Secret Header Validation**

The backend requires:

    X-Api-Secret: <expected value>

Configured via:

    api.secret.header=${API_SECRET_HEADER}

Implemented in:

*   `ApiSecretFilter.java`
*   `FilterConfig.java`

Both required for header-based access control.

***

# 🌐 Frontend Deployment (Cloudflare Pages)

### 1. Connect GitHub repository

Cloudflare Pages auto-detects React apps.

### 2. Build settings

*   Build command: `npm run build`
*   Output directory: `build`

### 3. Inject the secret header

Create:

    frontend/public/_headers

With:

    /*
      X-Api-Secret: YourSuperSecretValue

All requests to the backend will include this header.

***

# ⚙️ GitHub Actions CI/CD

Found under:

    .github/workflows/deploy.yml

This workflow:

1.  Builds the backend using Docker
2.  Deploys to Fly.io using Flyctl
3.  Runs automatically on `push` to `main`

Add secret in GitHub:

    FLY_API_TOKEN = <output of `fly auth token`>

***

# 🔍 Health Check

Fly.io monitors:

    GET /actuator/health

If it returns:

    {"status":"UP"}

The deployment remains healthy.

***

# 🧪 Testing

### Test API with header:

    curl -H "X-Api-Secret: YourSuperSecretValue" \
         https://your-app.fly.dev/api/example

### Test API *without* header (should fail):

    curl https://your-app.fly.dev/api/example

Should return:

    403 Forbidden

***

# 📝 Environment Variables Summary

### On Fly.io

    API_SECRET_HEADER

### On Cloudflare Pages (optional display)

    X_API_SECRET
