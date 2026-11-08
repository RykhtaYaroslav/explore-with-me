package ru.practicum.ewm.stats.collector;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.devh.boot.grpc.server.serverfactory.GrpcServerFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.kafka.avro.AvroSpecificDeserializer;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.ewm.stats.proto.collector.UserActionControllerGrpc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.config.additional-location=classpath:/collector-config/collector.yaml",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "grpc.server.address=127.0.0.1"
})
@EmbeddedKafka(kraft = true, partitions = 1, topics = UserActionKafkaIntegrationTest.TOPIC,
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class UserActionKafkaIntegrationTest {
    static final String TOPIC = "stats.user-actions.v1";
    private static final long USER_ID = 42L;
    private static final long EVENT_ID = 73L;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private EmbeddedKafkaBroker broker;
    @Autowired
    private GrpcServerFactory grpcServerFactory;

    @ParameterizedTest
    @DisplayName("Publishes a gRPC action as Avro to the user actions topic")
    @CsvSource({"ACTION_VIEW, VIEW", "ACTION_REGISTER, REGISTER", "ACTION_LIKE, LIKE"})
    void publishesActionToKafka(ActionTypeProto protoType, ActionTypeAvro avroType) {
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(USER_ID)
                .setEventId(EVENT_ID)
                .setActionType(protoType)
                .setTimestamp(Timestamp.newBuilder().setSeconds(1_700_000_000).setNanos(123_456_789))
                .build();
        Map<String, Object> consumerProperties = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false
        );
        ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", grpcServerFactory.getPort())
                .usePlaintext()
                .build();

        try (KafkaConsumer<String, UserActionAvro> consumer = new KafkaConsumer<>(
                consumerProperties, new StringDeserializer(), new AvroSpecificDeserializer<>(UserActionAvro.class))) {
            List<TopicPartition> partitions = List.of(new TopicPartition(TOPIC, 0));
            consumer.assign(partitions);
            consumer.endOffsets(partitions, TIMEOUT).forEach(consumer::seek);

            Empty response = UserActionControllerGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                    .collectUserAction(request);

            assertThat(response).isEqualTo(Empty.getDefaultInstance());
            ConsumerRecord<String, UserActionAvro> record = KafkaTestUtils.getSingleRecord(consumer, TOPIC, TIMEOUT);
            assertThat(record.key()).isEqualTo(Long.toString(USER_ID));
            UserActionAvro action = record.value();
            assertThat(action.getUserId()).isEqualTo(USER_ID);
            assertThat(action.getEventId()).isEqualTo(EVENT_ID);
            assertThat(action.getActionType()).isEqualTo(avroType);
            assertThat(action.getTimestamp()).isEqualTo(Instant.parse("2023-11-14T22:13:20.123Z"));
        } finally {
            channel.shutdownNow();
        }
    }
}
