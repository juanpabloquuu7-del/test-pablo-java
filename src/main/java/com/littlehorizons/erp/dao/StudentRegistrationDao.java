package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.GuardianLink;
import com.littlehorizons.erp.model.Student;

import java.util.List;

/** Registro atómico: estudiante, acudientes (nuevos o existentes), vínculos y matrícula en una sola transacción. */
public interface StudentRegistrationDao {

    /** Devuelve el id del estudiante. El {@code studentId} de la matrícula se ignora y se reemplaza por el nuevo. */
    int register(Student student, List<GuardianLink> guardians, Enrollment enrollment);
}
