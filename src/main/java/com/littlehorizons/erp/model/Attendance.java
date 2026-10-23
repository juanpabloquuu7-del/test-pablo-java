package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Asistencia. {@code status}: Presente, Ausente, Tardanza o Excusa Médica. {@code registeredBy}: id de profesor. */
public record Attendance(
        int id,
        int studentId,
        int subjectId,
        LocalDate date,
        String status,
        String observations,
        Integer registeredBy) {
}
