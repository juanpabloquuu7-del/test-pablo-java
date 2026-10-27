package com.littlehorizons.erp.model;

/** Acudiente junto con su relación con un estudiante. Un {@code guardian.id()} de 0 indica un acudiente nuevo. */
public record GuardianLink(Guardian guardian, boolean primary, boolean emergencyContact) {
}
