package com.littlehorizons.erp.model;

/**
 * Club extracurricular. {@code id} es 0 mientras no se haya guardado; {@code supervisorId} puede ser null;
 * {@code activeMembers} solo se llena al leer de la base de datos.
 */
public record Club(int id, String name, String description, Integer supervisorId, String supervisorName,
                   int maxCapacity, String schedule, boolean active, int activeMembers) {
}
