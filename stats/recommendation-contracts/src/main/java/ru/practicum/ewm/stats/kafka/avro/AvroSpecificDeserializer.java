package ru.practicum.ewm.stats.kafka.avro;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;

public class AvroSpecificDeserializer<T extends SpecificRecord> implements Deserializer<T> {
    private final Class<T> recordType;

    public AvroSpecificDeserializer(Class<T> recordType) {
        this.recordType = recordType;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            SpecificDatumReader<T> reader = new SpecificDatumReader<>(SpecificData.get().getSchema(recordType));
            return reader.read(null, decoder);
        } catch (IOException exception) {
            throw new SerializationException("Failed to deserialize Avro record for topic " + topic, exception);
        }
    }
}
