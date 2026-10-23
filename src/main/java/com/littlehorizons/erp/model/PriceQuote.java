package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/**
 * Valor sugerido de un concepto para un estudiante. Se aplica el mayor entre el descuento por hermanos y
 * la beca (no se suman); {@code appliedPercent} es ese mayor y {@code suggested} el monto resultante.
 */
public record PriceQuote(BigDecimal base, BigDecimal siblingPercent, BigDecimal scholarshipPercent,
                         BigDecimal appliedPercent, BigDecimal suggested) {
}
