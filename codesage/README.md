# CodeSage Application

Spring Boot modular monolith. See the [root README](../README.md) for overview, demo flow, and resume context.

## Run locally

```bash
# From this directory (codesage/)
docker compose up -d
export OPENAI_API_KEY=sk-your-key
./gradlew bootRun
```

### Run locally with free Ollama models

Install Ollama, then pull one embedding model and one chat model:

```powershell
ollama pull nomic-embed-text
ollama pull qwen2.5-coder:1.5b
```

Start Postgres, Redis, and Kafka:

```powershell
docker compose up -d
```

Run the app with the `ollama` Spring profile:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=ollama"
```

For a stronger local reviewer, pull a bigger code model and override the default:

```powershell
ollama pull qwen2.5-coder:7b
$env:OLLAMA_CHAT_MODEL = "qwen2.5-coder:7b"
.\gradlew.bat bootRun --args="--spring.profiles.active=ollama"
```

Embedding-only free-tier profiles are also included:

| Profile | Provider | Model | Dimensions | Key |
|---------|----------|-------|------------|-----|
| `ollama` | Ollama local | `nomic-embed-text` | 768 | none |
| `gemini` | Google Gemini | `text-embedding-004` | 768 | `GEMINI_API_KEY` |
| `huggingface` | Hugging Face | `BAAI/bge-small-en-v1.5` | 384 | `HUGGINGFACE_API_KEY` |

Use one profile at a time, because the pgvector column dimensions are set by the active profile during Flyway migration.

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
| `OPENAI_API_KEY` | - | Required for paid OpenAI embeddings + PR reviews |
| `OLLAMA_CHAT_MODEL` | qwen2.5-coder:1.5b | Optional chat model override when using the `ollama` profile |
| `SPRING_PROFILES_ACTIVE` | - | Use `ollama`, `gemini`, or `huggingface` for alternate embedding profiles |
| `EMBEDDING_MODEL` | `text-embedding-3-large` | Embedding model name for the default profile |
| `EMBEDDING_DIMENSIONS` | `3072` | Vector dimensions; must match the selected embedding model |
| `DB_HOST` | localhost | PostgreSQL host |
| `REDIS_HOST` | localhost | Redis host |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka brokers |
| `INDEXING_WORKSPACE` | /tmp/codesage/repos | Local git clone cache |
| `SERVER_PORT` | 8080 | HTTP port |
