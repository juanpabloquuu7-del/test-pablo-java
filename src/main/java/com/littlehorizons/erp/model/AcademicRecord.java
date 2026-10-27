package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Nota. {@code period}: I, II, III, IV o Final. {@code grade}: 0.0 a 5.0 con un decimal. */
public record AcademicRecord(
        int id,
        int studentId,
        int subjectId,
        int teacherId,
        int year,
        String period,
        BigDecimal grade,
        String observations,
        LocalDate recordDate) {
}
