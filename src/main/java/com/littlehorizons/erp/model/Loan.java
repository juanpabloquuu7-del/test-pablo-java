package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Préstamo de un recurso a un estudiante o a un profesor. {@code status}: Activo, Devuelto o Vencido. */
public record Loan(int id, int resourceId, String resourceCode, String resourceName,
                   BorrowerType borrowerType, int borrowerId, String borrowerName,
                   LocalDate loanDate, LocalDate expectedReturn, LocalDate actualReturn, String status) {
}
