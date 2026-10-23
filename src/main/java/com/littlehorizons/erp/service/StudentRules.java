package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.GuardianLink;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;

public final class StudentRules {

    private static final Set<String> VALID_PERIODS = Set.of("I", "II", "III", "IV");
    private static final Set<String> VALID_STATUSES = Set.of("Activo", "Graduado", "Retirado", "Suspendido");

    private StudentRules() {
    }

    public static int calculateAgeAtCutoff(LocalDate birthDate, int academicYear) {
        if (birthDate == null || academicYear <= 0) {
            return -1;
        }
        LocalDate cutoff = LocalDate.of(academicYear, 3, 31);
        return Period.between(birthDate, cutoff).getYears();
    }

    public static int referenceAgeForLevel(int numericLevel) {
        return numericLevel + 5;
    }

    public static boolean isAgeConsistent(LocalDate birthDate, int numericLevel, int academicYear) {
        if (birthDate == null || !birthDate.isBefore(LocalDate.now())) {
            return false;
        }
        if (academicYear <= 0 || numericLevel < 0 || numericLevel > 11) {
            return false;
        }
        LocalDate cutoff = LocalDate.of(academicYear, 3, 31);
        if (birthDate.isAfter(cutoff)) {
            return false;
        }
        int age = calculateAgeAtCutoff(birthDate, academicYear);
        int ref = referenceAgeForLevel(numericLevel);
        return Math.abs(age - ref) <= 3;
    }

    public static void validateAgeConsistency(LocalDate birthDate, int numericLevel, int academicYear) {
        if (!isAgeConsistent(birthDate, numericLevel, academicYear)) {
            throw new BusinessException("La edad del estudiante no es coherente con el grado seleccionado.");
        }
    }

    public static void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null || !birthDate.isBefore(LocalDate.now())) {
            throw new BusinessException("La fecha de nacimiento debe ser anterior a la fecha actual.");
        }
    }

    public static void validateStatusTransition(String currentStatus, String targetStatus, int numericLevel) {
        if (targetStatus == null || !VALID_STATUSES.contains(targetStatus)) {
            throw new BusinessException("El estado especificado no es válido.");
        }
        if (currentStatus == null || !VALID_STATUSES.contains(currentStatus)) {
            throw new BusinessException("El estado actual del estudiante no es válido.");
        }
        if ("Graduado".equalsIgnoreCase(currentStatus)) {
            throw new BusinessException("Un estudiante graduado no puede cambiar de estado.");
        }
        if ("Graduado".equalsIgnoreCase(targetStatus) && numericLevel != 11) {
            throw new BusinessException("Solo los estudiantes de grado Once pueden graduarse.");
        }
    }

    public static void validateGuardians(List<GuardianLink> guardians) {
        if (guardians == null || guardians.isEmpty()) {
            throw new BusinessException("Todo estudiante debe tener al menos un acudiente.");
        }
        for (GuardianLink link : guardians) {
            if (link == null || link.guardian() == null) {
                throw new BusinessException("La información del acudiente no puede ser nula.");
            }
        }
        long primaryCount = guardians.stream().filter(GuardianLink::primary).count();
        if (primaryCount != 1) {
            throw new BusinessException("Debe haber exactamente un acudiente principal.");
        }
    }

    public static void validatePeriod(String period) {
        if (period == null || !VALID_PERIODS.contains(period.trim())) {
            throw new BusinessException("El período académico debe ser I, II, III o IV.");
        }
    }

    public static void validateCanEnrollSubjects(String studentStatus) {
        if (studentStatus == null) {
            throw new BusinessException("El estado del estudiante no es válido.");
        }
        if ("Suspendido".equalsIgnoreCase(studentStatus)) {
            throw new BusinessException("No se pueden inscribir materias a un estudiante suspendido.");
        }
        if (!"Activo".equalsIgnoreCase(studentStatus)) {
            throw new BusinessException("Solo se pueden inscribir materias a un estudiante con estado Activo.");
        }
    }
}
