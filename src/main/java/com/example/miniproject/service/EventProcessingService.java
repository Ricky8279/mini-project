package com.example.miniproject.service;

import com.example.miniproject.model.SessionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessingService {

    private final KafkaTemplate<String, SessionEvent> kafkaTemplate;
    private final StorageService storageService;
    private static final String TOPIC = "session-events";
    private static final String DLQ_TOPIC = "session-events-dlq";

    public void publishEvent(SessionEvent event) {
        try{
            kafkaTemplate.send(TOPIC, event.getContentId(), event);
            log.info("Published event for session: {}", event.getContentId());
        }
        catch (Exception e){
            log.error("Error publishing event {}",e.getMessage());
            kafkaTemplate.send(DLQ_TOPIC, event.getContentId(), event);
        }
    }

    @KafkaListener(topics = TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void processEvent(SessionEvent event){
        try{
            validateEvent(event);
            storageService.store(event);
            log.info("Processed event for session: {}", event.getContentId());
        }
        catch(Exception e){
            log.error("Error processing event {}",e.getMessage());
            kafkaTemplate.send(DLQ_TOPIC, event.getContentId(), event);
        }
    }

    private void validateEvent(SessionEvent event){
        if (event.getSessionId() == null || event.getEventType() == null){
            throw new IllegalArgumentException("Invalid event data");
        }
    }

}
