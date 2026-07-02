# CodeSage Database Schema

PostgreSQL 16 with `pgvector` extension. All primary keys are UUID v7-compatible (generated via `gen_random_uuid()`). Timestamps are `TIMESTAMPTZ`.

## Entity Relationship Diagram

```
organizations ──┬── repositories ──┬── branches ── files ── chunks ── embeddings
                │                  │
                │                  └── pull_requests ── reviews ── review_comments
                │
                ├── api_keys
                ├── audit_logs
                └── api_usage
```

## Tables

### organizations

Multi-tenant root entity.

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| name | VARCHAR(255) | NOT NULL |
| slug | VARCHAR(100) | NOT NULL, UNIQUE |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

### api_keys

Authentication for REST API access.

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| organization_id | UUID | FK → organizations |
| key_hash | VARCHAR(64) | NOT NULL, UNIQUE |
| name | VARCHAR(255) | NOT NULL |
| prefix | VARCHAR(8) | NOT NULL (display prefix) |
| is_active | BOOLEAN | NOT NULL DEFAULT true |
| expires_at | TIMESTAMPTZ | NULL |
| created_at | TIMESTAMPTZ | NOT NULL |

### repositories

Registered SCM repositories.

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| organization_id | UUID | FK → organizations |
| scm_provider | VARCHAR(32) | NOT NULL |
| external_id | VARCHAR(64) | NOT NULL |
| name | VARCHAR(255) | NOT NULL |
| full_name | VARCHAR(512) | NOT NULL |
| clone_url | TEXT | NOT NULL |
| default_branch | VARCHAR(255) | NOT NULL |
| is_private | BOOLEAN | NOT NULL |
| indexing_status | VARCHAR(32) | NOT NULL |
| webhook_secret | VARCHAR(255) | NULL |
| installation_id | VARCHAR(64) | NULL |
| last_indexed_at | TIMESTAMPTZ | NULL |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Unique:** `(organization_id, scm_provider, external_id)`

### branches

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| repository_id | UUID | FK → repositories |
| name | VARCHAR(255) | NOT NULL |
| head_commit_sha | VARCHAR(40) | NOT NULL |
| is_default | BOOLEAN | NOT NULL |
| last_indexed_at | TIMESTAMPTZ | NULL |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Unique:** `(repository_id, name)`

### files

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| branch_id | UUID | FK → branches |
| path | TEXT | NOT NULL |
| language | VARCHAR(32) | NOT NULL |
| content_hash | VARCHAR(64) | NOT NULL |
| last_commit_sha | VARCHAR(40) | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Unique:** `(branch_id, path, last_commit_sha)`

### chunks

AST-based semantic code chunks.

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| file_id | UUID | FK → files |
| chunk_type | VARCHAR(32) | NOT NULL |
| chunk_hash | VARCHAR(64) | NOT NULL |
| package_name | VARCHAR(512) | NULL |
| class_name | VARCHAR(255) | NULL |
| method_name | VARCHAR(255) | NULL |
| start_line | INTEGER | NOT NULL |
| end_line | INTEGER | NOT NULL |
| content | TEXT | NOT NULL |
| metadata | JSONB | NOT NULL DEFAULT '{}' |
| created_at | TIMESTAMPTZ | NOT NULL |

**Unique:** `(file_id, chunk_hash)`

### embeddings

Vector embeddings stored via pgvector.

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| chunk_id | UUID | FK → chunks, UNIQUE |
| embedding | vector(3072) | NOT NULL |
| model | VARCHAR(64) | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL |

**Index:** HNSW on `embedding` using `vector_cosine_ops`

### pull_requests

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| repository_id | UUID | FK → repositories |
| external_id | VARCHAR(64) | NOT NULL |
| number | INTEGER | NOT NULL |
| title | TEXT | NOT NULL |
| source_branch | VARCHAR(255) | NOT NULL |
| target_branch | VARCHAR(255) | NOT NULL |
| head_sha | VARCHAR(40) | NOT NULL |
| base_sha | VARCHAR(40) | NOT NULL |
| state | VARCHAR(32) | NOT NULL |
| author | VARCHAR(255) | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Unique:** `(repository_id, external_id)`

### reviews

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| pull_request_id | UUID | FK → pull_requests |
| status | VARCHAR(32) | NOT NULL |
| summary | TEXT | NULL |
| llm_model | VARCHAR(64) | NULL |
| prompt_tokens | INTEGER | NULL |
| completion_tokens | INTEGER | NULL |
| error_message | TEXT | NULL |
| started_at | TIMESTAMPTZ | NULL |
| completed_at | TIMESTAMPTZ | NULL |
| created_at | TIMESTAMPTZ | NOT NULL |

### review_comments

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| review_id | UUID | FK → reviews |
| file_path | TEXT | NOT NULL |
| start_line | INTEGER | NOT NULL |
| end_line | INTEGER | NOT NULL |
| severity | VARCHAR(16) | NOT NULL |
| category | VARCHAR(64) | NOT NULL |
| message | TEXT | NOT NULL |
| suggestion | TEXT | NULL |
| external_comment_id | VARCHAR(64) | NULL |
| created_at | TIMESTAMPTZ | NOT NULL |

### audit_logs

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| organization_id | UUID | FK → organizations |
| event_type | VARCHAR(64) | NOT NULL |
| actor | VARCHAR(255) | NOT NULL |
| resource_type | VARCHAR(64) | NOT NULL |
| resource_id | VARCHAR(64) | NOT NULL |
| metadata | JSONB | NOT NULL DEFAULT '{}' |
| ip_address | VARCHAR(45) | NULL |
| created_at | TIMESTAMPTZ | NOT NULL |

### api_usage

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| organization_id | UUID | FK → organizations |
| api_key_id | UUID | FK → api_keys, NULL |
| endpoint | VARCHAR(512) | NOT NULL |
| http_method | VARCHAR(8) | NOT NULL |
| status_code | INTEGER | NOT NULL |
| latency_ms | INTEGER | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL |

## Indexes

- `idx_repositories_org` on `repositories(organization_id)`
- `idx_pull_requests_repo` on `pull_requests(repository_id)`
- `idx_chunks_file` on `chunks(file_id)`
- `idx_audit_logs_org_created` on `audit_logs(organization_id, created_at DESC)`
- `idx_embeddings_hnsw` HNSW on `embeddings(embedding vector_cosine_ops)`
