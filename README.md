# CodeSage

**AI-powered code intelligence platform** for automated pull request reviews, semantic code search, and repository understanding.

Built as a production-style **modular monolith** (Java 21, Spring Boot 3) — designed to demonstrate SDE-2 backend engineering: system design, event-driven architecture, RAG, and provider-agnostic integrations.

[![CI](https://github.com/YOUR_GITHUB_USERNAME/codesage/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_GITHUB_USERNAME/codesage/actions/workflows/ci.yml)

## Highlights

- **Provider-agnostic SCM layer** — GitHub today; GitLab/Bitbucket/Azure DevOps via interfaces (`ScmProvider`, `ReviewPublisher`, …)
- **AST-based semantic indexing** — JavaParser chunks (class/method/constructor), not fixed token splits
- **RAG pipeline** — pgvector HNSW similarity search with metadata filtering
- **AI PR review** — diff → modified method location → RAG context → GPT-4.1 → ranked findings → GitHub comments
- **Event-driven** — Kafka for indexing, embeddings, reviews; Redis for locks, dedup, rate limits

## Architecture

```
GitHub Webhook / REST API
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│              Platform Gateway (REST, Auth)               │
└─────────────────────────┬───────────────────────────────┘
                          ▼
                   Apache Kafka
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
   Repo Indexing    AI Review Engine    Audit
   (JGit, AST)     (Diff, RAG, LLM)          
          │               │
          └───────┬───────┘
                  ▼
        PostgreSQL + pgvector
              Redis
```

Detailed design: [docs/architecture.md](docs/architecture.md)

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Database | PostgreSQL 16 + pgvector |
| Cache | Redis 7 |
| Messaging | Apache Kafka 3.9 |
| AST | JavaParser |
| SCM | JGit + GitHub REST API |
| LLM | OpenAI GPT-4.1 + text-embedding-3-large |
| Build | Gradle |
| Infra | Docker Compose |

## Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose
- OpenAI API key (for embeddings + reviews)
- GitHub personal access token (for private repos / posting reviews)

### 1. Start infrastructure

```bash
cd codesage
docker compose up -d
```

Wait until Postgres, Redis, and Kafka are healthy.

### 2. Configure environment

```bash
export OPENAI_API_KEY=sk-your-key
export INDEXING_WORKSPACE=/tmp/codesage/repos   # optional
```

### 3. Run the application

```bash
cd codesage
./gradlew bootRun        # Linux/macOS
gradlew.bat bootRun      # Windows
```

Health check: `GET http://localhost:8080/actuator/health`

### 4. Bootstrap (create org + API key)

```bash
# Create organization (open endpoint)
curl -s -X POST http://localhost:8080/api/v1/organizations \
  -H "Content-Type: application/json" \
  -d '{"name":"Acme Corp","slug":"acme"}'

# Save organizationId from response, then create API key
curl -s -X POST http://localhost:8080/api/v1/organizations/{orgId}/api-keys \
  -H "Content-Type: application/json" \
  -d '{"name":"default"}'

# Save the raw apiKey — shown only once
export API_KEY=cs_...
```

## Demo Flow

End-to-end: **register repo → index → semantic search → trigger PR review**.

### Step 1 — Register a GitHub repository

```bash
curl -s -X POST "http://localhost:8080/api/v1/organizations/{orgId}/repositories" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{
    "provider": "GITHUB",
    "owner": "your-github-username",
    "repositoryName": "your-repo",
    "accessToken": "ghp_your_token",
    "webhookSecret": "your-webhook-secret"
  }'
```

Registration auto-triggers a full index via Kafka. Check status:

```bash
curl -s "http://localhost:8080/api/v1/repositories/{repoId}/index-status" \
  -H "X-API-Key: $API_KEY"
```

Wait until `indexingStatus` is `COMPLETED`.

### Step 2 — Semantic code search

```bash
curl -s -X POST http://localhost:8080/api/v1/search/semantic \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{
    "repositoryId": "{repoId}",
    "query": "authentication token validation",
    "topK": 5,
    "chunkType": "METHOD"
  }'
```

### Step 3 — Trigger AI PR review

Option A — **REST API** (after a PR exists in the DB, e.g. from webhook):

```bash
curl -s -X POST "http://localhost:8080/api/v1/pull-requests/{pullRequestId}/reviews" \
  -H "X-API-Key: $API_KEY"
```

Option B — **GitHub webhook** (configure in repo settings):

```
POST http://your-host/api/v1/webhooks/github/{orgId}
Events: pull_request, push
```

### Step 4 — Fetch review results

```bash
# List reviews for a PR
curl -s "http://localhost:8080/api/v1/pull-requests/{pullRequestId}/reviews" \
  -H "X-API-Key: $API_KEY"

# Get review comments
curl -s "http://localhost:8080/api/v1/pull-requests/{pullRequestId}/reviews/{reviewId}/comments" \
  -H "X-API-Key: $API_KEY"
```

Findings are also posted back to GitHub as a PR review when SCM token permissions allow.

## API Reference (summary)

| Method | Endpoint | Auth |
|--------|----------|------|
| POST | `/api/v1/organizations` | Open |
| POST | `/api/v1/organizations/{id}/api-keys` | Open |
| POST | `/api/v1/organizations/{id}/repositories` | API Key |
| POST | `/api/v1/repositories/index` | API Key |
| GET | `/api/v1/repositories/{id}/index-status` | API Key |
| POST | `/api/v1/search/semantic` | API Key |
| POST | `/api/v1/pull-requests/{id}/reviews` | API Key |
| GET | `/api/v1/pull-requests/{id}/reviews/{reviewId}/comments` | API Key |
| POST | `/api/v1/webhooks/github/{orgId}` | HMAC signature |

## Project Structure

```
codesage/                    ← Gradle application
├── src/main/java/.../
│   ├── platform/            Gateway, auth, webhooks
│   ├── scm/                 SCM abstractions + GitHub
│   ├── indexing/            Clone, AST chunking, embeddings
│   ├── rag/                 Vector search, context retrieval
│   ├── review/              AI PR review engine
│   └── llm/                 OpenAI clients
├── docker-compose.yml
└── README.md                (detailed app README)

docs/
├── architecture.md
├── database-schema.md
├── kafka-events.md
└── folder-structure.md
```

## CI/CD

GitHub Actions runs on every push/PR to `main`:

- Compile + build JAR
- Spring context smoke test against Postgres (pgvector), Redis, and Kafka service containers

See [.github/workflows/ci.yml](.github/workflows/ci.yml).

## Production Notes

This is a portfolio-grade architecture demo. Before production:

- Encrypt SCM tokens (Vault/KMS), never store plain text
- Add integration tests and observability (Micrometer, tracing)
- Harden bootstrap endpoints (org/API key creation)
- Configure Kafka retry/DLQ consumers in production

## License

MIT (add a LICENSE file if you open-source this repo)

## Author

**Your Name** — [GitHub](https://github.com/YOUR_GITHUB_USERNAME) · [LinkedIn](https://linkedin.com/in/YOUR_PROFILE)

> Replace `YOUR_GITHUB_USERNAME` and profile links before publishing.
