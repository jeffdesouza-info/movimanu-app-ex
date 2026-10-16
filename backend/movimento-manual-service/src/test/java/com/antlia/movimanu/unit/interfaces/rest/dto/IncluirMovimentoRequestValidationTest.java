package com.antlia.movimanu.unit.interfaces.rest.dto;

import java.util.Set;
import java.util.stream.Collectors;

import com.antlia.movimanu.interfaces.rest.dto.IncluirMovimentoRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IncluirMovimentoRequestValidationTest {
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closesValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void acceptsInclusiveContractLimitsAndPreservesTheProvidedText() {
        IncluirMovimentoRequest minimum = request(1, 1, "A", "1", "A", "-0.01");
        IncluirMovimentoRequest maximum = request(
                12, 9999, "ABCD", "12345678901", "A".repeat(50), "9999999999999999.99");

        assertThat(VALIDATOR.validate(minimum)).isEmpty();
        assertThat(VALIDATOR.validate(maximum)).isEmpty();
        assertThat(maximum.getDescricao()).hasSize(50);
    }

    @Test
    void reportsAllRequiredFieldsWhenValuesAreAbsent() {
        assertThat(invalidFields(new IncluirMovimentoRequest()))
                .containsExactlyInAnyOrder("mes", "ano", "codigoProduto", "codigoCosif", "descricao", "valor");
    }

    @Test
    void rejectsValuesOutsideNumericAndTextLengthLimits() {
        IncluirMovimentoRequest request = request(0, 10000, "ABCDE", "123456789012", "A".repeat(51), "0");

        assertThat(invalidFields(request))
                .contains("mes", "ano", "codigoProduto", "codigoCosif", "descricao", "valor");

        request.setMes(13);
        request.setAno(0);
        request.setCodigoProduto(" ");
        request.setCodigoCosif("");
        request.setDescricao("   ");
        request.setValor("1.234");

        assertThat(invalidFields(request))
                .contains("mes", "ano", "codigoProduto", "codigoCosif", "descricao", "valor");
    }

    private Set<String> invalidFields(IncluirMovimentoRequest request) {
        return VALIDATOR.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private IncluirMovimentoRequest request(
            Integer month,
            Integer year,
            String productCode,
            String cosifCode,
            String description,
            String value) {
        IncluirMovimentoRequest request = new IncluirMovimentoRequest();
        request.setMes(month);
        request.setAno(year);
        request.setCodigoProduto(productCode);
        request.setCodigoCosif(cosifCode);
        request.setDescricao(description);
        request.setValor(value);
        return request;
    }
}
