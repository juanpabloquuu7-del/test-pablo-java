package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** Concepto del catálogo de pagos. {@code recurring}: pago periódico (pensión), el único que admite descuentos. */
public record PaymentConcept(int id, String code, String name, String description, BigDecimal baseAmount,
                             boolean recurring, boolean active) {
}
