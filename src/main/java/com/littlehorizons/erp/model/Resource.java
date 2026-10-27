package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Recurso del inventario. {@code id} es 0 mientras no se haya guardado. */
public record Resource(int id, String code, String name, int categoryId, String categoryName,
                       String description, LocalDate acquisitionDate, String location, ResourceStatus status) {
}
