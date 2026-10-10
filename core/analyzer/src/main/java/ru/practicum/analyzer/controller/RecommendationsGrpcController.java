package ru.practicum.analyzer.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.proto.*;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("gRPC GetRecommendationsForUser: userId={}, maxResults={}", request.getUserId(), request.getMaxResults());
        List<RecommendedEventProto> recommendations = recommendationService.getRecommendationsForUser(request.getUserId(), request.getMaxResults());
        recommendations.forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("gRPC GetSimilarEvents: eventId={}, userId={}, maxResults={}", request.getEventId(), request.getUserId(), request.getMaxResults());
        List<RecommendedEventProto> similar = recommendationService.getSimilarEvents(request.getEventId(), request.getUserId(), request.getMaxResults());
        similar.forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("gRPC GetInteractionsCount: eventIds count={}", request.getEventIdCount());
        List<RecommendedEventProto> counts = recommendationService.getInteractionsCount(request.getEventIdList());
        counts.forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }
}