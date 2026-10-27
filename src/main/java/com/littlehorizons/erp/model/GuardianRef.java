package com.littlehorizons.erp.model;

/** Vista de solo lectura de un acudiente (dato del Área A) para uso del Área B. */
public record GuardianRef(int id, String document, String fullName, String address) {
}
