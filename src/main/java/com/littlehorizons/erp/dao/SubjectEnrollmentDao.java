package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SubjectEnrollment;

import java.util.List;

public interface SubjectEnrollmentDao {

    int insert(SubjectEnrollment enrollment);

    /** Inserta todas en una sola transacción. */
    void insertAll(List<SubjectEnrollment> enrollments);

    List<SubjectEnrollment> findByStudent(int studentId, int year, String period);

    /** Inscripciones Activas de una materia en un período; {@code section} puede ser null para todas. */
    List<SubjectEnrollment> findByCourse(int subjectId, int year, String period, String section);

    boolean exists(int studentId, int subjectId, int year, String period);

    /** True si el estudiante está inscrito en la materia en algún período I a IV del año. */
    boolean existsInYear(int studentId, int subjectId, int year);

    int updateStatus(int enrollmentId, String status);
}
