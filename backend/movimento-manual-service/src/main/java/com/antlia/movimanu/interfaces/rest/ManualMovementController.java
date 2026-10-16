package com.antlia.movimanu.interfaces.rest;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.antlia.movimanu.application.exception.ProductCosifNotFoundException;
import com.antlia.movimanu.application.port.MovementPage;
import com.antlia.movimanu.application.projection.ManualMovementProjection;
import com.antlia.movimanu.application.usecase.FindManualMovementsUseCase;
import com.antlia.movimanu.application.usecase.IncludeManualMovementUseCase;
import com.antlia.movimanu.application.usecase.ListProductCosifsUseCase;
import com.antlia.movimanu.application.usecase.ListProductsUseCase;
import com.antlia.movimanu.domain.model.ManualMovement;
import com.antlia.movimanu.domain.model.MovementDraft;
import com.antlia.movimanu.domain.model.Product;
import com.antlia.movimanu.domain.model.ProductCosif;
import com.antlia.movimanu.interfaces.rest.dto.CosifResponse;
import com.antlia.movimanu.interfaces.rest.dto.IncluirMovimentoRequest;
import com.antlia.movimanu.interfaces.rest.dto.MovimentoResponse;
import com.antlia.movimanu.interfaces.rest.dto.PaginaMovimentosResponse;
import com.antlia.movimanu.interfaces.rest.dto.ProdutoResponse;
import com.antlia.movimanu.interfaces.rest.exception.ApiValidationException;
import com.antlia.movimanu.interfaces.rest.validation.PageParameterParser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ManualMovementController {
    private static final DateTimeFormatter UTC_DATE_TIME = DateTimeFormatter.ISO_INSTANT;

    private final ListProductsUseCase listProductsUseCase;
    private final ListProductCosifsUseCase listProductCosifsUseCase;
    private final FindManualMovementsUseCase findManualMovementsUseCase;
    private final IncludeManualMovementUseCase includeManualMovementUseCase;

    public ManualMovementController(
            ListProductsUseCase listProductsUseCase,
            ListProductCosifsUseCase listProductCosifsUseCase,
            FindManualMovementsUseCase findManualMovementsUseCase,
            IncludeManualMovementUseCase includeManualMovementUseCase) {
        this.listProductsUseCase = listProductsUseCase;
        this.listProductCosifsUseCase = listProductCosifsUseCase;
        this.findManualMovementsUseCase = findManualMovementsUseCase;
        this.includeManualMovementUseCase = includeManualMovementUseCase;
    }

    @GetMapping(path = "/produtos", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProdutoResponse> listProducts() {
        return listProductsUseCase.execute().stream()
                .map(product -> new ProdutoResponse(product.code(), product.description()))
                .toList();
    }

    @GetMapping(path = "/produtos/{codigoProduto}/cosifs", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CosifResponse> listCosifs(@PathVariable String codigoProduto) {
        return listProductCosifsUseCase.execute(codigoProduto).stream()
                .map(this::toCosifResponse)
                .toList();
    }

    @GetMapping(path = "/movimentos-manuais", produces = MediaType.APPLICATION_JSON_VALUE)
    public PaginaMovimentosResponse findMovements(@RequestParam(required = false) String page) {
        int pageNumber = PageParameterParser.parse(page);
        MovementPage<ManualMovementProjection> result = findManualMovementsUseCase.execute(pageNumber);
        return new PaginaMovimentosResponse(
                result.content().stream().map(this::toMovementResponse).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }

    @PostMapping(
            path = "/movimentos-manuais",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MovimentoResponse> includeMovement(@Valid @RequestBody IncluirMovimentoRequest request) {
        Product product = listProductsUseCase.execute().stream()
                .filter(candidate -> candidate.code().equals(request.getCodigoProduto()))
                .findFirst()
                .orElseThrow(() -> new ApiValidationException(
                        "codigoProduto",
                        "O produto informado não existe."));

        ManualMovement movement;
        try {
            movement = includeManualMovementUseCase.execute(new MovementDraft(
                    request.getMes(),
                    request.getAno(),
                    request.getCodigoProduto(),
                    request.getCodigoCosif(),
                    request.getDescricao(),
                    new BigDecimal(request.getValor())));
        } catch (ProductCosifNotFoundException exception) {
            throw new ApiValidationException("codigoCosif", "O COSIF informado não está vinculado ao produto.");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(toMovementResponse(movement, product.description()));
    }

    private CosifResponse toCosifResponse(ProductCosif productCosif) {
        return new CosifResponse(productCosif.id().cosifCode(), productCosif.classificationCode());
    }

    private MovimentoResponse toMovementResponse(ManualMovementProjection movement) {
        return new MovimentoResponse(
                movement.month(),
                movement.year(),
                movement.productCode(),
                movement.cosifCode(),
                movement.description(),
                movement.value().toPlainString(),
                movement.productDescription(),
                Long.toString(movement.launchNumber()),
                UTC_DATE_TIME.format(movement.movementAt()),
                movement.userCode());
    }

    private MovimentoResponse toMovementResponse(ManualMovement movement, String productDescription) {
        return new MovimentoResponse(
                movement.id().month(),
                movement.id().year(),
                movement.id().productCode(),
                movement.id().cosifCode(),
                movement.description(),
                movement.value().toPlainString(),
                productDescription,
                Long.toString(movement.id().launchNumber()),
                UTC_DATE_TIME.format(movement.movementAt()),
                movement.userCode());
    }
}
