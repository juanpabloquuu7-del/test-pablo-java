package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pago registrado. {@code period} es I, II, III, IV o Anual; {@code receipt} es el recibo consecutivo. */
public record Payment(int id, int studentId, String studentName, Integer guardianId, String guardianName,
                      int conceptId, String conceptCode, String conceptName, BigDecimal amount, LocalDate date,
                      PaymentMethod method, String receipt, int academicYear, String period, String notes) {
}
