package com.antlia.movimanu.unit.infrastructure.persistence.converter;

import java.time.Instant;
import java.time.LocalDateTime;

import com.antlia.movimanu.infrastructure.persistence.converter.SmallDateTimeConverter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SmallDateTimeConverterTest {

    private final SmallDateTimeConverter converter = new SmallDateTimeConverter();

    @Test
    void roundsToTheNearestMinuteAtTheSqlServerThresholdAndAcrossDayBoundary() {
        assertThat(SmallDateTimeConverter.normalize(Instant.parse("2025-02-03T12:34:29.998999999Z")))
                .isEqualTo(Instant.parse("2025-02-03T12:34:00Z"));
        assertThat(SmallDateTimeConverter.normalize(Instant.parse("2025-02-03T12:34:29.999Z")))
                .isEqualTo(Instant.parse("2025-02-03T12:35:00Z"));
        assertThat(SmallDateTimeConverter.normalize(Instant.parse("2025-02-03T12:34:30Z")))
                .isEqualTo(Instant.parse("2025-02-03T12:35:00Z"));
        assertThat(SmallDateTimeConverter.normalize(Instant.parse("2024-12-31T23:59:30Z")))
                .isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
    }

    @Test
    void acceptsTheSupportedDateRangeAndConvertsBothDirectionsInUtc() {
        assertThat(SmallDateTimeConverter.normalize(Instant.parse("1900-01-01T00:00:00Z")))
                .isEqualTo(Instant.parse("1900-01-01T00:00:00Z"));
        assertThat(SmallDateTimeConverter.normalize(Instant.parse("2079-06-06T23:59:29.998999999Z")))
                .isEqualTo(Instant.parse("2079-06-06T23:59:00Z"));
        assertThat(converter.convertToDatabaseColumn(Instant.parse("2025-02-03T12:34:29.999Z")))
                .isEqualTo(LocalDateTime.parse("2025-02-03T12:35:00"));
        assertThat(converter.convertToEntityAttribute(LocalDateTime.parse("2025-02-03T12:34:45")))
                .isEqualTo(Instant.parse("2025-02-03T12:35:00Z"));
    }

    @Test
    void rejectsInstantsOutsideTheRangeAndValuesThatRoundPastItsUpperEnd() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SmallDateTimeConverter.normalize(Instant.parse("1899-12-31T23:59:59Z")));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SmallDateTimeConverter.normalize(Instant.parse("2079-06-07T00:00:00Z")));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SmallDateTimeConverter.normalize(Instant.parse("2079-06-06T23:59:29.999Z")));
    }

    @Test
    void preservesNullValuesInBothJpaConversionDirections() {
        assertThat(SmallDateTimeConverter.normalize(null)).isNull();
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
