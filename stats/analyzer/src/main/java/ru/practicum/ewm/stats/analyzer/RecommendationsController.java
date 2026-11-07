package ru.practicum.ewm.stats.analyzer;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.mapper.RecommendationMapper;
import ru.practicum.ewm.stats.model.response.RecommendedEvent;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;
import ru.practicum.ewm.stats.proto.dashboard.RecommendationsControllerGrpc;

import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationService recommendationService;

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> observer) {
        send(recommendationService.getSimilarEvents(RecommendationMapper.toModel(request)), observer);
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> observer) {
        send(recommendationService.getRecommendationsForUser(RecommendationMapper.toModel(request)), observer);
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> observer) {
        send(recommendationService.getInteractionsCount(RecommendationMapper.toModel(request)), observer);
    }

    private void send(List<RecommendedEvent> events, StreamObserver<RecommendedEventProto> observer) {
        for (RecommendedEvent event : events) {
            observer.onNext(RecommendationMapper.toProto(event));
        }
        observer.onCompleted();
    }
}
