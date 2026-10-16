package com.antlia.movimanu.unit.interfaces.rest.exception;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import com.antlia.movimanu.application.exception.ProductNotFoundException;
import com.antlia.movimanu.interfaces.rest.dto.IncluirMovimentoRequest;
import com.antlia.movimanu.interfaces.rest.exception.ApiFieldError;
import com.antlia.movimanu.interfaces.rest.exception.ApiValidationException;
import com.antlia.movimanu.interfaces.rest.exception.RestExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class RestExceptionHandlerTest {
    private static final String API_PATH = "/api/v1/movimentos-manuais";

    private final RestExceptionHandler handler = new RestExceptionHandler();
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void mapsBeanValidationErrorsToUniqueDtoFields() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                new IncluirMovimentoRequest(), "request");
        bindingResult.addError(new FieldError("request", "mes", "Mês obrigatório."));
        bindingResult.addError(new FieldError("request", "mes", "Mês inválido."));
        bindingResult.addError(new FieldError("request", "ano", "Ano obrigatório."));
        Method submit = Endpoint.class.getDeclaredMethod("submit", IncluirMovimentoRequest.class);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new MethodParameter(submit, 0), bindingResult);

        var response = handler.handleInvalidFields(exception, request());

        assertProblem(response.getBody(), HttpStatus.BAD_REQUEST, "Requisição inválida", API_PATH);
        assertThat(errors(response.getBody())).containsExactly(
                new ApiFieldError("mes", "Mês obrigatório."),
                new ApiFieldError("ano", "Ano obrigatório."));
    }

    @Test
    void preservesTheDtoFieldOnUnreadableJsonWithAnIdentifiablePath() {
        String json = """
                {"mes":"10","ano":2026,"codigoProduto":"0001","codigoCosif":"10000000001",
                 "descricao":"Ajuste","valor":"150.00"}
                """;
        Throwable cause = catchThrowable(() -> mapper.readValue(json, IncluirMovimentoRequest.class));
        assertThat(cause).isInstanceOf(JacksonException.class);
        var unreadable = new HttpMessageNotReadableException(
                "JSON inválido", cause, new MockHttpInputMessage(new byte[0]));

        var response = handler.handleUnreadableRequest(unreadable, request());

        assertProblem(response.getBody(), HttpStatus.BAD_REQUEST, "Requisição inválida", API_PATH);
        assertThat(errors(response.getBody()))
                .containsExactly(new ApiFieldError("mes", "O tipo informado é incompatível com o campo."));
    }

    @Test
    void leavesErrorsEmptyWhenUnreadableJsonHasNoIdentifiableDtoField() {
        var unreadable = new HttpMessageNotReadableException(
                "JSON inválido", new MockHttpInputMessage(new byte[0]));

        var response = handler.handleUnreadableRequest(unreadable, request());

        assertProblem(response.getBody(), HttpStatus.BAD_REQUEST, "Requisição inválida", API_PATH);
        assertThat(errors(response.getBody())).isEmpty();
    }

    @Test
    void leavesErrorsEmptyWhenTheJacksonPathDoesNotIdentifyARequestField() throws Exception {
        try (JsonParser parser = mapper.createParser("true")) {
            MismatchedInputException cause = MismatchedInputException.from(
                    parser, IncluirMovimentoRequest.class, "Propriedade não reconhecida.");
            cause.prependPath(IncluirMovimentoRequest.class, "campoExtra");
            var unreadable = new HttpMessageNotReadableException(
                    "JSON inválido", cause, new MockHttpInputMessage(new byte[0]));

            var response = handler.handleUnreadableRequest(unreadable, request());

            assertProblem(response.getBody(), HttpStatus.BAD_REQUEST, "Requisição inválida", API_PATH);
            assertThat(errors(response.getBody())).isEmpty();
        }
    }

    @Test
    void mapsApiValidationAndMissingProductToTheirContractedFieldsAndStatuses() {
        var invalidPage = handler.handleApiValidation(
                new ApiValidationException("page", "A página deve ser válida."), request());
        var missingProduct = handler.handleProductNotFound(new ProductNotFoundException("9999"), request());

        assertProblem(invalidPage.getBody(), HttpStatus.BAD_REQUEST, "Requisição inválida", API_PATH);
        assertThat(errors(invalidPage.getBody()))
                .containsExactly(new ApiFieldError("page", "A página deve ser válida."));
        assertProblem(missingProduct.getBody(), HttpStatus.NOT_FOUND, "Recurso não encontrado", API_PATH);
        assertThat(errors(missingProduct.getBody()))
                .containsExactly(new ApiFieldError("codigoProduto", "O produto informado não existe."));
    }

    @Test
    void mapsSequenceConflictsAndUnexpectedFailuresWithoutFieldErrorsOrInternalDetails() {
        var conflict = handler.handleSequenceConflict(request());
        var unexpected = handler.handleUnexpectedFailure(request());

        assertProblem(conflict.getBody(), HttpStatus.CONFLICT, "Conflito de inclusão", API_PATH);
        assertThat(errors(conflict.getBody())).isEmpty();
        assertProblem(unexpected.getBody(), HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", API_PATH);
        assertThat(errors(unexpected.getBody())).isEmpty();
        assertThat(unexpected.getBody().getDetail()).doesNotContain("IllegalStateException");
    }

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest("POST", API_PATH);
    }

    private void assertProblem(ProblemDetail problem, HttpStatus status, String title, String instance) {
        assertThat(problem.getType()).isEqualTo(URI.create("about:blank"));
        assertThat(problem.getStatus()).isEqualTo(status.value());
        assertThat(problem.getTitle()).isEqualTo(title);
        assertThat(problem.getDetail()).isNotBlank();
        assertThat(problem.getInstance()).isEqualTo(URI.create(instance));
        assertThat(problem.getProperties()).containsKey("errors");
    }

    @SuppressWarnings("unchecked")
    private List<ApiFieldError> errors(ProblemDetail problem) {
        return (List<ApiFieldError>) problem.getProperties().get("errors");
    }

    private static class Endpoint {
        public void submit(IncluirMovimentoRequest request) {
        }
    }
}
