CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE organizations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE api_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID         NOT NULL REFERENCES organizations (id),
    key_hash        VARCHAR(64)  NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    prefix          VARCHAR(8)   NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_api_keys_org ON api_keys (organization_id);

CREATE TABLE repositories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID         NOT NULL REFERENCES organizations (id),
    scm_provider    VARCHAR(32)  NOT NULL,
    external_id     VARCHAR(64)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    full_name       VARCHAR(512) NOT NULL,
    clone_url       TEXT         NOT NULL,
    default_branch  VARCHAR(255) NOT NULL,
    is_private      BOOLEAN      NOT NULL DEFAULT FALSE,
    indexing_status VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    webhook_secret  VARCHAR(255),
    installation_id VARCHAR(64),
    last_indexed_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_repositories_org_provider_external UNIQUE (organization_id, scm_provider, external_id)
);

CREATE INDEX idx_repositories_org ON repositories (organization_id);

CREATE TABLE branches (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id   UUID         NOT NULL REFERENCES repositories (id),
    name            VARCHAR(255) NOT NULL,
    head_commit_sha VARCHAR(40)  NOT NULL,
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    last_indexed_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_branches_repo_name UNIQUE (repository_id, name)
);

CREATE TABLE files (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id       UUID        NOT NULL REFERENCES branches (id),
    path            TEXT        NOT NULL,
    language        VARCHAR(32) NOT NULL,
    content_hash    VARCHAR(64) NOT NULL,
    last_commit_sha VARCHAR(40) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_files_branch_path_commit UNIQUE (branch_id, path, last_commit_sha)
);

CREATE TABLE chunks (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id      UUID         NOT NULL REFERENCES files (id),
    chunk_type   VARCHAR(32)  NOT NULL,
    chunk_hash   VARCHAR(64)  NOT NULL,
    package_name VARCHAR(512),
    class_name   VARCHAR(255),
    method_name  VARCHAR(255),
    start_line   INTEGER      NOT NULL,
    end_line     INTEGER      NOT NULL,
    content      TEXT         NOT NULL,
    metadata     JSONB        NOT NULL DEFAULT '{}',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_chunks_file_hash UNIQUE (file_id, chunk_hash)
);

CREATE INDEX idx_chunks_file ON chunks (file_id);

CREATE TABLE embeddings (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chunk_id  UUID         NOT NULL UNIQUE REFERENCES chunks (id),
    embedding vector(3072) NOT NULL,
    model     VARCHAR(64)  NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_embeddings_hnsw ON embeddings USING hnsw (embedding vector_cosine_ops);

CREATE TABLE pull_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id   UUID         NOT NULL REFERENCES repositories (id),
    external_id     VARCHAR(64)  NOT NULL,
    number          INTEGER      NOT NULL,
    title           TEXT         NOT NULL,
    source_branch   VARCHAR(255) NOT NULL,
    target_branch   VARCHAR(255) NOT NULL,
    head_sha        VARCHAR(40)  NOT NULL,
    base_sha        VARCHAR(40)  NOT NULL,
    state           VARCHAR(32)  NOT NULL,
    author          VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_pull_requests_repo_external UNIQUE (repository_id, external_id)
);

CREATE INDEX idx_pull_requests_repo ON pull_requests (repository_id);

CREATE TABLE reviews (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pull_request_id   UUID         NOT NULL REFERENCES pull_requests (id),
    status            VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    summary           TEXT,
    llm_model         VARCHAR(64),
    prompt_tokens     INTEGER,
    completion_tokens INTEGER,
    error_message     TEXT,
    started_at        TIMESTAMPTZ,
    completed_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE review_comments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id           UUID         NOT NULL REFERENCES reviews (id),
    file_path           TEXT         NOT NULL,
    start_line          INTEGER      NOT NULL,
    end_line            INTEGER      NOT NULL,
    severity            VARCHAR(16)  NOT NULL,
    category            VARCHAR(64)  NOT NULL,
    message             TEXT         NOT NULL,
    suggestion          TEXT,
    external_comment_id VARCHAR(64),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID         NOT NULL REFERENCES organizations (id),
    event_type      VARCHAR(64)  NOT NULL,
    actor           VARCHAR(255) NOT NULL,
    resource_type   VARCHAR(64)  NOT NULL,
    resource_id     VARCHAR(64)  NOT NULL,
    metadata        JSONB        NOT NULL DEFAULT '{}',
    ip_address      VARCHAR(45),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_org_created ON audit_logs (organization_id, created_at DESC);

CREATE TABLE api_usage (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID         NOT NULL REFERENCES organizations (id),
    api_key_id      UUID REFERENCES api_keys (id),
    endpoint        VARCHAR(512) NOT NULL,
    http_method     VARCHAR(8)   NOT NULL,
    status_code     INTEGER      NOT NULL,
    latency_ms      INTEGER      NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_api_usage_org_created ON api_usage (organization_id, created_at DESC);
