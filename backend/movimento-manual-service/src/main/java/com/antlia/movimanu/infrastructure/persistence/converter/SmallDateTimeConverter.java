package com.antlia.movimanu.infrastructure.persistence.converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SmallDateTimeConverter implements AttributeConverter<Instant, LocalDateTime> {

    private static final LocalDateTime MINIMUM = LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime MAXIMUM_INPUT = LocalDateTime.of(2079, 6, 6, 23, 59, 59, 999_999_999);
    private static final int ROUND_UP_NANO_THRESHOLD = 999_000_000;

    @Override
    public LocalDateTime convertToDatabaseColumn(Instant attribute) {
        if (attribute == null) {
            return null;
        }
        return LocalDateTime.ofInstant(normalize(attribute), ZoneOffset.UTC);
    }

    @Override
    public Instant convertToEntityAttribute(LocalDateTime dbData) {
        if (dbData == null) {
            return null;
        }
        return normalize(dbData.toInstant(ZoneOffset.UTC));
    }

    public static Instant normalize(Instant value) {
        if (value == null) {
            return null;
        }

        LocalDateTime utcValue = LocalDateTime.ofInstant(value, ZoneOffset.UTC);
        if (!isWithinRange(utcValue)) {
            throw new IllegalArgumentException("DAT_MOVIMENTO deve estar entre 1900-01-01 e 2079-06-06.");
        }

        LocalDateTime minuteValue = utcValue.truncatedTo(ChronoUnit.MINUTES);
        if (roundsUp(utcValue)) {
            minuteValue = minuteValue.plusMinutes(1);
        }
        if (!isWithinRange(minuteValue)) {
            throw new IllegalArgumentException("DAT_MOVIMENTO arredondado excede a faixa SMALLDATETIME.");
        }
        return minuteValue.toInstant(ZoneOffset.UTC);
    }

    private static boolean isWithinRange(LocalDateTime value) {
        return !value.isBefore(MINIMUM) && !value.isAfter(MAXIMUM_INPUT);
    }

    private static boolean roundsUp(LocalDateTime value) {
        return value.getSecond() >= 30
                || value.getSecond() == 29 && value.getNano() >= ROUND_UP_NANO_THRESHOLD;
    }
}
