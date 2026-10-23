package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.MenuModule;
import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.dao.AcademicRecordDaoImpl;
import com.littlehorizons.erp.dao.AttendanceDaoImpl;
import com.littlehorizons.erp.service.AcademicService;
import com.littlehorizons.erp.view.AcademicView;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.AssignmentDaoImpl;
import com.littlehorizons.erp.dao.TeacherDaoImpl;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.view.TeacherView;
import com.littlehorizons.erp.dao.EnrollmentDaoImpl;
import com.littlehorizons.erp.dao.GradeLevelDaoImpl;
import com.littlehorizons.erp.dao.GuardianDaoImpl;
import com.littlehorizons.erp.dao.StudentDaoImpl;
import com.littlehorizons.erp.dao.StudentRegistrationDaoImpl;
import com.littlehorizons.erp.dao.SubjectDaoImpl;
import com.littlehorizons.erp.dao.SubjectEnrollmentDaoImpl;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.view.StudentView;

import java.util.List;
import java.util.Scanner;

/**
 * Registro de módulos del Área A (Dev A): posiciones 1, 2 y 3 del menú.
 * Dueño: Dev A. Para registrar un módulo, agregue su instancia a la lista, por ejemplo:
 * {@code new StudentController(new StudentView(scanner), new StudentService(new StudentDaoImpl()))}.
 */
public final class AreaAModules {

    private AreaAModules() {
    }

    public static List<MenuModule> create(Scanner scanner) {
        return List.of(
                new StudentController(new StudentView(scanner), new StudentService(new StudentDaoImpl(),
                        new GuardianDaoImpl(), new EnrollmentDaoImpl(), new GradeLevelDaoImpl(),
                        new StudentRegistrationDaoImpl(), new SubjectDaoImpl(), new SubjectEnrollmentDaoImpl())),
                new AcademicController(new AcademicView(scanner), new AcademicService(new AcademicRecordDaoImpl(),
                        new AttendanceDaoImpl(), new StudentDaoImpl(), new SubjectDaoImpl(),
                        new SubjectEnrollmentDaoImpl(), new AssignmentDaoImpl())),
                new TeacherController(new TeacherView(scanner), new TeacherService(new TeacherDaoImpl(),
                        new AssignmentDaoImpl(), new SubjectDaoImpl()))
        );
    }
}
