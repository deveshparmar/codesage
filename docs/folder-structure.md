# CodeSage Folder Structure

```
codesage/
├── build.gradle
├── settings.gradle
├── Dockerfile
├── docker-compose.yml
├── docs/
│   ├── architecture.md
│   ├── database-schema.md
│   ├── kafka-events.md
│   └── folder-structure.md
└── src/
    ├── main/
    │   ├── java/com/deveshparmar/codesage/
    │   │   ├── CodesageApplication.java
    │   │   │
    │   │   ├── common/
    │   │   │   ├── domain/           # Shared value objects, enums
    │   │   │   ├── exception/        # Domain exceptions, error responses
    │   │   │   └── config/           # Cross-cutting configuration
    │   │   │
    │   │   ├── platform/
    │   │   │   ├── api/              # REST controllers, DTOs, mappers
    │   │   │   ├── application/      # Use cases, services
    │   │   │   ├── domain/           # Platform entities, repositories (ports)
    │   │   │   ├── infrastructure/   # JPA entities, Kafka producers, Redis
    │   │   │   └── config/           # Security, Kafka, Redis config
    │   │   │
    │   │   ├── scm/
    │   │   │   ├── domain/           # Provider-agnostic SCM interfaces & models
    │   │   │   ├── application/      # ScmProviderRegistry, factory
    │   │   │   ├── infrastructure/
    │   │   │   │   └── github/       # GitHub adapter implementation
    │   │   │   └── config/           # SCM provider configuration
    │   │   │
    │   │   ├── audit/                # Audit event handling (Phase 1 foundation)
    │   │   │   ├── domain/
    │   │   │   ├── application/
    │   │   │   └── infrastructure/
    │   │   │
    │   │   ├── indexing/             # Repository indexing (Phase 2)
    │   │   │   ├── domain/
    │   │   │   ├── application/
    │   │   │   ├── infrastructure/
    │   │   │   └── config/
    │   │   │
    │   │   ├── rag/                  # RAG pipeline (Phase 2)
    │   │   │   ├── domain/
    │   │   │   ├── application/
    │   │   │   └── infrastructure/
    │   │   │
    │   │   ├── review/               # AI Review Engine (Phase 3)
    │   │   │   ├── domain/
    │   │   │   ├── application/
    │   │   │   └── infrastructure/
    │   │   │
    │   │   └── llm/                  # LLM integration (Phase 3)
    │   │       ├── domain/
    │   │       ├── application/
    │   │       └── infrastructure/
    │   │
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/         # Flyway SQL migrations
    │
    └── test/                         # Tests (future phases)
```

## Module Layer Convention

Each module follows the same internal structure:

```
<module>/
├── api/              # Inbound adapters (controllers, listeners) — Platform only
├── application/      # Use cases, orchestration, port implementations
├── domain/           # Entities, value objects, repository interfaces (ports)
├── infrastructure/   # Outbound adapters (JPA, HTTP clients, Kafka)
└── config/           # Module-specific Spring configuration
```

## Dependency Rules

```
api → application → domain ← infrastructure
                         ↑
                    common (shared types)
```

- **domain** has zero dependencies on other modules' infrastructure
- **application** depends on domain ports and SCM domain interfaces
- **infrastructure** implements domain ports
- **scm.domain** is consumed by platform, review, and indexing — never the reverse
