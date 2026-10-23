package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Inscripción en una materia. {@code period}: I, II, III o IV. {@code status}: Activa, Retirada o Completada. */
public record SubjectEnrollment(
        int id,
        int studentId,
        int subjectId,
        int year,
        String period,
        String section,
        LocalDate enrollmentDate,
        String status) {
}
