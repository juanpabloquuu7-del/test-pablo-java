package com.littlehorizons.erp.model;

/** Participación de un estudiante en un evento. {@code position} es null si el evento no tiene competencia. */
public record EventParticipant(int id, int eventId, int studentId, String studentName, String role,
                               String position) {
}
