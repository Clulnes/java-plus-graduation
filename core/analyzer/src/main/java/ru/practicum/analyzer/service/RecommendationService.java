package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.dao.EventSimilarityRepository;
import ru.practicum.analyzer.dao.UserEventInteractionRepository;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserEventInteraction;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final EventSimilarityRepository similarityRepository;
    private final UserEventInteractionRepository interactionRepository;

    @Transactional
    public void saveUserAction(UserActionAvro action) {
        double newWeight = switch (action.getActionType()) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };

        Optional<UserEventInteraction> existingOpt = interactionRepository.findByUserIdAndEventId(action.getUserId(), action.getEventId());
        if (existingOpt.isPresent()) {
            UserEventInteraction existing = existingOpt.get();
            if (newWeight > existing.getWeight()) {
                existing.setWeight(newWeight);
                existing.setTimestamp(action.getTimestamp());
                interactionRepository.save(existing);
            }
        } else {
            interactionRepository.save(UserEventInteraction.builder()
                    .userId(action.getUserId())
                    .eventId(action.getEventId())
                    .weight(newWeight)
                    .timestamp(action.getTimestamp())
                    .build());
        }
    }

    @Transactional
    public void saveEventSimilarity(EventSimilarityAvro similarity) {
        long a = Math.min(similarity.getEventA(), similarity.getEventB());
        long b = Math.max(similarity.getEventA(), similarity.getEventB());

        Optional<EventSimilarity> existingOpt = similarityRepository.findByEventAAndEventB(a, b);
        if (existingOpt.isPresent()) {
            EventSimilarity existing = existingOpt.get();
            existing.setScore(similarity.getScore());
            existing.setTimestamp(similarity.getTimestamp());
            similarityRepository.save(existing);
        } else {
            similarityRepository.save(EventSimilarity.builder()
                    .eventA(a)
                    .eventB(b)
                    .score(similarity.getScore())
                    .timestamp(similarity.getTimestamp())
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        List<EventSimilarity> pairs = similarityRepository.findAllByEventId(eventId);
        Set<Long> userInteractedEvents = userId > 0
                ? interactionRepository.findAllByUserId(userId).stream().map(UserEventInteraction::getEventId).collect(Collectors.toSet())
                : Collections.emptySet();

        return pairs.stream()
                .map(p -> {
                    long other = p.getEventA().equals(eventId) ? p.getEventB() : p.getEventA();
                    return Map.entry(other, p.getScore());
                })
                .filter(entry -> !userInteractedEvents.contains(entry.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        return eventIds.stream()
                .map(id -> {
                    double sum = interactionRepository.sumWeightByEventId(id);
                    return RecommendedEventProto.newBuilder()
                            .setEventId(id)
                            .setScore(sum)
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        List<UserEventInteraction> userInteractions = interactionRepository.findAllByUserId(userId);
        if (userInteractions.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> interactedEvents = userInteractions.stream().map(UserEventInteraction::getEventId).collect(Collectors.toSet());
        Map<Long, Double> userRatings = userInteractions.stream().collect(Collectors.toMap(UserEventInteraction::getEventId, UserEventInteraction::getWeight));
        Map<Long, Double> candidateScores = new HashMap<>();

        Set<Long> candidateIds = new HashSet<>();
        for (Long evaluatedEventId : interactedEvents) {
            for (EventSimilarity sim : similarityRepository.findAllByEventId(evaluatedEventId)) {
                long other = sim.getEventA().equals(evaluatedEventId) ? sim.getEventB() : sim.getEventA();
                if (!interactedEvents.contains(other)) {
                    candidateIds.add(other);
                }
            }
        }

        for (Long candidateId : candidateIds) {
            List<EventSimilarity> sims = similarityRepository.findAllByEventId(candidateId);
            double weightedSum = 0.0;
            double simSum = 0.0;

            for (EventSimilarity s : sims) {
                long evaluatedNeighbor = s.getEventA().equals(candidateId) ? s.getEventB() : s.getEventA();
                if (userRatings.containsKey(evaluatedNeighbor)) {
                    double sim = s.getScore();
                    double rating = userRatings.get(evaluatedNeighbor);
                    weightedSum += sim * rating;
                    simSum += sim;
                }
            }

            if (simSum > 0) {
                candidateScores.put(candidateId, weightedSum / simSum);
            }
        }

        return candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .toList();
    }
}