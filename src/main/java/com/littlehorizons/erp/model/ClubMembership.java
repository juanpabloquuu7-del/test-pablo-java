package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Membresía de un estudiante en un club. {@code status}: Activa o Retirada. */
public record ClubMembership(int id, int studentId, String studentName, int clubId, String clubName,
                             LocalDate joinDate, String status, LocalDate leaveDate, String leaveReason) {
}
