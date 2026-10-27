package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** Beca parcial de un estudiante en un año académico. */
public record Scholarship(int id, int studentId, int academicYear, BigDecimal percent, String description) {
}
