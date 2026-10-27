package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.StudentRef;

import java.util.Optional;

/** Consultas de solo lectura sobre estudiantes (tablas del Área A). */
public interface StudentLookupDao {

    Optional<StudentRef> findById(int id);

    Optional<StudentRef> findByDocument(String document);

    /** Otros estudiantes con matrícula activa en {@code year} que comparten algún acudiente con este. */
    int countSiblings(int idStudent, int year);

    /** Nombre del grado en el que está matriculado el estudiante en {@code year}. */
    Optional<String> findGradeName(int idStudent, int year);
}
