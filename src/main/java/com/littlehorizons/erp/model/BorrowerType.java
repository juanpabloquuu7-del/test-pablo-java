package com.littlehorizons.erp.model;

/** Quién toma prestado un recurso; cada tipo tiene su propia tabla de préstamos. */
public enum BorrowerType {
    STUDENT("Estudiante"),
    TEACHER("Profesor");

    private final String label;

    BorrowerType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
