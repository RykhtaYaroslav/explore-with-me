package ru.practicum.core.compilation.util;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.FieldPredicates;
import org.jeasy.random.ObjectCreationException;
import org.jeasy.random.api.ObjectFactory;
import org.jeasy.random.api.RandomizerContext;
import org.jeasy.random.randomizers.misc.BooleanRandomizer;
import org.jeasy.random.randomizers.range.IntegerRangeRandomizer;
import org.jeasy.random.randomizers.range.LongRangeRandomizer;
import org.objenesis.ObjenesisStd;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@UtilityClass
public class TestDataUtils {
    private static final long SEED = 0L;

    public static EasyRandom createEasyRandomizer() {
        EasyRandomParameters parameters = new EasyRandomParameters()
                .seed(SEED)
                .stringLengthRange(20, 100)
                .charset(StandardCharsets.UTF_8)
                .dateRange(LocalDate.now().minusDays(30), LocalDate.now().plusDays(100))
                .randomize(Integer.class, new IntegerRangeRandomizer(1, 100))
                .randomize(Long.class, new LongRangeRandomizer(1L, 1000L))
                .randomize(FieldPredicates.named("paid"), new BooleanRandomizer())
                .randomize(FieldPredicates.named("participantLimit"), new IntegerRangeRandomizer(1, 1000))
                .randomize(FieldPredicates.named("requestModeration"), new BooleanRandomizer())
                .excludeField(FieldPredicates.named("id"))
                .collectionSizeRange(1, 10)
                .objectFactory(new CustomObjectFactory());
        return new EasyRandom(parameters);
    }

    private static class CustomObjectFactory implements ObjectFactory {
        private final ObjenesisStd objenesis = new ObjenesisStd();

        @Override
        public <T> T createInstance(Class<T> type, RandomizerContext context) throws ObjectCreationException {
            return objenesis.newInstance(type);
        }
    }
}
