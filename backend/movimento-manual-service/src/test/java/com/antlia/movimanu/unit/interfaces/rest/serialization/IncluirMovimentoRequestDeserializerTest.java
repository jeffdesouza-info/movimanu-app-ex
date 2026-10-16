package com.antlia.movimanu.unit.interfaces.rest.serialization;

import com.antlia.movimanu.interfaces.rest.dto.IncluirMovimentoRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JacksonException.Reference;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncluirMovimentoRequestDeserializerTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void deserializesTheDeclaredJsonTypesWithoutCoercion() throws Exception {
        IncluirMovimentoRequest request = mapper.readValue(
                """
                {"mes":12,"ano":9999,"codigoProduto":"0001","codigoCosif":"10000000001",
                 "descricao":"  Ajuste  ","valor":"-9999999999999999.99"}
                """,
                IncluirMovimentoRequest.class);

        assertThat(request.getMes()).isEqualTo(12);
        assertThat(request.getAno()).isEqualTo(9999);
        assertThat(request.getCodigoProduto()).isEqualTo("0001");
        assertThat(request.getCodigoCosif()).isEqualTo("10000000001");
        assertThat(request.getDescricao()).isEqualTo("  Ajuste  ");
        assertThat(request.getValor()).isEqualTo("-9999999999999999.99");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "mes|\"1\"",
        "ano|2026.5",
        "codigoProduto|1",
        "codigoCosif|true",
        "descricao|{}",
        "valor|150.00"
    })
    void attachesTheIncompatibleInputPathToTheDtoField(String field, String incompatibleValue) {
        String json = replaceFieldValue(field, incompatibleValue);

        assertThatThrownBy(() -> mapper.readValue(json, IncluirMovimentoRequest.class))
                .isInstanceOf(MismatchedInputException.class)
                .satisfies(exception -> assertThat(((JacksonException) exception).getPath())
                        .extracting(Reference::getPropertyName)
                        .containsExactly(field));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "mes|2147483648",
        "mes|-2147483649",
        "ano|2147483648",
        "ano|-2147483649"
    })
    void attachesTheDtoFieldWhenAnIntegerValueExceedsTheIntRange(String field, String value) {
        String json = replaceFieldValue(field, value);

        assertThatThrownBy(() -> mapper.readValue(json, IncluirMovimentoRequest.class))
                .isInstanceOf(MismatchedInputException.class)
                .satisfies(exception -> assertThat(((JacksonException) exception).getPath())
                        .extracting(Reference::getPropertyName)
                        .containsExactly(field));
    }

    @Test
    void leavesThePathEmptyForAnUnknownProperty() {
        String json = """
                {"mes":10,"ano":2026,"codigoProduto":"0001","codigoCosif":"10000000001",
                 "descricao":"Ajuste","valor":"150.00","campoExtra":true}
                """;

        assertThatThrownBy(() -> mapper.readValue(json, IncluirMovimentoRequest.class))
                .isInstanceOf(MismatchedInputException.class)
                .satisfies(exception -> assertThat(((JacksonException) exception).getPath()).isEmpty());
    }

    private String replaceFieldValue(String field, String value) {
        String json = """
                {"mes":10,"ano":2026,"codigoProduto":"0001","codigoCosif":"10000000001",
                 "descricao":"Ajuste","valor":"150.00"}
                """;
        String originalValue = switch (field) {
            case "mes" -> "10";
            case "ano" -> "2026";
            case "codigoProduto" -> "\"0001\"";
            case "codigoCosif" -> "\"10000000001\"";
            case "descricao" -> "\"Ajuste\"";
            case "valor" -> "\"150.00\"";
            default -> throw new IllegalArgumentException("Campo de teste não previsto.");
        };
        return json.replace("\"" + field + "\":" + originalValue, "\"" + field + "\":" + value);
    }
}
