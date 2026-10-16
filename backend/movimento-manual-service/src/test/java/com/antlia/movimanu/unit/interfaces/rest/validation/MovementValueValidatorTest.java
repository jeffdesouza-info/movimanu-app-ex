package com.antlia.movimanu.unit.interfaces.rest.validation;

import com.antlia.movimanu.interfaces.rest.validation.MovementValueValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class MovementValueValidatorTest {
    private final MovementValueValidator validator = new MovementValueValidator();

    @Test
    void acceptsNullSoTheRequiredConstraintCanReportItsOwnMessage() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"150", "150.5", "150.50", "-150.00", "9999999999999999.99"})
    void acceptsContractValues(String value) {
        assertThat(validator.isValid(value, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "0", "0.00", "-0.00", "1,23", "1.234", "1.234,56", "1,234.56", "1 234.56", "1e3", "1E3",
        ".50", "150.", "10000000000000000.00", " 150.00", "$150.00"
    })
    void rejectsValuesOutsideTheCompleteDecimalContract(String value) {
        assertThat(validator.isValid(value, null)).isFalse();
    }
}
