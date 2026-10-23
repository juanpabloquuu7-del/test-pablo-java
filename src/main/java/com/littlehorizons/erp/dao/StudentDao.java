package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Student;

import java.util.List;
import java.util.Optional;

public interface StudentDao {

    int insert(Student student);

    int update(Student student);

    Optional<Student> findById(int id);

    Optional<Student> findByDocument(String documentNumber);

    /** Busca por fragmento del nombre completo o del documento (máx. 50 resultados). */
    List<Student> search(String text);

    /** Estudiantes con matrícula Activa en el grado y año dados. */
    List<Student> findByGrade(int gradeId, int year);

    int updateStatus(int studentId, String status);
}
