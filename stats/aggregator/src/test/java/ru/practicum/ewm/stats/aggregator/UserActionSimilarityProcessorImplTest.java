package ru.practicum.ewm.stats.aggregator;

import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.range.LongRangeRandomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.stats.aggregator.model.UserEventInteractionEntity;
import ru.practicum.ewm.stats.aggregator.model.UserEventInteractionId;
import ru.practicum.ewm.stats.aggregator.repository.EventPairWeightRepository;
import ru.practicum.ewm.stats.aggregator.repository.EventPairWeightRepository.SimilarityAggregate;
import ru.practicum.ewm.stats.aggregator.repository.EventWeightRepository;
import ru.practicum.ewm.stats.aggregator.repository.UserEventInteractionRepository;
import ru.practicum.ewm.stats.aggregator.validation.UserActionValidatorImpl;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserActionSimilarityProcessorImplTest {
    private static final Instant TIME = Instant.parse("2026-11-07T00:00:00Z");

    @Mock
    private EventSimilarityPublisher publisher;
    @Mock
    private UserEventInteractionRepository interactionRepository;
    @Mock
    private EventWeightRepository eventWeightRepository;
    @Mock
    private EventPairWeightRepository eventPairWeightRepository;

    private EasyRandom random;
    private UserActionSimilarityProcessor processor;

    private static EasyRandom createRandom() {
        Random values = new Random(67L);
        return new EasyRandom(new EasyRandomParameters()
                .seed(67L)
                .randomize(Long.class, new LongRangeRandomizer(1L, 1_000_000L, 67L))
                .randomize(long.class, new LongRangeRandomizer(1L, 1_000_000L, 68L))
                .randomize(double.class, () -> values.nextInt(1, 6) * 0.2)
                .randomize(Instant.class, () -> Instant.ofEpochMilli(values.nextLong(
                        Instant.parse("2020-01-01T00:00:00Z").toEpochMilli(),
                        Instant.parse("2030-01-01T00:00:00Z").toEpochMilli()))));
    }

    @BeforeEach
    void setUp() {
        random = createRandom();
        processor = new UserActionSimilarityProcessorImpl(publisher, interactionRepository,
                eventWeightRepository, eventPairWeightRepository, new UserActionValidatorImpl());
    }

    @ParameterizedTest
    @DisplayName("Невалидное действие отклоняется до обращения к БД")
    @MethodSource("invalidActions")
    void rejectsInvalidActionBeforeAccessingStorage(UserActionAvro action, String message) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> processor.process(action));

        assertEquals(message, exception.getMessage());
        verifyNoInteractions(interactionRepository, eventWeightRepository, eventPairWeightRepository, publisher);
    }

    private static Stream<Arguments> invalidActions() {
        return Stream.of(
                Arguments.of(null, "action must be set"),
                Arguments.of(invalidAction(a -> a.setUserId(0L)), "user_id must be positive"),
                Arguments.of(invalidAction(a -> a.setUserId(-1L)), "user_id must be positive"),
                Arguments.of(invalidAction(a -> a.setEventId(0L)), "event_id must be positive"),
                Arguments.of(invalidAction(a -> a.setEventId(-1L)), "event_id must be positive"),
                Arguments.of(invalidAction(a -> a.setActionType(null)), "action_type must be set"),
                Arguments.of(new EasyRandom(new EasyRandomParameters().seed(67L)
                        .randomize(long.class, () -> 1L)
                        .excludeField(field -> field.getName().equals("timestamp")))
                        .nextObject(UserActionAvro.class), "timestamp must be set")
        );
    }

    private static UserActionAvro invalidAction(Consumer<UserActionAvro> corrupt) {
        UserActionAvro action = createRandom().nextObject(UserActionAvro.class);
        corrupt.accept(action);
        return action;
    }

    @ParameterizedTest
    @DisplayName("Первое действие без пар сохраняет вес без публикации сходства")
    @CsvSource({"VIEW, 0.4", "REGISTER, 0.8", "LIKE, 1.0"})
    void storesInitialWeightWithoutPublishingWhenThereAreNoPairs(ActionTypeAvro type, double weight) {
        UserActionAvro action = random.nextObject(UserActionAvro.class);
        action.setEventId(action.getEventId() + 1);
        UserEventInteractionId id = random.nextObject(UserEventInteractionId.class);
        id.setUserId(action.getUserId());
        id.setEventId(action.getEventId());
        when(interactionRepository.findById(id)).thenReturn(Optional.empty());

        action.setActionType(type);
        processor.process(action);

        ArgumentCaptor<UserEventInteractionEntity> interaction = ArgumentCaptor.forClass(UserEventInteractionEntity.class);
        verify(interactionRepository).save(interaction.capture());
        assertEquals(weight, interaction.getValue().getWeight(), 1e-10);
        assertEquals(action.getTimestamp(), interaction.getValue().getLastInteractionTime());
        verify(eventWeightRepository).addWeight(action.getEventId(), weight);
        verify(eventPairWeightRepository).updateSharedWeights(action.getUserId(), action.getEventId(), 0.0, weight);
        verifyNoInteractions(publisher);
    }

    @ParameterizedTest
    @DisplayName("Повторное или менее весомое действие обновляет только время, если оно новее")
    @CsvSource({"VIEW, 10", "LIKE, 10", "LIKE, -10"})
    void repeatOrLowerWeightOnlyAdvancesInteractionTime(ActionTypeAvro type, long seconds) {
        UserActionAvro action = random.nextObject(UserActionAvro.class);
        action.setEventId(action.getEventId() + 1);
        UserEventInteractionId id = random.nextObject(UserEventInteractionId.class);
        id.setUserId(action.getUserId());
        id.setEventId(action.getEventId());
        UserEventInteractionEntity interaction = random.nextObject(UserEventInteractionEntity.class);
        interaction.setId(id);
        interaction.setWeight(1.0);
        interaction.setLastInteractionTime(TIME);
        when(interactionRepository.findById(id)).thenReturn(Optional.of(interaction));

        action.setActionType(type);
        action.setTimestamp(TIME.plusSeconds(seconds));
        processor.process(action);

        assertEquals(1.0, interaction.getWeight());
        assertEquals(seconds > 0 ? TIME.plusSeconds(seconds) : TIME, interaction.getLastInteractionTime());
        if (seconds > 0) {
            verify(interactionRepository).save(interaction);
        } else {
            verify(interactionRepository, never()).save(any());
        }
        verify(interactionRepository, never()).findAllByIdUserIdAndIdEventIdNot(anyLong(), anyLong());
        verifyNoInteractions(eventWeightRepository, eventPairWeightRepository, publisher);
    }

    @Test
    @DisplayName("Рост веса обновляет суммы и публикует сходство всех пар мероприятия")
    void increasedWeightUpdatesOnlyPositivePairDeltasAndRepublishesAllEventPairs() {
        UserActionAvro action = random.nextObject(UserActionAvro.class);
        action.setEventId(action.getEventId() + 1);
        UserEventInteractionId id = random.nextObject(UserEventInteractionId.class);
        id.setUserId(action.getUserId());
        id.setEventId(action.getEventId());
        UserEventInteractionEntity interaction = random.nextObject(UserEventInteractionEntity.class);
        interaction.setId(id);
        interaction.setWeight(0.4);
        interaction.setLastInteractionTime(TIME);
        when(interactionRepository.findById(id)).thenReturn(Optional.of(interaction));
        List<SimilarityAggregate> aggregates = List.of(
                aggregate(action.getEventId() - 1, action.getEventId(), 0.8, 1.0, 0.8),
                aggregate(action.getEventId(), action.getEventId() + 1, 0.4, 0.8, 0.4)
        );
        when(eventPairWeightRepository.findSimilaritiesForEvent(action.getEventId())).thenReturn(aggregates);
        doReturn(CompletableFuture.completedFuture(null)).when(publisher).publish(any());

        action.setActionType(ActionTypeAvro.REGISTER);
        action.setTimestamp(TIME.minusSeconds(10));
        processor.process(action);

        assertEquals(0.8, interaction.getWeight());
        assertEquals(TIME, interaction.getLastInteractionTime());
        verify(eventWeightRepository).addWeight(action.getEventId(), 0.4);
        verify(eventPairWeightRepository).updateSharedWeights(action.getUserId(), action.getEventId(), 0.4, 0.8);
        ArgumentCaptor<EventSimilarityAvro> similarity = ArgumentCaptor.forClass(EventSimilarityAvro.class);
        verify(publisher, times(2)).publish(similarity.capture());
        assertEquals(0.8 / Math.sqrt(0.8), similarity.getAllValues().get(0).getScore(), 1e-10);
        assertEquals(action.getEventId(), similarity.getAllValues().get(1).getEventA());
        assertEquals(action.getEventId() + 1, similarity.getAllValues().get(1).getEventB());
        assertEquals(0.4 / Math.sqrt(0.8 * 0.4), similarity.getAllValues().get(1).getScore(), 1e-10);
        assertEquals(TIME.minusSeconds(10), similarity.getAllValues().get(1).getTimestamp());
    }

    private SimilarityAggregate aggregate(long eventA, long eventB, double shared, double weightA, double weightB) {
        SimilarityAggregate aggregate = mock(SimilarityAggregate.class);
        when(aggregate.getEventA()).thenReturn(eventA);
        when(aggregate.getEventB()).thenReturn(eventB);
        when(aggregate.getSharedWeight()).thenReturn(shared);
        when(aggregate.getEventAWeight()).thenReturn(weightA);
        when(aggregate.getEventBWeight()).thenReturn(weightB);
        return aggregate;
    }
}
