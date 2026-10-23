package com.littlehorizons.erp.model;

import java.time.LocalTime;

/** Horario de una asignación. {@code day}: Lunes, Martes, Miércoles, Jueves, Viernes o Sábado. */
public record ClassSchedule(int id, int assignmentId, String day, LocalTime startTime, LocalTime endTime) {
}
