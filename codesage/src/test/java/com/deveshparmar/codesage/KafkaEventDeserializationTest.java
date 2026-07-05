package com.deveshparmar.codesage;

import com.deveshparmar.codesage.platform.infrastructure.kafka.EventEnvelope;
import com.deveshparmar.codesage.platform.infrastructure.kafka.KafkaEventJsonReader;
import com.deveshparmar.codesage.platform.infrastructure.kafka.RepositoryIndexRequestedPayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest
class KafkaEventDeserializationTest {

    private static final String JSON = """
            {"eventId":"ce2f0e9d-33fd-4834-bc54-c4033d4b131f","eventType":"repository.index.requested","timestamp":1783248885.621577400,"correlationId":"4d0b8390-167c-4e49-b5c4-b457d2433de7","organizationId":"650f344a-9f2a-4dcc-bace-59bc27ae27f6","payload":{"repositoryId":"adaaea37-d7b8-4efa-a94a-e789ac54a09f","branchName":"master","commitSha":null,"fullReindex":true}}
            """;

    @Autowired
    private KafkaEventJsonReader kafkaEventJsonReader;

    @Test
    void readerDeserializesRepositoryIndexPayload() {
        EventEnvelope<RepositoryIndexRequestedPayload> envelope =
                kafkaEventJsonReader.read(JSON, RepositoryIndexRequestedPayload.class);
        assertInstanceOf(RepositoryIndexRequestedPayload.class, envelope.payload());
        assertEquals(UUID.fromString("adaaea37-d7b8-4efa-a94a-e789ac54a09f"), envelope.payload().repositoryId());
    }
}
