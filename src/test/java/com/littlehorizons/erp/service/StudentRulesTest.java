package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.GuardianLink;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentRulesTest {

    @Test
    void testReferenceAge() {
        assertEquals(5, StudentRules.referenceAgeForLevel(0));   // Preescolar
        assertEquals(6, StudentRules.referenceAgeForLevel(1));   // Primero
        assertEquals(11, StudentRules.referenceAgeForLevel(6));  // Sexto
        assertEquals(16, StudentRules.referenceAgeForLevel(11)); // Once
    }

    @Test
    void testAgeCalculationAtCutoff() {
        // Born 2020-06-06, academic year 2026 (cutoff 2026-03-31) -> 5 years old
        LocalDate birthDate = LocalDate.of(2020, 6, 6);
        assertEquals(5, StudentRules.calculateAgeAtCutoff(birthDate, 2026));

        // Born 2020-02-15, academic year 2026 (cutoff 2026-03-31) -> 6 years old
        LocalDate birthDate2 = LocalDate.of(2020, 2, 15);
        assertEquals(6, StudentRules.calculateAgeAtCutoff(birthDate2, 2026));

        assertEquals(-1, StudentRules.calculateAgeAtCutoff(null, 2026));
        assertEquals(-1, StudentRules.calculateAgeAtCutoff(birthDate, 0));
        assertEquals(-1, StudentRules.calculateAgeAtCutoff(birthDate, -2026));
    }

    @Test
    void testAgeConsistencyWithinTolerance() {
        int academicYear = 2026;
        int numericLevel = 1; // Primero -> ref age 6

        // Exact age 6 (born 2020-03-01) -> diff 0 -> ok
        assertTrue(StudentRules.isAgeConsistent(LocalDate.of(2020, 3, 1), numericLevel, academicYear));

        // Age 9 (born 2017-03-01) -> diff +3 -> ok
        assertTrue(StudentRules.isAgeConsistent(LocalDate.of(2017, 3, 1), numericLevel, academicYear));

        // Age 3 (born 2023-03-01) -> diff -3 -> ok
        assertTrue(StudentRules.isAgeConsistent(LocalDate.of(2023, 3, 1), numericLevel, academicYear));

        // Age 10 (born 2016-03-01) -> diff +4 -> false
        assertFalse(StudentRules.isAgeConsistent(LocalDate.of(2016, 3, 1), numericLevel, academicYear));

        // Age 2 (born 2024-03-01) -> diff -4 -> false
        assertFalse(StudentRules.isAgeConsistent(LocalDate.of(2024, 3, 1), numericLevel, academicYear));

        // Future birth date -> false
        assertFalse(StudentRules.isAgeConsistent(LocalDate.now().plusDays(1), numericLevel, academicYear));

        // Null birth date -> false
        assertFalse(StudentRules.isAgeConsistent(null, numericLevel, academicYear));

        // Invalid academic year / numeric level
        assertFalse(StudentRules.isAgeConsistent(LocalDate.of(2020, 3, 1), -1, academicYear));
        assertFalse(StudentRules.isAgeConsistent(LocalDate.of(2020, 3, 1), 12, academicYear));
        assertFalse(StudentRules.isAgeConsistent(LocalDate.of(2020, 3, 1), numericLevel, 0));

        // Birth date after cutoff
        assertFalse(StudentRules.isAgeConsistent(LocalDate.of(2026, 4, 1), numericLevel, 2026));
    }

    @Test
    void testValidateAgeConsistencyThrows() {
        assertThrows(BusinessException.class, () ->
                StudentRules.validateAgeConsistency(LocalDate.of(2010, 1, 1), 0, 2026));
        assertDoesNotThrow(() ->
                StudentRules.validateAgeConsistency(LocalDate.of(2021, 2, 1), 0, 2026));
    }

    @Test
    void testValidateBirthDate() {
        assertThrows(BusinessException.class, () -> StudentRules.validateBirthDate(null));
        assertThrows(BusinessException.class, () -> StudentRules.validateBirthDate(LocalDate.now()));
        assertThrows(BusinessException.class, () -> StudentRules.validateBirthDate(LocalDate.now().plusDays(1)));
        assertDoesNotThrow(() -> StudentRules.validateBirthDate(LocalDate.of(2015, 5, 20)));
    }

    @Test
    void testValidateStatusTransition() {
        // Valid transitions
        assertDoesNotThrow(() -> StudentRules.validateStatusTransition("Activo", "Suspendido", 5));
        assertDoesNotThrow(() -> StudentRules.validateStatusTransition("Activo", "Retirado", 5));
        assertDoesNotThrow(() -> StudentRules.validateStatusTransition("Suspendido", "Activo", 5));
        assertDoesNotThrow(() -> StudentRules.validateStatusTransition("Activo", "Graduado", 11)); // Once

        // Graduation from non-Once grade
        assertThrows(BusinessException.class, () ->
                StudentRules.validateStatusTransition("Activo", "Graduado", 10));

        // Once graduated, cannot change status
        assertThrows(BusinessException.class, () ->
                StudentRules.validateStatusTransition("Graduado", "Activo", 11));

        // Unknown status
        assertThrows(BusinessException.class, () ->
                StudentRules.validateStatusTransition("Activo", "Invalido", 5));
        assertThrows(BusinessException.class, () ->
                StudentRules.validateStatusTransition("Invalido", "Activo", 5));
        assertThrows(BusinessException.class, () ->
                StudentRules.validateStatusTransition(null, "Activo", 5));
        assertThrows(BusinessException.class, () ->
                StudentRules.validateStatusTransition("Activo", null, 5));
    }

    @Test
    void testValidateGuardians() {
        Guardian g1 = new Guardian(1, "CC", "12345678", "Juan", null, "Perez", null, "Padre", "3001234567", null, null, null, null, true);
        Guardian g2 = new Guardian(2, "CC", "87654321", "Maria", null, "Gomez", null, "Madre", "3007654321", null, null, null, null, true);

        // Empty / null list -> throws
        assertThrows(BusinessException.class, () -> StudentRules.validateGuardians(List.of()));
        assertThrows(BusinessException.class, () -> StudentRules.validateGuardians(null));

        // Null link or null guardian -> throws
        assertThrows(BusinessException.class, () -> StudentRules.validateGuardians(Collections.singletonList(null)));
        assertThrows(BusinessException.class, () -> StudentRules.validateGuardians(List.of(new GuardianLink(null, true, true))));

        // No primary -> throws
        assertThrows(BusinessException.class, () ->
                StudentRules.validateGuardians(List.of(
                        new GuardianLink(g1, false, true)
                )));

        // Two primaries -> throws
        assertThrows(BusinessException.class, () ->
                StudentRules.validateGuardians(List.of(
                        new GuardianLink(g1, true, true),
                        new GuardianLink(g2, true, false)
                )));

        // Exactly one primary -> ok
        assertDoesNotThrow(() ->
                StudentRules.validateGuardians(List.of(
                        new GuardianLink(g1, true, true),
                        new GuardianLink(g2, false, false)
                )));
    }

    @Test
    void testValidatePeriod() {
        assertDoesNotThrow(() -> StudentRules.validatePeriod("I"));
        assertDoesNotThrow(() -> StudentRules.validatePeriod("II"));
        assertDoesNotThrow(() -> StudentRules.validatePeriod("III"));
        assertDoesNotThrow(() -> StudentRules.validatePeriod("IV"));
        assertDoesNotThrow(() -> StudentRules.validatePeriod(" I "));

        assertThrows(BusinessException.class, () -> StudentRules.validatePeriod("Final"));
        assertThrows(BusinessException.class, () -> StudentRules.validatePeriod("V"));
        assertThrows(BusinessException.class, () -> StudentRules.validatePeriod(""));
        assertThrows(BusinessException.class, () -> StudentRules.validatePeriod(null));
    }

    @Test
    void testValidateCanEnrollSubjects() {
        assertDoesNotThrow(() -> StudentRules.validateCanEnrollSubjects("Activo"));
        assertThrows(BusinessException.class, () -> StudentRules.validateCanEnrollSubjects("Suspendido"));
        assertThrows(BusinessException.class, () -> StudentRules.validateCanEnrollSubjects("Retirado"));
        assertThrows(BusinessException.class, () -> StudentRules.validateCanEnrollSubjects("Graduado"));
        assertThrows(BusinessException.class, () -> StudentRules.validateCanEnrollSubjects(null));
    }
}
