package ru.practicum.ewm.stats.analyzer;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.kafka.AnalyzerKafkaListeners;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityId;
import ru.practicum.ewm.stats.analyzer.model.UserEventInteractionId;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.UserEventInteractionRepository;
import ru.practicum.ewm.stats.analyzer.service.AnalyzerIngestionServiceImpl;
import ru.practicum.ewm.stats.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;
import ru.practicum.ewm.stats.proto.dashboard.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.dashboard.RecommendationsControllerGrpc.RecommendationsControllerBlockingStub;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest(showSql = false, properties = {
        "spring.cloud.config.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=always",
        "stats.recommendations.recent-events=3",
        "stats.recommendations.neighbors=2"
})
@ContextConfiguration(classes = RecommendationsControllerIntegrationTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RecommendationsControllerIntegrationTest {
    private static final Instant TIME = Instant.parse("2026-11-07T00:00:00Z");
    private static final long USER_ID = 1L;

    @Configuration
    @EntityScan(basePackageClasses = EventSimilarityId.class)
    @EnableJpaRepositories(basePackageClasses = EventSimilarityRepository.class)
    @EnableConfigurationProperties(RecommendationProperties.class)
    @Import({AnalyzerIngestionServiceImpl.class, AnalyzerKafkaListeners.class,
            RecommendationService.class, RecommendationsController.class})
    static class Config {
    }

    @Autowired
    private AnalyzerKafkaListeners listeners;
    @Autowired
    private RecommendationsController controller;
    @Autowired
    private UserEventInteractionRepository interactionRepository;
    @Autowired
    private EventSimilarityRepository similarityRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private Server server;
    private ManagedChannel channel;
    private RecommendationsControllerBlockingStub stub;

    @BeforeEach
    void setUp() throws IOException {
        jdbc.execute("TRUNCATE TABLE event_similarities");
        jdbc.execute("TRUNCATE TABLE user_event_interactions");
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName).directExecutor().addService(controller).build().start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = RecommendationsControllerGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    @DisplayName("Повторы сохраняют максимум веса, последнее время и одну запись на пару")
    void storesInteractionAndSimilarityUpdates() {
        recordAction(USER_ID, 10L, ActionTypeAvro.VIEW, 1);
        recordAction(USER_ID, 10L, ActionTypeAvro.LIKE, 0);
        recordAction(USER_ID, 10L, ActionTypeAvro.REGISTER, 2);
        recordAction(USER_ID, 10L, ActionTypeAvro.REGISTER, 2);
        recordSimilarity(10L, 20L, 0.3, 1);
        recordSimilarity(10L, 20L, 0.8, 2);
        recordSimilarity(10L, 20L, 0.8, 2);

        assertThat(interactionRepository.findAll()).singleElement().satisfies(interaction -> {
            assertThat(interaction.getId()).isEqualTo(new UserEventInteractionId(USER_ID, 10L));
            assertThat(interaction.getWeight()).isEqualTo(1.0);
            assertThat(interaction.getLastInteractionTime()).isEqualTo(TIME.plusSeconds(2));
        });
        assertThat(similarityRepository.findAll()).singleElement().satisfies(similarity -> {
            assertThat(similarity.getId()).isEqualTo(new EventSimilarityId(10L, 20L));
            assertThat(similarity.getScore()).isEqualTo(0.8);
            assertThat(similarity.getTimestamp()).isEqualTo(TIME.plusSeconds(2));
        });
    }

    @Test
    @DisplayName("Похожие мероприятия находятся с обеих сторон пары и выдаются с учётом лимита")
    void streamsUnseenSimilarEventsInScoreOrder() {
        recordAction(USER_ID, 10L, ActionTypeAvro.VIEW, 1);
        recordAction(2L, 30L, ActionTypeAvro.VIEW, 1);
        recordSimilarity(10L, 20L, 0.99, 1);
        recordSimilarity(15L, 20L, 0.8, 1);
        recordSimilarity(20L, 30L, 0.9, 1);
        recordSimilarity(20L, 40L, 0.8, 1);
        recordSimilarity(50L, 60L, 1.0, 1);

        List<RecommendedEventProto> results = similarEvents(20L, 2);

        assertThat(results).extracting(RecommendedEventProto::getEventId).containsExactly(30L, 15L);
        assertThat(results).extracting(RecommendedEventProto::getScore).containsExactly(0.9, 0.8);
    }

    @Test
    @DisplayName("Рекомендации отбирают кандидатов по недавним событиям и оценивают их по всей истории")
    void streamsLimitedPredictionsByWeightedScore() {
        recordAction(USER_ID, 8L, ActionTypeAvro.LIKE, 2);
        recordAction(USER_ID, 10L, ActionTypeAvro.VIEW, 5);
        recordAction(USER_ID, 20L, ActionTypeAvro.REGISTER, 4);
        recordAction(USER_ID, 30L, ActionTypeAvro.VIEW, 3);
        recordAction(USER_ID, 50L, ActionTypeAvro.VIEW, 1);

        recordSimilarity(10L, 50L, 1.0, 1);
        recordSimilarity(10L, 15L, 0.3, 1);
        recordSimilarity(15L, 30L, 0.85, 1);
        recordSimilarity(10L, 60L, 0.95, 1);
        recordSimilarity(20L, 60L, 0.6, 1);
        recordSimilarity(8L, 60L, 1.0, 1);
        recordSimilarity(50L, 60L, 0.8, 1);
        recordSimilarity(20L, 70L, 0.9, 1);
        recordSimilarity(30L, 70L, 0.2, 1);
        recordSimilarity(8L, 70L, 0.95, 1);
        recordSimilarity(30L, 90L, 0.8, 1);
        recordSimilarity(8L, 90L, 0.99, 1);
        recordSimilarity(8L, 100L, 1.0, 1);
        recordSimilarity(5L, 30L, 0.75, 1);

        List<RecommendedEventProto> results = recommendations(USER_ID, 10);

        assertThat(results).extracting(RecommendedEventProto::getEventId).containsExactly(70L, 60L, 15L);
        assertThat(results.get(0).getScore()).isCloseTo((0.95 + 0.8 * 0.9) / (0.95 + 0.9), within(1e-10));
        assertThat(results.get(1).getScore()).isCloseTo((1.0 + 0.4 * 0.95) / (1.0 + 0.95), within(1e-10));
        assertThat(results.get(2).getScore()).isCloseTo(0.4, within(1e-10));
        assertThat(recommendations(USER_ID, 1)).containsExactly(results.getFirst());
    }

    @Test
    @DisplayName("Пустая история, отсутствие соседей и нулевое сходство дают пустой поток")
    void streamsNoPredictionsWithoutUsableNeighbors() {
        assertThat(recommendations(USER_ID, 10)).isEmpty();

        recordAction(USER_ID, 10L, ActionTypeAvro.VIEW, 1);
        assertThat(recommendations(USER_ID, 10)).isEmpty();

        recordSimilarity(10L, 20L, 0.0, 1);
        assertThat(recommendations(USER_ID, 10)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("Неположительный max_results даёт пустой поток")
    void streamsNoResultsForNonPositiveLimit(int limit) {
        recordAction(USER_ID, 10L, ActionTypeAvro.VIEW, 1);
        recordSimilarity(10L, 20L, 0.9, 1);

        assertThat(recommendations(USER_ID, limit)).isEmpty();
        assertThat(similarEvents(10L, limit)).isEmpty();
    }

    @Test
    @DisplayName("Суммы учитывают максимальный вес каждого пользователя и возвращаются для всех ID")
    void streamsTotalWeightsInRequestOrder() {
        recordAction(USER_ID, 10L, ActionTypeAvro.VIEW, 1);
        recordAction(USER_ID, 10L, ActionTypeAvro.LIKE, 2);
        recordAction(USER_ID, 10L, ActionTypeAvro.LIKE, 2);
        recordAction(2L, 10L, ActionTypeAvro.REGISTER, 1);
        recordAction(2L, 30L, ActionTypeAvro.VIEW, 1);

        List<RecommendedEventProto> results = collect(stub.getInteractionsCount(
                InteractionsCountRequestProto.newBuilder().addAllEventId(List.of(30L, 10L, 20L, 10L)).build()));

        assertThat(results).extracting(RecommendedEventProto::getEventId).containsExactly(30L, 10L, 20L, 10L);
        assertThat(results).extracting(RecommendedEventProto::getScore).containsExactly(0.4, 1.8, 0.0, 1.8);
        assertThat(collect(stub.getInteractionsCount(InteractionsCountRequestProto.getDefaultInstance()))).isEmpty();
    }

    @Test
    @DisplayName("VIEW counts distinct users independently of registrations and likes")
    void countsUniqueViewers() {
        recordAction(USER_ID, 10L, ActionTypeAvro.REGISTER, 1);
        recordAction(USER_ID, 10L, ActionTypeAvro.VIEW, 2);
        recordAction(USER_ID, 10L, ActionTypeAvro.VIEW, 2);
        recordAction(USER_ID, 10L, ActionTypeAvro.LIKE, 3);
        recordAction(2L, 10L, ActionTypeAvro.VIEW, 1);
        recordAction(3L, 10L, ActionTypeAvro.REGISTER, 1);
        recordAction(2L, 20L, ActionTypeAvro.LIKE, 1);
        recordAction(2L, 30L, ActionTypeAvro.VIEW, 1);
        recordAction(2L, 40L, ActionTypeAvro.VIEW, 1);

        var request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(List.of(10L, 10L, 20L, 30L, 50L))
                .build();

        assertThat(stub.getViewCounts(request).getViewsByEventMap())
                .containsExactlyInAnyOrderEntriesOf(Map.of(10L, 2L, 30L, 1L));
        assertThat(stub.getViewCounts(InteractionsCountRequestProto.getDefaultInstance()).getViewsByEventMap())
                .isEmpty();
    }

    @Test
    @DisplayName("Actions with non-positive ids are rejected and not stored")
    void rejectsNonPositiveIdsWithoutStoringInteraction() {
        assertThatThrownBy(() -> recordAction(USER_ID, 0L, ActionTypeAvro.VIEW, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> recordAction(0L, 10L, ActionTypeAvro.VIEW, 1))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(stub.getViewCounts(InteractionsCountRequestProto.newBuilder().addEventId(10L).build())
                .getViewsByEventMap()).isEmpty();
    }

    private void recordAction(long userId, long eventId, ActionTypeAvro type, long seconds) {
        listeners.onUserAction(UserActionAvro.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(type)
                .setTimestamp(TIME.plusSeconds(seconds))
                .build());
    }

    private void recordSimilarity(long eventA, long eventB, double score, long seconds) {
        listeners.onEventSimilarity(EventSimilarityAvro.newBuilder()
                .setEventA(eventA)
                .setEventB(eventB)
                .setScore(score)
                .setTimestamp(TIME.plusSeconds(seconds))
                .build());
    }

    private List<RecommendedEventProto> recommendations(long userId, int limit) {
        return collect(stub.getRecommendationsForUser(
                UserPredictionsRequestProto.newBuilder().setUserId(userId).setMaxResults(limit).build()));
    }

    private List<RecommendedEventProto> similarEvents(long eventId, int limit) {
        return collect(stub.getSimilarEvents(SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(USER_ID)
                .setMaxResults(limit)
                .build()));
    }

    private List<RecommendedEventProto> collect(Iterator<RecommendedEventProto> stream) {
        List<RecommendedEventProto> results = new ArrayList<>();
        stream.forEachRemaining(results::add);
        return results;
    }
}
