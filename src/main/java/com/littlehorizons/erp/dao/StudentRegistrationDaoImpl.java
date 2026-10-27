package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.GuardianLink;
import com.littlehorizons.erp.model.Student;

import java.util.List;

public class StudentRegistrationDaoImpl extends BaseJdbcDao implements StudentRegistrationDao {

    @Override
    public int register(Student student, List<GuardianLink> guardians, Enrollment enrollment) {
        return inTransaction(c -> {
            int studentId = StudentDaoImpl.insert(c, student);

            for (GuardianLink link : guardians) {
                int guardianId = link.guardian().id();
                if (guardianId <= 0) {
                    guardianId = GuardianDaoImpl.insert(c, link.guardian());
                }
                GuardianDaoImpl.link(c, studentId, guardianId, link.primary(), link.emergencyContact());
            }

            Enrollment enrollmentWithStudentId = new Enrollment(
                    0,
                    studentId,
                    enrollment.gradeId(),
                    enrollment.year(),
                    enrollment.status() != null ? enrollment.status() : "Activa",
                    enrollment.enrollmentDate(),
                    enrollment.withdrawalDate(),
                    enrollment.notes()
            );
            EnrollmentDaoImpl.insert(c, enrollmentWithStudentId);

            return studentId;
        });
    }
}
