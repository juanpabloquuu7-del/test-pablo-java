package com.littlehorizons.erp.model;

/** Grado académico. {@code numericLevel} es 0 para Preescolar y 1 a 11 para Primero a Once. */
public record GradeLevel(int id, String name, int numericLevel) {
}
