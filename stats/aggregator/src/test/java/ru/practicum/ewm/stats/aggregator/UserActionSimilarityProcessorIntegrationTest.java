package ru.practicum.ewm.stats.aggregator;

import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.range.LongRangeRandomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.practicum.ewm.stats.aggregator.model.UserEventInteractionEntity;
import ru.practicum.ewm.stats.aggregator.repository.EventPairWeightRepository;
import ru.practicum.ewm.stats.aggregator.repository.EventWeightRepository;
import ru.practicum.ewm.stats.aggregator.validation.UserActionValidatorImpl;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Проверяет алгоритм вместе с реальными PostgreSQL-запросами, подменяя только публикацию в Kafka.
 */
@DataJpaTest(showSql = false, properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=always"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = UserActionSimilarityProcessorIntegrationTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
class UserActionSimilarityProcessorIntegrationTest {
    private static final Instant TIME = Instant.parse("2026-11-07T00:00:00Z");
    private static final double EPSILON = 1e-10;

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Configuration
    @EntityScan(basePackageClasses = UserEventInteractionEntity.class)
    @EnableJpaRepositories(basePackageClasses = EventPairWeightRepository.class)
    @Import({UserActionSimilarityProcessorImpl.class, UserActionValidatorImpl.class})
    static class Config {
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private UserActionSimilarityProcessor processor;
    @Autowired
    private EventPairWeightRepository pairRepository;
    @Autowired
    private EventWeightRepository weightRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @MockBean
    private EventSimilarityPublisher publisher;

    private final Map<Pair, Double> published = new HashMap<>();
    private EasyRandom random;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE aggregator_event_pair_weights, aggregator_event_weights, aggregator_user_event_interactions");
        published.clear();
        random = createRandom(67L);
    }

    private static EasyRandom createRandom(long seed) {
        Random values = new Random(seed);
        return new EasyRandom(new EasyRandomParameters().seed(seed)
                .randomize(long.class, new LongRangeRandomizer(1L, 1_000_000L, seed))
                .randomize(Instant.class, () -> TIME.plusMillis(values.nextInt(100_000))));
    }

    private UserActionAvro action(long user, long event, ActionTypeAvro type) {
        UserActionAvro action = random.nextObject(UserActionAvro.class);
        action.setUserId(user);
        action.setEventId(event);
        action.setActionType(type);
        return action;
    }

    private void capturePublications() {
        doAnswer(invocation -> {
            EventSimilarityAvro similarity = invocation.getArgument(0);
            assertTrue(similarity.getEventA() < similarity.getEventB(), "Пара должна быть канонической");
            published.put(new Pair(similarity.getEventA(), similarity.getEventB()), similarity.getScore());
            return CompletableFuture.completedFuture(null);
        }).when(publisher).publish(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Порядок поступления мероприятий не меняет пару и её сходство")
    void pairsAreSymmetric(boolean reverse) {
        capturePublications();
        long user = random.nextLong(1, 1_000_000);
        long eventA = random.nextLong(1, 1_000_000);
        long eventB = eventA + 1;
        UserActionAvro first = action(user, eventA, ActionTypeAvro.VIEW);
        UserActionAvro second = action(user, eventB, ActionTypeAvro.LIKE);
        processor.process(reverse ? second : first);
        processor.process(reverse ? first : second);

        assertEquals(1, pairRepository.count());
        assertEquals(0.4, pairRepository.findAll().getFirst().getSharedWeight(), EPSILON);
        assertEquals(0.4 / Math.sqrt(0.4), published.get(new Pair(eventA, eventB)), EPSILON);
        assertMatchesFullCalculation(List.of(first, second));
    }

    @Test
    @DisplayName("Мероприятия без общих пользователей не образуют пару")
    void noPairWithoutCommonUsers() {
        long user = random.nextLong(1, 1_000_000);
        long event = random.nextLong(1, 1_000_000);
        List<UserActionAvro> history = List.of(
                action(user, event, ActionTypeAvro.VIEW),
                action(user + 1, event + 1, ActionTypeAvro.LIKE),
                action(user, event, ActionTypeAvro.REGISTER),
                action(user + 1, event + 1, ActionTypeAvro.REGISTER)
        );
        history.forEach(processor::process);

        assertEquals(2, weightRepository.count());
        assertEquals(0, pairRepository.count());
        verifyNoInteractions(publisher);
        assertMatchesFullCalculation(history);
    }

    @ParameterizedTest
    @ValueSource(longs = {67L, 123L, 2026L})
    @DisplayName("Инкрементальные суммы и сходство совпадают с полным пересчётом после каждого действия")
    void incrementalCalculationMatchesFullHistory(long seed) {
        capturePublications();
        random = createRandom(seed);
        Random choices = new Random(seed);
        long firstUser = choices.nextLong(1, 1_000_000);
        long firstEvent = choices.nextLong(1, 1_000_000);
        List<UserActionAvro> history = new ArrayList<>();
        // Гарантируем повышение веса и изменение знаменателя пары с прежним числителем.
        List<UserActionAvro> prefix = List.of(
                action(firstUser, firstEvent, ActionTypeAvro.VIEW),
                action(firstUser, firstEvent + 1, ActionTypeAvro.VIEW),
                action(firstUser, firstEvent, ActionTypeAvro.LIKE),
                action(firstUser, firstEvent, ActionTypeAvro.LIKE),
                action(firstUser, firstEvent, ActionTypeAvro.VIEW)
        );
        List<UserActionAvro> actions = new ArrayList<>(prefix);
        for (int i = 0; i < 80; i++) {
            actions.add(action(firstUser + choices.nextInt(5), firstEvent + choices.nextInt(4),
                    ActionTypeAvro.values()[choices.nextInt(ActionTypeAvro.values().length)]));
        }
        for (UserActionAvro action : actions) {
            history.add(action);
            processor.process(action);
            assertMatchesFullCalculation(history);
        }
    }

    /**
     * Эталон заново строится из истории, не используя инкрементальные дельты и SQL агрегатора.
     */
    private void assertMatchesFullCalculation(List<UserActionAvro> history) {
        Map<Long, Map<Long, Double>> byEvent = new HashMap<>();
        for (UserActionAvro action : history) {
            double weight = switch (action.getActionType()) {
                case VIEW -> 0.4;
                case REGISTER -> 0.8;
                case LIKE -> 1.0;
            };
            byEvent.computeIfAbsent(action.getEventId(), ignored -> new HashMap<>())
                    .merge(action.getUserId(), weight, Math::max);
        }
        Map<Long, Double> totals = new HashMap<>();
        byEvent.forEach((event, users) -> totals.put(event, users.values().stream().mapToDouble(Double::doubleValue).sum()));
        assertEquals(totals.size(), weightRepository.count());
        weightRepository.findAll().forEach(row -> assertEquals(totals.get(row.getEventId()), row.getTotalWeight(), EPSILON));

        Map<Pair, Double> shared = new HashMap<>();
        List<Long> events = byEvent.keySet().stream().sorted().toList();
        for (int a = 0; a < events.size(); a++) {
            for (int b = a + 1; b < events.size(); b++) {
                Map<Long, Double> usersA = byEvent.get(events.get(a));
                Map<Long, Double> usersB = byEvent.get(events.get(b));
                double sum = usersA.entrySet().stream().filter(entry -> usersB.containsKey(entry.getKey()))
                        .mapToDouble(entry -> Math.min(entry.getValue(), usersB.get(entry.getKey()))).sum();
                if (sum > 0) {
                    shared.put(new Pair(events.get(a), events.get(b)), sum);
                }
            }
        }
        assertEquals(shared.size(), pairRepository.count());
        pairRepository.findAll().forEach(row -> assertEquals(
                shared.get(new Pair(row.getId().getEventA(), row.getId().getEventB())), row.getSharedWeight(), EPSILON));
        assertEquals(shared.keySet(), published.keySet());
        shared.forEach((pair, sum) -> assertEquals(sum / Math.sqrt(totals.get(pair.a()) * totals.get(pair.b())),
                published.get(pair), EPSILON, "Полный пересчёт, шаг " + history.size() + ", пара " + pair));
    }

    private record Pair(long a, long b) {
    }
}
