package com.littlehorizons.erp.model;

/** Vista de solo lectura de un estudiante (dato del Área A) para uso del Área B. */
public record StudentRef(int id, String document, String fullName, boolean active) {
}
