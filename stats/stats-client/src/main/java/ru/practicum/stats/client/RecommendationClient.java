package ru.practicum.stats.client;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RecommendationClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorClient;

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerClient;

    public void sendUserAction(long userId, long eventId, ActionTypeProto actionType) {
        try {
            Instant now = Instant.now();
            UserActionProto proto = UserActionProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(now.getEpochSecond())
                            .setNanos(now.getNano())
                            .build())
                    .build();
            collectorClient.collectUserAction(proto);
            log.info("Sent user action to Collector: user={}, event={}, action={}", userId, eventId, actionType);
        } catch (Exception e) {
            log.warn("Failed to send action to Collector (fallback): {}", e.getMessage());
        }
    }

    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        try {
            UserPredictionsRequestProto proto = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();
            Iterator<RecommendedEventProto> iterator = analyzerClient.getRecommendationsForUser(proto);
            List<RecommendedEventProto> list = new ArrayList<>();
            iterator.forEachRemaining(list::add);
            return list;
        } catch (Exception e) {
            log.warn("Failed to get recommendations from Analyzer: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            InteractionsCountRequestProto proto = InteractionsCountRequestProto.newBuilder()
                    .addAllEventId(eventIds)
                    .build();
            Iterator<RecommendedEventProto> iterator = analyzerClient.getInteractionsCount(proto);
            Map<Long, Double> map = new HashMap<>();
            iterator.forEachRemaining(r -> map.put(r.getEventId(), r.getScore()));
            return map;
        } catch (Exception e) {
            log.warn("Failed to get interactions count from Analyzer: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}