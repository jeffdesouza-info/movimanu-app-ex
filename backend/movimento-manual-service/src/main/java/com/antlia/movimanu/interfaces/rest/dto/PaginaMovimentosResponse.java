package com.antlia.movimanu.interfaces.rest.dto;

import java.util.List;

public record PaginaMovimentosResponse(
        List<MovimentoResponse> content,
        int page,
        int size,
        long totalElements,
        long totalPages) {
    public PaginaMovimentosResponse {
        content = List.copyOf(content);
    }
}
