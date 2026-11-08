package ru.practicum.ewm.stats.collector;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.mock.MockProducerFactory;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.collector.error.UserActionErrorHandlerImpl;
import ru.practicum.ewm.stats.collector.validation.UserActionValidatorImpl;
import ru.practicum.ewm.stats.kafka.avro.AvroSpecificSerializer;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.ewm.stats.proto.collector.UserActionControllerGrpc;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UserActionGrpcServiceTest {
    private static final String TOPIC = "stats.user-actions.v1";
    private static final long USER_ID = 42L;
    private static final long EVENT_ID = 73L;
    private static final String KAFKA_ERROR = "Could not publish user action to Kafka";

    private final MockProducer<String, SpecificRecord> producer =
            spy(new MockProducer<>(false, new StringSerializer(), new AvroSpecificSerializer()));
    private Server server;
    private ManagedChannel channel;
    private UserActionControllerGrpc.UserActionControllerFutureStub client;

    @BeforeEach
    void setUp() throws IOException {
        KafkaTemplate<String, SpecificRecord> template = new KafkaProducerConfiguration()
                .kafkaTemplate(new MockProducerFactory<>(() -> producer), TOPIC);
        UserActionGrpcService service = new UserActionGrpcService(
                template, new UserActionValidatorImpl(), new UserActionErrorHandlerImpl());
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName).directExecutor().addService(service).build().start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        client = UserActionControllerGrpc.newFutureStub(channel);
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @ParameterizedTest
    @DisplayName("Publishes each action and waits for Kafka acknowledgment")
    @CsvSource({"ACTION_VIEW, VIEW", "ACTION_REGISTER, REGISTER", "ACTION_LIKE, LIKE"})
    void publishesEachActionAndRespondsOnlyAfterAcknowledgment(ActionTypeProto protoType, ActionTypeAvro avroType)
            throws Exception {
        ListenableFuture<Empty> response = client.collectUserAction(validRequest().setActionType(protoType).build());

        assertThat(producer.history()).singleElement().satisfies(record -> {
            assertThat(record.topic()).isEqualTo(TOPIC);
            assertThat(record.key()).isEqualTo(Long.toString(USER_ID));
            assertThat(record.value()).isInstanceOfSatisfying(UserActionAvro.class, action -> {
                assertThat(action.getUserId()).isEqualTo(USER_ID);
                assertThat(action.getEventId()).isEqualTo(EVENT_ID);
                assertThat(action.getActionType()).isEqualTo(avroType);
            });
        });
        assertThat(response.isDone()).isFalse();
        assertThat(producer.completeNext()).isTrue();
        assertThat(response.get(5, TimeUnit.SECONDS)).isEqualTo(Empty.getDefaultInstance());
    }

    @ParameterizedTest
    @DisplayName("Converts Protobuf timestamps to Avro milliseconds")
    @CsvSource({
            "1700000000, 123456789, 2023-11-14T22:13:20.123Z",
            "0, 0, 1970-01-01T00:00:00Z",
            "-1, 999999999, 1969-12-31T23:59:59.999Z"
    })
    void convertsProtobufTimestampToAvroMilliseconds(long seconds, int nanos, String expectedTimestamp)
            throws Exception {
        UserActionProto request = validRequest()
                .setTimestamp(Timestamp.newBuilder().setSeconds(seconds).setNanos(nanos))
                .build();

        ListenableFuture<Empty> response = client.collectUserAction(request);

        assertThat(producer.history()).singleElement().satisfies(record -> {
            assertThat(record.value()).isInstanceOfSatisfying(UserActionAvro.class,
                    action -> assertThat(action.getTimestamp()).isEqualTo(Instant.parse(expectedTimestamp)));
        });
        assertThat(producer.completeNext()).isTrue();
        assertThat(response.get(5, TimeUnit.SECONDS)).isEqualTo(Empty.getDefaultInstance());
    }

    @ParameterizedTest(name = "{index} → {1}")
    @DisplayName("Rejects invalid requests before sending to Kafka")
    @MethodSource("invalidRequests")
    void rejectsInvalidRequestsWithoutSendingToKafka(UserActionProto request, String description) {
        assertRpcFailure(client.collectUserAction(request), Status.Code.INVALID_ARGUMENT, description);
        verifyNoInteractions(producer);
    }

    @ParameterizedTest
    @DisplayName("Reports synchronous Kafka errors after one send attempt")
    @MethodSource("kafkaFailures")
    void reportsSynchronousKafkaFailure(RuntimeException failure, Status.Code code) {
        producer.sendException = failure;

        assertRpcFailure(client.collectUserAction(validRequest().build()), code, KAFKA_ERROR);
        verify(producer, times(1)).send(any(), any());
    }

    @ParameterizedTest
    @DisplayName("Reports asynchronous Kafka errors after one send attempt")
    @MethodSource("kafkaFailures")
    void reportsAsynchronousKafkaFailure(RuntimeException failure, Status.Code code) {
        ListenableFuture<Empty> response = client.collectUserAction(validRequest().build());

        assertThat(response.isDone()).isFalse();
        assertThat(producer.errorNext(failure)).isTrue();
        assertRpcFailure(response, code, KAFKA_ERROR);
        verify(producer, times(1)).send(any(), any());
    }

    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of(validRequest().setUserId(0).build(), "user_id must be positive"),
                Arguments.of(validRequest().setUserId(-1).build(), "user_id must be positive"),
                Arguments.of(validRequest().setEventId(0).build(), "event_id must be positive"),
                Arguments.of(validRequest().setEventId(-1).build(), "event_id must be positive"),
                Arguments.of(validRequest().clearTimestamp().build(), "timestamp must be set"),
                Arguments.of(validRequest().setTimestamp(Timestamp.newBuilder().setNanos(-1)).build(),
                        "timestamp nanos must be between 0 and 999999999"),
                Arguments.of(validRequest().setTimestamp(Timestamp.newBuilder().setNanos(1_000_000_000)).build(),
                        "timestamp nanos must be between 0 and 999999999"),
                Arguments.of(validRequest().setActionTypeValue(99).build(), "Unrecognized Protobuf action type: 99")
        );
    }

    private static Stream<Arguments> kafkaFailures() {
        return Stream.of(
                Arguments.of(new SerializationException("Invalid Avro record"), Status.Code.INTERNAL),
                Arguments.of(new TimeoutException("Kafka send timed out"), Status.Code.UNAVAILABLE)
        );
    }

    private static UserActionProto.Builder validRequest() {
        return UserActionProto.newBuilder()
                .setUserId(USER_ID)
                .setEventId(EVENT_ID)
                .setActionType(ActionTypeProto.ACTION_VIEW)
                .setTimestamp(Timestamp.newBuilder().setSeconds(1_700_000_000).setNanos(123_456_789));
    }

    private static void assertRpcFailure(ListenableFuture<Empty> response, Status.Code code, String description) {
        assertThatThrownBy(() -> response.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .cause()
                .isInstanceOfSatisfying(StatusRuntimeException.class, failure -> {
                    assertThat(failure.getStatus().getCode()).isEqualTo(code);
                    assertThat(failure.getStatus().getDescription()).isEqualTo(description);
                });
    }
}
