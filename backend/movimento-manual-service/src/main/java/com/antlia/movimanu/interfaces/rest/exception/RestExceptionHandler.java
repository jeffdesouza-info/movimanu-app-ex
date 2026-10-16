package com.antlia.movimanu.interfaces.rest.exception;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.antlia.movimanu.application.exception.ManualMovementSequenceConflictException;
import com.antlia.movimanu.application.exception.ProductNotFoundException;
import tools.jackson.core.JacksonException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {
    private static final Set<String> INPUT_FIELDS = Set.of(
            "mes", "ano", "codigoProduto", "codigoCosif", "descricao", "valor");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleInvalidFields(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, ApiFieldError> errorsByField = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errorsByField.putIfAbsent(error.getField(), toFieldError(error));
        }
        return problem(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida",
                "Revise os campos informados.",
                List.copyOf(errorsByField.values()),
                request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida",
                "O JSON contém formato inválido ou tipos incompatíveis com o contrato.",
                findInputFieldErrors(exception),
                request);
    }

    @ExceptionHandler(ApiValidationException.class)
    public ResponseEntity<ProblemDetail> handleApiValidation(
            ApiValidationException exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida",
                "Revise os dados informados.",
                List.of(new ApiFieldError(exception.field(), exception.getMessage())),
                request);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleProductNotFound(
            ProductNotFoundException exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Recurso não encontrado",
                "O produto informado não foi encontrado.",
                List.of(new ApiFieldError("codigoProduto", "O produto informado não existe.")),
                request);
    }

    @ExceptionHandler(ManualMovementSequenceConflictException.class)
    public ResponseEntity<ProblemDetail> handleSequenceConflict(HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                "Conflito de inclusão",
                "Não foi possível gerar um número de lançamento disponível.",
                List.of(),
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedFailure(HttpServletRequest request) {
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno",
                "Ocorreu um erro interno ao processar a requisição.",
                List.of(),
                request);
    }

    private ApiFieldError toFieldError(FieldError error) {
        return new ApiFieldError(error.getField(), error.getDefaultMessage());
    }

    private List<ApiFieldError> findInputFieldErrors(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof JacksonException jacksonException) {
                for (JacksonException.Reference reference : jacksonException.getPath()) {
                    String field = reference.getPropertyName();
                    if (INPUT_FIELDS.contains(field)) {
                        return List.of(new ApiFieldError(field, "O tipo informado é incompatível com o campo."));
                    }
                }
            }
        }
        return List.of();
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String title,
            String detail,
            List<ApiFieldError> errors,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errors", errors);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
