# CodeSage Kafka Events

## Topic Naming Convention

```
codesage.<domain>.<action>
codesage.<domain>.<action>.retry
codesage.<domain>.<action>.dlq
```

## Topics

| Topic | Producer | Consumer | Description |
|-------|----------|----------|-------------|
| `codesage.repository.index.requested` | Platform Gateway | Indexing Module | Trigger repository indexing |
| `codesage.repository.index.completed` | Indexing Module | Platform Gateway | Indexing finished notification |
| `codesage.embedding.generation.requested` | Indexing Module | Indexing Module | Generate embeddings for chunks |
| `codesage.review.requested` | Platform Gateway | Review Engine | Trigger AI PR review |
| `codesage.review.completed` | Review Engine | Platform Gateway, Audit | Review finished |
| `codesage.audit.events` | All Modules | Audit Module | Immutable audit trail |

## Event Envelope

All events share a common envelope for idempotency and tracing:

```json
{
  "eventId": "uuid",
  "eventType": "review.requested",
  "timestamp": "2026-06-28T10:00:00Z",
  "correlationId": "uuid",
  "organizationId": "uuid",
  "payload": { }
}
```

## Event Definitions

### RepositoryIndexRequestedEvent

**Topic:** `codesage.repository.index.requested`

```json
{
  "repositoryId": "uuid",
  "branchName": "main",
  "commitSha": "abc123",
  "fullReindex": false
}
```

### RepositoryIndexCompletedEvent

**Topic:** `codesage.repository.index.completed`

```json
{
  "repositoryId": "uuid",
  "branchName": "main",
  "commitSha": "abc123",
  "filesIndexed": 142,
  "chunksCreated": 890,
  "durationMs": 45000,
  "status": "SUCCESS"
}
```

### EmbeddingGenerationRequestedEvent

**Topic:** `codesage.embedding.generation.requested`

```json
{
  "repositoryId": "uuid",
  "chunkIds": ["uuid1", "uuid2"],
  "model": "text-embedding-3-large"
}
```

### ReviewRequestedEvent

**Topic:** `codesage.review.requested`

```json
{
  "reviewId": "uuid",
  "pullRequestId": "uuid",
  "repositoryId": "uuid",
  "pullRequestNumber": 42,
  "headSha": "abc123",
  "baseSha": "def456",
  "triggerSource": "WEBHOOK"
}
```

### ReviewCompletedEvent

**Topic:** `codesage.review.completed`

```json
{
  "reviewId": "uuid",
  "pullRequestId": "uuid",
  "repositoryId": "uuid",
  "status": "COMPLETED",
  "findingsCount": 5,
  "commentsPosted": 5,
  "durationMs": 12000
}
```

### AuditEvent

**Topic:** `codesage.audit.events`

```json
{
  "eventType": "WEBHOOK_RECEIVED",
  "actor": "github-webhook",
  "resourceType": "PULL_REQUEST",
  "resourceId": "12345",
  "metadata": {
    "action": "opened",
    "repository": "org/repo"
  },
  "ipAddress": "140.82.112.0"
}
```

## Consumer Guarantees

- **Idempotency:** Consumers track processed `eventId` in Redis with TTL
- **Retry:** Failed messages routed to `.retry` topic with exponential backoff
- **DLQ:** Messages exceeding max retries routed to `.dlq` topic
- **Partition Key:** `organizationId` for tenant isolation

## Configuration

| Setting | Value |
|---------|-------|
| Partitions | 6 (per topic) |
| Replication Factor | 1 (local dev) |
| Retention | 7 days |
| Max Retries | 3 |
| Retry Backoff | 1s, 5s, 30s |
