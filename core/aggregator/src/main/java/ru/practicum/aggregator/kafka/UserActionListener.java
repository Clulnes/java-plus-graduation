package ru.practicum.aggregator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.aggregator.service.SimilarityService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionListener {

    private final SimilarityService similarityService;

    @KafkaListener(topics = "${topics.user-actions:stats.user-actions.v1}")
    public void listen(ConsumerRecord<String, UserActionAvro> record) {
        log.info("Aggregator received action from Kafka: userId={}, eventId={}",
                record.value().getUserId(), record.value().getEventId());
        similarityService.processUserAction(record.value());
    }
}