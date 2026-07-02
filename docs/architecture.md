# CodeSage System Architecture

## Overview

CodeSage is a **modular monolith** — a single deployable Spring Boot application organized into independent feature modules with clear boundaries. Each module follows Clean Architecture (domain → application → infrastructure → api) and communicates via well-defined interfaces and Kafka events.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CodeSage Modular Monolith                          │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────────┐  │
│  │ Platform Gateway │  │  SCM Integration │  │   Platform Services      │  │
│  │  REST / Webhooks │  │  Provider Abstr. │  │  Audit / Rate Limit /    │  │
│  │  Auth / Events   │  │  GitHub Adapter  │  │  Redis Cache             │  │
│  └────────┬─────────┘  └────────┬─────────┘  └────────────┬─────────────┘  │
│           │                     │                          │                 │
│           └─────────────────────┼──────────────────────────┘                 │
│                                 ▼                                            │
│                        ┌─────────────────┐                                     │
│                        │  Apache Kafka   │                                     │
│                        └────────┬────────┘                                     │
│           ┌─────────────────────┼─────────────────────┐                       │
│           ▼                     ▼                     ▼                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │ Repo Indexing   │  │ AI Review Engine│  │  RAG / LLM      │             │
│  │ (Phase 2)       │  │ (Phase 3)       │  │  (Phase 2–3)    │             │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘             │
│           └─────────────────────┼─────────────────────┘                       │
│                                 ▼                                            │
│              ┌──────────────────────────────────────┐                        │
│              │  PostgreSQL + pgvector  │  Redis     │                        │
│              └──────────────────────────────────────┘                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Module Definitions

| Module | Package | Responsibility | Phase |
|--------|---------|----------------|-------|
| **Platform Gateway** | `platform` | REST APIs, webhook ingestion, authentication, webhook verification, Kafka event publishing | 1 |
| **SCM Integration** | `scm` | Provider-agnostic SCM abstractions; GitHub adapter | 1 |
| **Platform Services** | `platform` (services) | Audit logging, usage analytics, rate limiting, Redis caching | 1 |
| **Repository Indexing** | `indexing` | Clone, parse AST, semantic chunk, embed, store vectors | 2 |
| **RAG Pipeline** | `rag` | Vector search, metadata filtering, context retrieval | 2 |
| **AI Review Engine** | `review` | Diff parsing, method location, prompt building, LLM invocation | 3 |
| **LLM Integration** | `llm` | OpenAI client, embedding generation, GPT review calls | 3 |
| **Audit** | `audit` | Immutable audit trail, compliance logging | 1 (foundation) |
| **Common** | `common` | Shared domain types, exceptions, utilities | 1 |

## Design Principles

### Provider Agnosticism (SCM)

Business logic never imports GitHub-specific types. All SCM operations flow through:

- `ScmProvider` — factory entry point per provider
- `RepositoryProvider` — repository metadata and listing
- `PullRequestProvider` — PR data and diffs
- `ReviewPublisher` — post review comments back to SCM
- `WebhookVerifier` — validate incoming webhook signatures

New providers (GitLab, Bitbucket, Azure DevOps) are added by implementing these interfaces and registering in `ScmProviderRegistry`.

### Event-Driven Internal Communication

Modules communicate asynchronously via Kafka. Synchronous calls are limited to:

- Gateway → SCM adapters (external API calls)
- Gateway → PostgreSQL (persistence)
- Future: Review Engine → RAG (in-process, same JVM)

### Extensibility for Future Features

| Future Feature | Extension Point |
|----------------|-----------------|
| Repository Q&A | RAG similarity search + LLM Q&A prompt |
| Semantic Code Search | pgvector similarity + metadata filters |
| Security Analysis | New review strategy plugged into Review Engine |
| Bug Detection | Additional LLM prompt template + finding ranker |
| Duplicate Code Detection | Chunk hash comparison + vector clustering |
| Architecture Analysis | Class-level chunk aggregation + graph analysis |

No module redesign required — each feature adds an application service and reuses indexing/RAG infrastructure.

## Pull Request Review Flow (End-to-End)

```
GitHub Webhook (pull_request.opened/synchronize)
        │
        ▼
Platform Gateway: verify signature → deduplicate (Redis) → persist PR
        │
        ▼
Kafka: review.requested
        │
        ▼
AI Review Engine: fetch diff via PullRequestProvider
        │
        ▼
Parse diff → locate modified methods (JavaParser AST)
        │
        ▼
RAG: retrieve related context from pgvector
        │
        ▼
LLM: build prompt → GPT-4.1 → parse findings
        │
        ▼
Rank + deduplicate findings
        │
        ▼
ReviewPublisher: post comments to GitHub
        │
        ▼
Kafka: review.completed + audit event
```

## Technology Mapping

| Concern | Technology |
|---------|------------|
| API | Spring Web MVC |
| Persistence | Spring Data JPA + Flyway |
| Vectors | pgvector (PostgreSQL extension) |
| Cache / Locks | Spring Data Redis |
| Messaging | Spring Kafka |
| External SCM | Spring RestClient |
| Security | Spring Security (API key + webhook HMAC) |

## Deployment

Single Docker image running the modular monolith. Infrastructure services (PostgreSQL, Redis, Kafka) run via Docker Compose for local development.

## Phase 1 Deliverables (Current)

- [x] Architecture and schema design
- [x] Gradle project with all dependencies
- [x] Platform Gateway (REST, webhooks, auth, Kafka publishing)
- [x] SCM abstraction layer with GitHub implementation

## Phase 2 Deliverables (Current)

- [x] Repository Indexing (clone, AST chunking, incremental indexing)
- [x] OpenAI embedding generation with Redis cache
- [x] pgvector storage and HNSW similarity search
- [x] RAG pipeline (context retrieval, semantic search API)
## Phase 3 Deliverables (Current)

- [x] AI Review Engine (diff parsing, AST method location, RAG context)
- [x] GPT-4.1 code review via OpenAI Chat Completions
- [x] Finding ranking and deduplication
- [x] SCM review comment publishing
- [x] Kafka review consumer and completion events
