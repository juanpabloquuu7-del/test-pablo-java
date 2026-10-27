package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Factura electrónica asociada a un pago; {@code fileName} y {@code filePath} son null hasta generar el PDF. */
public record Invoice(int id, String number, int paymentId, LocalDate issueDate, String cufe,
                      String fileName, String filePath) {
}
