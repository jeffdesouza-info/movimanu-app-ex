package com.antlia.movimanu.unit.interfaces.rest.validation;

import com.antlia.movimanu.interfaces.rest.exception.ApiValidationException;
import com.antlia.movimanu.interfaces.rest.validation.PageParameterParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageParameterParserTest {
    @Test
    void defaultsAnOmittedPageToZeroAndAcceptsTheIntegerBounds() {
        assertThat(PageParameterParser.parse(null)).isZero();
        assertThat(PageParameterParser.parse("0")).isZero();
        assertThat(PageParameterParser.parse("2147483647")).isEqualTo(Integer.MAX_VALUE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-1", "abc", "1.0", " 1", "2147483648"})
    void rejectsEmptyNegativeMalformedAndOverflowingPages(String page) {
        assertThatThrownBy(() -> PageParameterParser.parse(page))
                .isInstanceOfSatisfying(ApiValidationException.class, exception -> {
                    assertThat(exception.field()).isEqualTo("page");
                    assertThat(exception.getMessage()).contains("inteiro não negativo");
                });
    }
}
