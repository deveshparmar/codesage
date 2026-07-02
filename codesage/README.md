# CodeSage Application

Spring Boot modular monolith — see the [root README](../README.md) for overview, demo flow, and resume context.

## Run locally

```bash
# From this directory (codesage/)
docker compose up -d
export OPENAI_API_KEY=sk-your-key
./gradlew bootRun
```

## Build

```bash
./gradlew build          # full build + tests
./gradlew bootJar        # produces build/libs/codesage-0.0.1-SNAPSHOT.jar
```

## Docker image

```bash
./gradlew bootJar
docker build -t codesage:latest .
docker run -p 8080:8080 \
  -e OPENAI_API_KEY=sk-... \
  -e DB_HOST=host.docker.internal \
  -e REDIS_HOST=host.docker.internal \
  -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  codesage:latest
```

## Configuration

All settings in `src/main/resources/application.yml` under `codesage.*`.

| Variable | Default | Description |
|----------|---------|-------------|
| `OPENAI_API_KEY` | — | Required for indexing embeddings + PR reviews |
| `DB_HOST` | localhost | PostgreSQL host |
| `REDIS_HOST` | localhost | Redis host |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka brokers |
| `INDEXING_WORKSPACE` | /tmp/codesage/repos | Local git clone cache |
| `SERVER_PORT` | 8080 | HTTP port |

## Documentation

- [Architecture](../docs/architecture.md)
- [Database schema](../docs/database-schema.md)
- [Kafka events](../docs/kafka-events.md)
