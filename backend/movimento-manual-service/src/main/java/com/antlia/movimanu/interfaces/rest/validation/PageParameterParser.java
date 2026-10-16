package com.antlia.movimanu.interfaces.rest.validation;

import com.antlia.movimanu.interfaces.rest.exception.ApiValidationException;

public final class PageParameterParser {
    private PageParameterParser() {
    }

    public static int parse(String page) {
        if (page == null) {
            return 0;
        }
        try {
            if (!page.matches("[0-9]+")) {
                throw new NumberFormatException("A página deve conter somente dígitos.");
            }
            return Integer.parseInt(page);
        } catch (NumberFormatException exception) {
            throw new ApiValidationException("page", "A página deve ser um inteiro não negativo.");
        }
    }
}
