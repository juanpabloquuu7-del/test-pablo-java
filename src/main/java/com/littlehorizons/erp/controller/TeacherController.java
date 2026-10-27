package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeachingAssignment;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.service.TeacherService.TeacherAssignmentDetail;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.view.TeacherView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class TeacherController extends BaseController implements MenuModule {

    private final TeacherView view;
    private final TeacherService service;

    public TeacherController(TeacherView view, TeacherService service) {
        super(view);
        this.view = view;
        this.service = service;
    }

    @Override
    public int position() {
        return 3;
    }

    @Override
    public void run() {
        while (true) {
            view.showMenu("Gestión de Profesores",
                    "Registrar profesor",
                    "Listar profesores",
                    "Asignar materia",
                    "Registrar horario",
                    "Ver carga académica",
                    "Cambiar estado");
            int option = view.readOption(6);
            if (option == 0) {
                return;
            }
            safely(() -> handleOption(option));
        }
    }

    private void handleOption(int option) {
        switch (option) {
            case 1 -> registerTeacher();
            case 2 -> listTeachers();
            case 3 -> assignSubject();
            case 4 -> registerSchedule();
            case 5 -> viewAcademicLoad();
            case 6 -> changeStatus();
            default -> view.showError("Opción no válida.");
        }
    }

    private void registerTeacher() {
        view.banner("Registrar Profesor");
        String docType = view.readDocumentType();
        String docNumber = view.readDocumentNumber();
        String firstName = view.readFirstName();
        String middleName = view.readMiddleName();
        String lastName = view.readLastName();
        String secondLastName = view.readSecondLastName();
        String email = view.readEmail();
        String phone = view.readPhone();
        String specialization = view.readSpecialization();
        LocalDate hireDate = view.readHireDate();

        Teacher teacher = new Teacher(
                0,
                docType,
                docNumber,
                firstName,
                middleName,
                lastName,
                secondLastName,
                email,
                phone,
                specialization,
                hireDate,
                "Activo"
        );

        Teacher created = service.registerTeacher(teacher);
        view.showSuccess("Profesor registrado exitosamente con ID " + created.id() + ".");
    }

    private void listTeachers() {
        view.banner("Directorio de Profesores");
        List<Teacher> teachers = service.listAllTeachers();
        view.showTeachers(teachers);
        view.pause();
    }

    private void assignSubject() {
        view.banner("Asignar Materia a Profesor");
        int teacherId = view.readTeacherId();
        Teacher teacher = service.findTeacherById(teacherId)
                .orElseThrow(() -> new BusinessException("Profesor no encontrado."));
        if (!"Activo".equals(teacher.status())) {
            throw new BusinessException("Solo los profesores con estado Activo pueden recibir asignaciones.");
        }

        int subjectId = view.readSubjectId();
        String section = view.readSection();

        TeachingAssignment assignment = service.assignSubject(teacherId, subjectId, section);
        view.showSuccess("Materia asignada exitosamente con ID " + assignment.id() + " (sección " + assignment.section() + ").");
    }

    private void registerSchedule() {
        view.banner("Registrar Horario de Clase");
        int assignmentId = view.readAssignmentId();
        service.findAssignmentById(assignmentId)
                .orElseThrow(() -> new BusinessException("Asignación no encontrada."));

        String day = view.readDayOfWeek();
        LocalTime startTime = view.readTime("Hora de inicio (HH:mm): ");
        LocalTime endTime = view.readTime("Hora de fin (HH:mm): ");

        service.registerSchedule(assignmentId, day, startTime, endTime);
        view.showSuccess("Horario registrado exitosamente.");
    }

    private void viewAcademicLoad() {
        view.banner("Consulta de Carga Académica");
        int teacherId = view.readTeacherId();
        Teacher teacher = service.findTeacherById(teacherId)
                .orElseThrow(() -> new BusinessException("Profesor no encontrado."));

        List<TeacherAssignmentDetail> workload = service.getTeacherWorkload(teacherId);
        view.showWorkload(teacher, workload);
        view.pause();
    }

    private void changeStatus() {
        view.banner("Cambiar Estado de Profesor");
        int teacherId = view.readTeacherId();
        Teacher teacher = service.findTeacherById(teacherId)
                .orElseThrow(() -> new BusinessException("Profesor no encontrado."));
        view.showInfo("Profesor: " + teacher.fullName() + " | Estado actual: " + teacher.status());

        String newStatus = view.readStatus();
        service.changeTeacherStatus(teacherId, newStatus);
        view.showSuccess("Estado del profesor actualizado a " + newStatus + ".");
    }
}
