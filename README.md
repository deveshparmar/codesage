# CodeSage

**AI-powered code intelligence platform** for automated pull request reviews, semantic code search, and repository understanding.

Built as a production-style **modular monolith** (Java 21, Spring Boot 3) — designed to demonstrate SDE-2 backend engineering: system design, event-driven architecture, RAG, and provider-agnostic integrations.

[![CI](https://github.com/YOUR_GITHUB_USERNAME/codesage/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_GITHUB_USERNAME/codesage/actions/workflows/ci.yml)

## Highlights

- **Provider-agnostic SCM layer** - GitHub today; GitLab/Bitbucket/Azure DevOps via interfaces (`ScmProvider`, `ReviewPublisher`, …)
- **AST-based semantic indexing** - JavaParser chunks (class/method/constructor), not fixed token splits
- **RAG pipeline** - pgvector HNSW similarity search with metadata filtering
- **AI PR review** - diff → modified method location → RAG context → GPT-4.1 → ranked findings → GitHub comments
- **Event-driven** - Kafka for indexing, embeddings, reviews; Redis for locks, dedup, rate limits

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
