package com.deveshparmar.codesage.platform.infrastructure.kafka;

import com.deveshparmar.codesage.common.exception.CodeSageException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaEventJsonReader {

    private final ObjectMapper objectMapper;

    public <T> EventEnvelope<T> read(String json, Class<T> payloadType) {
        try {
            return objectMapper.readValue(json, new TypeReference<EventEnvelope<T>>() {
                @Override
                public java.lang.reflect.Type getType() {
                    return objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, payloadType);
                }
            });
        } catch (Exception ex) {
            throw new CodeSageException("Failed to deserialize Kafka event payload type " + payloadType.getSimpleName(), ex);
        }
    }
}
