package com.littlehorizons.erp.model;

import java.time.LocalDate;

/**
 * Evento del colegio. {@code id} es 0 mientras no se haya guardado; {@code organizerId} puede ser null;
 * {@code participants} solo se llena al leer de la base de datos.
 */
public record SchoolEvent(int id, String name, String description, EventType type, LocalDate date,
                          String location, boolean competition, Integer organizerId, String organizerName,
                          int participants) {
}
