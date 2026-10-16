package com.antlia.movimanu.interfaces.rest.dto;

import com.antlia.movimanu.interfaces.rest.serialization.IncluirMovimentoRequestDeserializer;
import com.antlia.movimanu.interfaces.rest.validation.ValidMovementValue;
import tools.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonDeserialize(using = IncluirMovimentoRequestDeserializer.class)
public class IncluirMovimentoRequest {
    @NotNull(message = "O mês é obrigatório.")
    @Min(value = 1, message = "O mês deve estar entre 1 e 12.")
    @Max(value = 12, message = "O mês deve estar entre 1 e 12.")
    private Integer mes;

    @NotNull(message = "O ano é obrigatório.")
    @Min(value = 1, message = "O ano deve ser positivo e ter até quatro dígitos.")
    @Max(value = 9999, message = "O ano deve ser positivo e ter até quatro dígitos.")
    private Integer ano;

    @NotBlank(message = "O código do produto é obrigatório.")
    @Size(max = 4, message = "O código do produto deve ter até quatro caracteres.")
    private String codigoProduto;

    @NotBlank(message = "O código COSIF é obrigatório.")
    @Size(max = 11, message = "O código COSIF deve ter até onze caracteres.")
    private String codigoCosif;

    @NotNull(message = "A descrição é obrigatória.")
    @NotBlank(message = "A descrição deve conter ao menos um caractere diferente de espaço.")
    @Size(max = 50, message = "A descrição deve ter até 50 caracteres.")
    private String descricao;

    @NotNull(message = "O valor é obrigatório.")
    @ValidMovementValue
    private String valor;

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public String getCodigoProduto() {
        return codigoProduto;
    }

    public void setCodigoProduto(String codigoProduto) {
        this.codigoProduto = codigoProduto;
    }

    public String getCodigoCosif() {
        return codigoCosif;
    }

    public void setCodigoCosif(String codigoCosif) {
        this.codigoCosif = codigoCosif;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

}
