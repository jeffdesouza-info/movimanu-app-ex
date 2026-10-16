package com.antlia.movimanu.application.port;

import java.util.List;

public record MovementPage<T>(List<T> content, int page, long totalElements) {
    public static final int SIZE = 6;

    public MovementPage {
        if (content == null) {
            throw new IllegalArgumentException("O conteúdo da página é obrigatório.");
        }
        content = List.copyOf(content);
        if (page < 0) {
            throw new IllegalArgumentException("A página não pode ser negativa.");
        }
        if (content.size() > SIZE) {
            throw new IllegalArgumentException("A página não pode conter mais de 6 movimentos.");
        }
        if (totalElements < content.size()) {
            throw new IllegalArgumentException("O total de movimentos não pode ser menor que o conteúdo da página.");
        }
    }

    public int size() {
        return SIZE;
    }

    public long totalPages() {
        return totalElements / SIZE + (totalElements % SIZE == 0 ? 0 : 1);
    }
}
