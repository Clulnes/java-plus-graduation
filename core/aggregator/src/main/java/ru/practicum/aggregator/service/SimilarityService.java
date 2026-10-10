package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarityService {

    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    @Value("${topics.events-similarity:stats.events-similarity.v1}")
    private String eventsSimilarityTopic;

    private final Map<Long, Map<Long, Double>> eventUserWeights = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventWeightSums = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();
    private final Set<Long> allEvents = ConcurrentHashMap.newKeySet();

    public void processUserAction(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();
        double newActionWeight = getWeight(action.getActionType());
        Instant timestamp = action.getTimestamp();

        allEvents.add(eventId);

        Map<Long, Double> userWeights = eventUserWeights.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        double oldWeight = userWeights.getOrDefault(userId, 0.0);

        if (newActionWeight <= oldWeight) {
            return;
        }

        double newWeight = newActionWeight;
        userWeights.put(userId, newWeight);

        double deltaWeight = newWeight - oldWeight;
        double newEventWeightSum = eventWeightSums.merge(eventId, deltaWeight, Double::sum);

        for (Long otherEventId : allEvents) {
            if (otherEventId.equals(eventId)) {
                continue;
            }

            double otherEventWeightSum = eventWeightSums.getOrDefault(otherEventId, 0.0);
            if (otherEventWeightSum <= 0) {
                continue;
            }

            Map<Long, Double> otherUserWeights = eventUserWeights.getOrDefault(otherEventId, Collections.emptyMap());
            double userWeightOnOther = otherUserWeights.getOrDefault(userId, 0.0);
            double oldMin = Math.min(oldWeight, userWeightOnOther);
            double newMin = Math.min(newWeight, userWeightOnOther);
            double deltaMin = newMin - oldMin;
            long first = Math.min(eventId, otherEventId);
            long second = Math.max(eventId, otherEventId);

            Map<Long, Double> pairMap = minWeightsSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>());
            double currentMinSum = pairMap.getOrDefault(second, 0.0);
            double updatedMinSum = currentMinSum + deltaMin;
            pairMap.put(second, updatedMinSum);

            if (updatedMinSum > 0) {
                double denominator = Math.sqrt(newEventWeightSum) * Math.sqrt(otherEventWeightSum);
                double similarity = denominator > 0 ? (updatedMinSum / denominator) : 0.0;

                EventSimilarityAvro similarityAvro = EventSimilarityAvro.newBuilder()
                        .setEventA(first)
                        .setEventB(second)
                        .setScore(similarity)
                        .setTimestamp(timestamp)
                        .build();

                String messageKey = first + ":" + second;
                kafkaTemplate.send(eventsSimilarityTopic, messageKey, similarityAvro);

                log.info("Calculated similarity for events ({}, {}): score={}", first, second, similarity);
            }
        }
    }

    private double getWeight(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}