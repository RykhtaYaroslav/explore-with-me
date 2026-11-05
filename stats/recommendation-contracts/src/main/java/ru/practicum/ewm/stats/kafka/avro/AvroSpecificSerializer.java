package ru.practicum.ewm.stats.kafka.avro;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroSpecificSerializer implements Serializer<SpecificRecord> {
    @Override
    public byte[] serialize(String topic, SpecificRecord data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(output, null);
            new SpecificDatumWriter<SpecificRecord>(data.getSchema()).write(data, encoder);
            encoder.flush();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new SerializationException("Failed to serialize Avro record for topic " + topic, exception);
        }
    }
}
