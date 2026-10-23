package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Estudiante moroso: {@code lastPaymentOrEnrollment} es el último pago de pensión o, sin pagos, la matrícula. */
public record DelinquentStudent(int studentId, String studentName, String guardianName,
                                LocalDate lastPaymentOrEnrollment, long daysPending) {
}
