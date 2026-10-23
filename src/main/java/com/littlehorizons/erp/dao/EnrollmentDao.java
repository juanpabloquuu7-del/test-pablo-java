package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Enrollment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EnrollmentDao {

    int insert(Enrollment enrollment);

    Optional<Enrollment> findByStudentAndYear(int studentId, int year);

    /** Historial de matrículas, la más reciente primero. */
    List<Enrollment> findByStudent(int studentId);

    int updateGrade(int enrollmentId, int gradeId);

    int updateStatus(int enrollmentId, String status, LocalDate withdrawalDate);
}
