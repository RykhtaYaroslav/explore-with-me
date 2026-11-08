package ru.practicum.stats.client.grpc;

import com.google.protobuf.Empty;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.range.LongRangeRandomizer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.practicum.ewm.stats.model.action.ActionType;
import ru.practicum.ewm.stats.model.action.UserAction;
import ru.practicum.ewm.stats.model.request.InteractionsCountRequest;
import ru.practicum.ewm.stats.model.request.SimilarEventsRequest;
import ru.practicum.ewm.stats.model.request.UserPredictionsRequest;
import ru.practicum.ewm.stats.model.response.RecommendedEvent;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;
import ru.practicum.ewm.stats.proto.ViewCountsProto;
import ru.practicum.ewm.stats.proto.collector.UserActionControllerGrpc.UserActionControllerImplBase;
import ru.practicum.ewm.stats.proto.dashboard.RecommendationsControllerGrpc.RecommendationsControllerImplBase;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest(
        classes = StatsGrpcClientsIntegrationTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false"}
)
@DirtiesContext
class StatsGrpcClientsIntegrationTest {
    private static final Instant ACTION_TIME = Instant.parse("2026-11-08T01:02:03.123456789Z");
    private static final long REJECTED_USER_ID = 10L;
    private static final long UNRESPONSIVE_USER_ID = 11L;

    private static final CollectorService COLLECTOR = new CollectorService();
    private static final AnalyzerService ANALYZER = new AnalyzerService();
    private static Server collectorServer;
    private static Server analyzerServer;

    @Autowired
    private CollectorGrpcClient collector;
    @Autowired
    private AnalyzerGrpcClient analyzer;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({CollectorGrpcClient.class, AnalyzerGrpcClient.class})
    static class Config {
    }

    @BeforeAll
    static void startServers() throws IOException {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
        collectorServer = NettyServerBuilder.forAddress(address).addService(COLLECTOR).build().start();
        analyzerServer = NettyServerBuilder.forAddress(address).addService(ANALYZER).build().start();
    }

    @AfterAll
    static void stopServers() {
        if (collectorServer != null) {
            collectorServer.shutdownNow();
        }
        if (analyzerServer != null) {
            analyzerServer.shutdownNow();
        }
    }

    @DynamicPropertySource
    static void configureDiscovery(DynamicPropertyRegistry registry) {
        registerService(registry, "collector", collectorServer);
        registerService(registry, "analyzer", analyzerServer);
    }

    private static void registerService(DynamicPropertyRegistry registry, String name, Server server) {
        registry.add(String.format("grpc.client.%s.address", name), () -> String.format("discovery:///%s", name));
        registry.add(String.format("grpc.client.%s.negotiation-type", name), () -> "plaintext");
        registry.add(String.format("stats.client.grpc.%s.deadline-ms", name), () -> 1500);
        String instance = String.format("spring.cloud.discovery.client.simple.instances.%s[0]", name);
        registry.add(String.format("%s.uri", instance), () -> "http://127.0.0.1:1");
        registry.add(String.format("%s.metadata[gRPC_port]", instance), () -> Integer.toString(server.getPort()));
    }

    @ParameterizedTest
    @EnumSource(ActionType.class)
    @DisplayName("Collector forwards the action and nanosecond timestamp to its discovered port")
    void sendsEachActionType(ActionType type) {
        EasyRandom random = new EasyRandom(new EasyRandomParameters().seed(67L)
                .randomize(Long.class, new LongRangeRandomizer(100L, 1_000_000L, 67L)));
        UserAction action = new UserAction(random.nextObject(Long.class), random.nextObject(Long.class),
                type, ACTION_TIME);

        collector.collectUserAction(action);

        UserActionProto sent = COLLECTOR.action.get();
        assertThat(sent.getUserId()).isEqualTo(action.userId());
        assertThat(sent.getEventId()).isEqualTo(action.eventId());
        assertThat(sent.getActionType()).isEqualTo(ActionTypeProto.valueOf(String.format("ACTION_%s", type)));
        assertThat(sent.getTimestamp().getSeconds()).isEqualTo(action.timestamp().getEpochSecond());
        assertThat(sent.getTimestamp().getNanos()).isEqualTo(action.timestamp().getNano());
    }

    @Test
    @DisplayName("Analyzer forwards prediction filters and preserves streamed order")
    void getsRecommendationsForUser() {
        assertThat(analyzer.getRecommendationsForUser(new UserPredictionsRequest(27L, 2)))
                .containsExactly(new RecommendedEvent(40L, 0.9), new RecommendedEvent(30L, 0.7));
        assertThat(ANALYZER.predictions.get()).isEqualTo(
                UserPredictionsRequestProto.newBuilder().setUserId(27L).setMaxResults(2).build());
    }

    @Test
    @DisplayName("Analyzer forwards the event, user and limit for similar events")
    void getsSimilarEvents() {
        assertThat(analyzer.getSimilarEvents(new SimilarEventsRequest(19L, 27L, 1)))
                .containsExactly(new RecommendedEvent(30L, 0.8));
        assertThat(ANALYZER.similarEvents.get()).isEqualTo(
                SimilarEventsRequestProto.newBuilder().setEventId(19L).setUserId(27L).setMaxResults(1).build());
    }

    @Test
    @DisplayName("Analyzer forwards count IDs and handles both populated and empty streams")
    void getsInteractionsCount() {
        assertThat(analyzer.getInteractionsCount(new InteractionsCountRequest(List.of(30L, 40L))))
                .containsExactly(new RecommendedEvent(30L, 2.4), new RecommendedEvent(40L, 0.0));
        assertThat(ANALYZER.counts.get().getEventIdList()).containsExactly(30L, 40L);

        assertThat(analyzer.getInteractionsCount(new InteractionsCountRequest(List.of()))).isEmpty();
        assertThat(ANALYZER.counts.get().getEventIdList()).isEmpty();
    }

    @Test
    @DisplayName("Analyzer returns 64-bit viewer counts for a batch of event IDs")
    void getsViewCounts() {
        assertThat(analyzer.getViewCounts(new InteractionsCountRequest(List.of(30L, 40L))))
                .containsExactlyInAnyOrderEntriesOf(Map.of(30L, 3_000_000_000L));
        assertThat(ANALYZER.viewCounts.get().getEventIdList()).containsExactly(30L, 40L);
    }

    @Test
    @DisplayName("Collector preserves the gRPC error status and cause")
    void handlesCollectorError() {
        UserAction action = new UserAction(REJECTED_USER_ID, 19L, ActionType.LIKE, ACTION_TIME);

        assertThatExceptionOfType(StatsGrpcException.class)
                .isThrownBy(() -> collector.collectUserAction(action))
                .withMessageContaining("CollectUserAction")
                .withCauseInstanceOf(StatusRuntimeException.class)
                .satisfies(error -> assertThat(error.getStatusCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
    }

    @Test
    @DisplayName("Analyzer propagates a failed stream after a partial response")
    void handlesErrorDuringStreamIteration() {
        SimilarEventsRequest request = new SimilarEventsRequest(19L, REJECTED_USER_ID, 2);

        assertThatExceptionOfType(StatsGrpcException.class)
                .isThrownBy(() -> analyzer.getSimilarEvents(request))
                .withMessageContaining("GetSimilarEvents")
                .withCauseInstanceOf(StatusRuntimeException.class)
                .satisfies(error -> assertThat(error.getStatusCode()).isEqualTo(Status.Code.UNAVAILABLE));
    }

    @Test
    @DisplayName("Collector applies the configured deadline while waiting for acknowledgement")
    void enforcesCollectorDeadline() {
        UserAction action = new UserAction(UNRESPONSIVE_USER_ID, 19L, ActionType.VIEW, ACTION_TIME);

        assertThatExceptionOfType(StatsGrpcException.class)
                .isThrownBy(() -> collector.collectUserAction(action))
                .satisfies(error -> assertThat(error.getStatusCode()).isEqualTo(Status.Code.DEADLINE_EXCEEDED));
    }

    @Test
    @DisplayName("Analyzer applies the configured deadline through stream completion")
    void enforcesAnalyzerDeadline() {
        SimilarEventsRequest request = new SimilarEventsRequest(19L, UNRESPONSIVE_USER_ID, 2);

        assertThatExceptionOfType(StatsGrpcException.class)
                .isThrownBy(() -> analyzer.getSimilarEvents(request))
                .satisfies(error -> assertThat(error.getStatusCode()).isEqualTo(Status.Code.DEADLINE_EXCEEDED));
    }

    private static class CollectorService extends UserActionControllerImplBase {
        private final AtomicReference<UserActionProto> action = new AtomicReference<>();

        @Override
        public void collectUserAction(UserActionProto request, StreamObserver<Empty> observer) {
            action.set(request);
            if (request.getUserId() == REJECTED_USER_ID) {
                observer.onError(Status.INVALID_ARGUMENT.asRuntimeException());
            } else if (request.getUserId() != UNRESPONSIVE_USER_ID) {
                observer.onNext(Empty.getDefaultInstance());
                observer.onCompleted();
            }
        }
    }

    private static class AnalyzerService extends RecommendationsControllerImplBase {
        private final AtomicReference<UserPredictionsRequestProto> predictions = new AtomicReference<>();
        private final AtomicReference<SimilarEventsRequestProto> similarEvents = new AtomicReference<>();
        private final AtomicReference<InteractionsCountRequestProto> counts = new AtomicReference<>();
        private final AtomicReference<InteractionsCountRequestProto> viewCounts = new AtomicReference<>();

        @Override
        public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                              StreamObserver<RecommendedEventProto> observer) {
            predictions.set(request);
            observer.onNext(event(40L, 0.9));
            observer.onNext(event(30L, 0.7));
            observer.onCompleted();
        }

        @Override
        public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> observer) {
            similarEvents.set(request);
            observer.onNext(event(30L, 0.8));
            if (request.getUserId() == REJECTED_USER_ID) {
                observer.onError(Status.UNAVAILABLE.asRuntimeException());
            } else if (request.getUserId() != UNRESPONSIVE_USER_ID) {
                observer.onCompleted();
            }
        }

        @Override
        public void getInteractionsCount(InteractionsCountRequestProto request,
                                         StreamObserver<RecommendedEventProto> observer) {
            counts.set(request);
            if (request.getEventIdCount() > 0) {
                observer.onNext(event(30L, 2.4));
                observer.onNext(event(40L, 0.0));
            }
            observer.onCompleted();
        }

        @Override
        public void getViewCounts(InteractionsCountRequestProto request, StreamObserver<ViewCountsProto> observer) {
            viewCounts.set(request);
            observer.onNext(ViewCountsProto.newBuilder().putViewsByEvent(30L, 3_000_000_000L).build());
            observer.onCompleted();
        }

        private RecommendedEventProto event(long id, double score) {
            return RecommendedEventProto.newBuilder().setEventId(id).setScore(score).build();
        }
    }
}
