package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.GuardianRef;

import java.util.List;
import java.util.Optional;

/** Consultas de solo lectura sobre acudientes (tablas del Área A). */
public interface GuardianLookupDao {

    Optional<GuardianRef> findById(int id);

    Optional<GuardianRef> findByDocument(String document);

    /** Acudiente principal del estudiante; si ninguno está marcado, el responsable financiero. */
    Optional<GuardianRef> findPrincipalGuardian(int idStudent);

    List<GuardianRef> findByStudent(int idStudent);

    boolean isGuardianOf(int idGuardian, int idStudent);
}
