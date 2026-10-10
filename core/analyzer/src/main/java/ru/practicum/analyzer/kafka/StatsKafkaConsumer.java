package ru.practicum.analyzer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsKafkaConsumer {

    private final RecommendationService recommendationService;

    @KafkaListener(topics = "${topics.user-actions:stats.user-actions.v1}",
            containerFactory = "userActionListenerFactory")
    public void listenUserAction(ConsumerRecord<String, UserActionAvro> record) {
        log.info("Analyzer received action from Kafka: userId={}, eventId={}",
                record.value().getUserId(), record.value().getEventId());
        recommendationService.saveUserAction(record.value());
    }

    @KafkaListener(topics = "${topics.events-similarity:stats.events-similarity.v1}",
            containerFactory = "similarityListenerFactory")
    public void listenEventSimilarity(ConsumerRecord<String, EventSimilarityAvro> record) {
        log.info("Analyzer received similarity from Kafka: eventA={}, eventB={}, score={}",
                record.value().getEventA(), record.value().getEventB(), record.value().getScore());
        recommendationService.saveEventSimilarity(record.value());
    }
}