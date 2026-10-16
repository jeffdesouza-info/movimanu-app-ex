package com.antlia.movimanu.interfaces.rest.serialization;

import com.antlia.movimanu.interfaces.rest.dto.IncluirMovimentoRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

public class IncluirMovimentoRequestDeserializer extends ValueDeserializer<IncluirMovimentoRequest> {
    @Override
    public IncluirMovimentoRequest deserialize(JsonParser parser, DeserializationContext context)
            throws JacksonException {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            throw mismatch(parser, "O corpo da inclusão deve ser um objeto JSON.");
        }

        IncluirMovimentoRequest request = new IncluirMovimentoRequest();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            JsonToken valueToken = parser.nextToken();
            switch (field) {
                case "mes" -> request.setMes(readInteger(parser, valueToken, "mes"));
                case "ano" -> request.setAno(readInteger(parser, valueToken, "ano"));
                case "codigoProduto" -> request.setCodigoProduto(readString(parser, valueToken, "codigoProduto"));
                case "codigoCosif" -> request.setCodigoCosif(readString(parser, valueToken, "codigoCosif"));
                case "descricao" -> request.setDescricao(readString(parser, valueToken, "descricao"));
                case "valor" -> request.setValor(readString(parser, valueToken, "valor"));
                default -> throw mismatch(parser, "O corpo da inclusão contém uma propriedade não prevista.");
            }
        }
        return request;
    }

    private Integer readInteger(JsonParser parser, JsonToken token, String field) throws JacksonException {
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token != JsonToken.VALUE_NUMBER_INT) {
            throw mismatch(parser, field, "O campo deve ser um número inteiro no JSON.");
        }
        try {
            return parser.getIntValue();
        } catch (InputCoercionException exception) {
            throw mismatch(parser, field, "O valor informado excede o limite de um inteiro.");
        }
    }

    private String readString(JsonParser parser, JsonToken token, String field) throws JacksonException {
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token != JsonToken.VALUE_STRING) {
            throw mismatch(parser, field, "O campo deve ser texto no JSON.");
        }
        return parser.getString();
    }

    private MismatchedInputException mismatch(JsonParser parser, String message) {
        return MismatchedInputException.from(parser, IncluirMovimentoRequest.class, message);
    }

    private MismatchedInputException mismatch(JsonParser parser, String field, String message) {
        MismatchedInputException exception = mismatch(parser, message);
        exception.prependPath(IncluirMovimentoRequest.class, field);
        return exception;
    }
}
