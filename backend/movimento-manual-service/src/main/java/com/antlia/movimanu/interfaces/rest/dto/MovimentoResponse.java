package com.antlia.movimanu.interfaces.rest.dto;

public record MovimentoResponse(
        int mes,
        int ano,
        String codigoProduto,
        String codigoCosif,
        String descricao,
        String valor,
        String descricaoProduto,
        String numeroLancamento,
        String dataMovimento,
        String codigoUsuario) {
}
