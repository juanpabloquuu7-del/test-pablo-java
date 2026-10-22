package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.TeacherRef;

import java.util.Optional;

/** Consultas de solo lectura sobre profesores (tablas del Área A). */
public interface TeacherLookupDao {

    Optional<TeacherRef> findById(int id);

    /** Solo si el profesor existe y su estado es Activo. */
    Optional<TeacherRef> findActiveById(int id);
}
