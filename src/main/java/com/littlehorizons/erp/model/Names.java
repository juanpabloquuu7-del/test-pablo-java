package com.littlehorizons.erp.model;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Utilidad del Área A para armar nombres completos omitiendo las partes vacías. */
public final class Names {

    private Names() {
    }

    public static String fullName(String firstName, String middleName, String lastName, String secondLastName) {
        return Stream.of(firstName, middleName, lastName, secondLastName)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }
}
