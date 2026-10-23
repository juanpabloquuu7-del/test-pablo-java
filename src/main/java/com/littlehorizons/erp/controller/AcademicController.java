package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.service.AcademicService;
import com.littlehorizons.erp.service.AcademicService.AbsenceAlertDetail;
import com.littlehorizons.erp.service.AcademicService.SubjectGradeSummary;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.InputValidator;
import com.littlehorizons.erp.view.AcademicView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AcademicController extends BaseController implements MenuModule {

    private final AcademicView view;
    private final AcademicService service;

    public AcademicController(AcademicView view, AcademicService service) {
        super(view);
        this.view = view;
        this.service = service;
    }

    @Override
    public int position() {
        return 2;
    }

    @Override
    public void run() {
        while (true) {
            view.showAcademicMenu();
            int option = view.readOption(5);
            if (option == 0) {
                return;
            }
            safely(() -> {
                switch (option) {
                    case 1 -> handleRegisterGrade();
                    case 2 -> handleViewAcademicHistory();
                    case 3 -> handleViewAverages();
                    case 4 -> handleRegisterCourseAttendance();
                    case 5 -> handleAbsenceReport();
                }
            });
        }
    }

    private void handleRegisterGrade() {
        int studentId = view.readStudentId();
        int subjectId = view.readSubjectId();
        int year = view.readAcademicYear(AppConfig.getInstance().getAcademicYear());
        String period = view.readPeriod(true);
        int teacherId = view.readTeacherId();

        service.validateGradePrerequisites(studentId, subjectId, year, period, teacherId);

        Optional<AcademicRecord> existing = service.findGrade(studentId, subjectId, year, period);
        if (existing.isPresent()) {
            boolean update = view.confirmUpdate(
                    "Ya existe una calificación registrada para este período (nota actual: "
                            + existing.get().grade().toPlainString() + "). ¿Desea actualizarla?"
            );
            if (!update) {
                view.showInfo("Operación cancelada.");
                return;
            }
            double gradeVal = view.readGradeValue();
            String observations = view.readObservations();
            AcademicRecord toUpdate = new AcademicRecord(
                    existing.get().id(),
                    studentId,
                    subjectId,
                    teacherId,
                    year,
                    period,
                    BigDecimal.valueOf(gradeVal),
                    observations,
                    LocalDate.now()
            );
            service.updateGrade(toUpdate);
            view.showSuccess("Calificación actualizada correctamente.");
        } else {
            double gradeVal = view.readGradeValue();
            String observations = view.readObservations();
            AcademicRecord toInsert = new AcademicRecord(
                    0,
                    studentId,
                    subjectId,
                    teacherId,
                    year,
                    period,
                    BigDecimal.valueOf(gradeVal),
                    observations,
                    LocalDate.now()
            );
            service.registerGrade(toInsert);
            view.showSuccess("Calificación registrada correctamente.");
        }
    }

    private void handleViewAcademicHistory() {
        int studentId = view.readStudentId();
        Student student = service.findStudent(studentId)
                .orElseThrow(() -> new BusinessException("Estudiante no encontrado."));
        List<AcademicRecord> history = service.getAcademicHistory(studentId);
        Map<Integer, String> subjectNames = new HashMap<>();
        for (AcademicRecord r : history) {
            if (!subjectNames.containsKey(r.subjectId())) {
                service.findSubject(r.subjectId()).ifPresent(s -> subjectNames.put(s.id(), s.name()));
            }
        }
        view.displayAcademicHistory(student, history, subjectNames);
    }

    private void handleViewAverages() {
        while (true) {
            view.showAveragesMenu();
            int subOption = view.readOption(3);
            if (subOption == 0) {
                return;
            }
            safely(() -> {
                switch (subOption) {
                    case 1 -> viewSubjectAverages();
                    case 2 -> viewPeriodAverage();
                    case 3 -> viewCourseAverage();
                }
            });
        }
    }

    private void viewSubjectAverages() {
        int studentId = view.readStudentId();
        Student student = service.findStudent(studentId)
                .orElseThrow(() -> new BusinessException("Estudiante no encontrado."));
        int year = view.readAcademicYear(AppConfig.getInstance().getAcademicYear());
        List<SubjectGradeSummary> summaries = service.getSubjectSummariesByStudent(studentId, year);
        view.displaySubjectSummaries(student, year, summaries);
    }

    private void viewPeriodAverage() {
        int studentId = view.readStudentId();
        Student student = service.findStudent(studentId)
                .orElseThrow(() -> new BusinessException("Estudiante no encontrado."));
        int year = view.readAcademicYear(AppConfig.getInstance().getAcademicYear());
        String period = view.readPeriod(false);
        Optional<BigDecimal> avg = service.getStudentPeriodAverage(studentId, year, period);
        if (avg.isPresent()) {
            view.showInfo("Promedio general de " + student.fullName() + " en el período " + period + " (" + year + "): " + avg.get().toPlainString());
        } else {
            view.showInfo("No hay calificaciones registradas para el estudiante en el período " + period + ".");
        }
    }

    private void viewCourseAverage() {
        int subjectId = view.readSubjectId();
        Subject subject = service.findSubject(subjectId)
                .orElseThrow(() -> new BusinessException("Materia no encontrada."));
        int year = view.readAcademicYear(AppConfig.getInstance().getAcademicYear());
        boolean byPeriod = view.confirmUpdate("¿Desea filtrar por período específico?");
        String period = byPeriod ? view.readPeriod(false) : null;
        Optional<BigDecimal> avg = service.getCourseAverage(subjectId, year, period);
        if (avg.isPresent()) {
            String label = period != null ? "período " + period : "año completo";
            view.showInfo("Promedio del curso en " + subject.name() + " (" + label + "): " + avg.get().toPlainString());
        } else {
            view.showInfo("No hay calificaciones registradas para el curso en la materia.");
        }
    }

    private void handleRegisterCourseAttendance() {
        int subjectId = view.readSubjectId();
        int year = view.readAcademicYear(AppConfig.getInstance().getAcademicYear());
        String period = view.readPeriod(false);
        String section = view.readSection();
        LocalDate date = view.readAttendanceDate();
        int teacherId = view.readTeacherId();

        service.validateCourseAttendancePrerequisites(subjectId, teacherId, year, date);
        List<Student> students = service.getEnrolledStudents(subjectId, year, period, section);
        if (students.isEmpty()) {
            view.showInfo("No hay estudiantes inscritos en el curso y período seleccionados.");
            return;
        }

        for (Student s : students) {
            if (service.hasAttendance(s.id(), subjectId, date)) {
                view.showError("Ya existen registros de asistencia para la fecha " + date + " en este curso.");
                return;
            }
        }

        view.showInfo("Registrando asistencia para " + students.size() + " estudiante(s).");
        List<Attendance> attendances = new ArrayList<>();
        for (Student s : students) {
            view.banner("Estudiante: " + s.fullName() + " (ID: " + s.id() + ")");
            String status = view.readAttendanceStatus();
            String obs = view.readObservations();
            attendances.add(new Attendance(0, s.id(), subjectId, date, status, obs, teacherId));
        }

        service.registerCourseAttendance(attendances);
        view.showSuccess("Se guardaron " + attendances.size() + " asistencias exitosamente.");
    }

    private void handleAbsenceReport() {
        int threshold = AppConfig.getInstance().getInt("business.weeklyAbsenceThreshold", 3);
        int year = AppConfig.getInstance().getAcademicYear();
        List<AbsenceAlertDetail> alerts = service.getAbsenceAlerts(threshold, year);
        view.displayAbsenceAlerts(alerts, threshold);
    }
}
