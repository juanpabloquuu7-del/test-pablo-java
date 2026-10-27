package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Matrícula anual. {@code status}: Activa, Completada o Retirada. */
public record Enrollment(
        int id,
        int studentId,
        int gradeId,
        int year,
        String status,
        LocalDate enrollmentDate,
        LocalDate withdrawalDate,
        String notes) {
}
