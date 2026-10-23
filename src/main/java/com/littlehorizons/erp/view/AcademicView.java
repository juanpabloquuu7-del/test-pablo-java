package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.service.AcademicService.AbsenceAlertDetail;
import com.littlehorizons.erp.service.AcademicService.SubjectGradeSummary;
import com.littlehorizons.erp.util.InputValidator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class AcademicView extends BaseView {

    public AcademicView(Scanner scanner) {
        super(scanner);
    }

    public void showAcademicMenu() {
        showMenu(
                "Gestión Académica",
                "Registrar nota",
                "Ver historial académico",
                "Ver promedios",
                "Registrar asistencia del curso",
                "Reporte de inasistencias"
        );
    }

    public void showAveragesMenu() {
        showMenu(
                "Consultas de Promedios",
                "Promedios por materia de un estudiante",
                "Promedio general de un estudiante en un período",
                "Promedio del curso por materia"
        );
    }

    public int readStudentId() {
        return readInt("ID del estudiante: ", 1, Integer.MAX_VALUE);
    }

    public int readSubjectId() {
        return readInt("ID de la materia: ", 1, Integer.MAX_VALUE);
    }

    public int readTeacherId() {
        return readInt("ID del profesor: ", 1, Integer.MAX_VALUE);
    }

    public int readAcademicYear(int defaultYear) {
        while (true) {
            String input = readOptional("Año académico [" + defaultYear + "]");
            if (input == null || input.isBlank()) {
                return defaultYear;
            }
            try {
                int y = Integer.parseInt(input.trim());
                if (y >= 2000 && y <= 2100) {
                    return y;
                }
                showError("Año académico fuera de rango (debe estar entre 2000 y 2100).");
            } catch (NumberFormatException e) {
                showError("Año académico inválido. Ingrese un número entero de 4 dígitos.");
            }
        }
    }

    public String readPeriod(boolean allowFinal) {
        List<String> options = allowFinal
                ? List.of("I", "II", "III", "IV", "Final")
                : List.of("I", "II", "III", "IV");
        int index = readChoice("Seleccione el período académico: ", options);
        return options.get(index);
    }

    public double readGradeValue() {
        while (true) {
            double value = readDouble("Nota (0.0 a 5.0): ", 0.0, 5.0);
            if (InputValidator.isValidGrade(value)) {
                return value;
            }
            showError("La nota debe estar entre 0.0 y 5.0 con un solo decimal (por ejemplo 3.5 o 4.0).");
        }
    }

    public String readObservations() {
        return readOptional("Observaciones");
    }

    public String readAttendanceStatus() {
        List<String> options = List.of("Presente", "Ausente", "Tardanza", "Excusa Médica");
        int index = readChoice("Estado de asistencia: ", options);
        return options.get(index);
    }

    public LocalDate readAttendanceDate() {
        while (true) {
            LocalDate date = readDate("Fecha de la clase");
            if (!date.isAfter(LocalDate.now())) {
                return date;
            }
            showError("La fecha de asistencia no puede ser futura.");
        }
    }

    public String readSection() {
        while (true) {
            String sec = readOptional("Sección [A]");
            if (sec == null || sec.isBlank()) {
                return "A";
            }
            sec = sec.trim().toUpperCase();
            if (sec.length() <= 10) {
                return sec;
            }
            showError("La sección no puede tener más de 10 caracteres.");
        }
    }

    public boolean confirmUpdate(String message) {
        return readYesNo(message);
    }

    public void displayAcademicHistory(Student student, List<AcademicRecord> history, Map<Integer, String> subjectNames) {
        if (student == null) {
            showError("Estudiante inválido.");
            return;
        }
        banner("Historial Académico", "Estudiante: " + student.fullName() + " (ID: " + student.id() + ")");
        if (history == null || history.isEmpty()) {
            showInfo("El estudiante no tiene registros académicos.");
            return;
        }
        String[] headers = {"Año", "Período", "Materia", "Nota", "Observaciones", "Fecha"};
        List<String[]> rows = new ArrayList<>();
        for (AcademicRecord r : history) {
            if (r == null) continue;
            String subject = subjectNames != null ? subjectNames.getOrDefault(r.subjectId(), "Materia #" + r.subjectId()) : "Materia #" + r.subjectId();
            String dateStr = r.recordDate() != null ? r.recordDate().toString() : "";
            String obs = r.observations() != null ? r.observations() : "";
            String gradeStr = r.grade() != null ? r.grade().toPlainString() : "-";
            rows.add(new String[]{
                    String.valueOf(r.year()),
                    r.period() != null ? r.period() : "-",
                    subject,
                    gradeStr,
                    obs,
                    dateStr
            });
        }
        printTable(headers, rows);
    }

    public void displaySubjectSummaries(Student student, int year, List<SubjectGradeSummary> summaries) {
        if (student == null) {
            showError("Estudiante inválido.");
            return;
        }
        banner("Promedios por Materia (" + year + ")", "Estudiante: " + student.fullName() + " (ID: " + student.id() + ")");
        if (summaries == null || summaries.isEmpty()) {
            showInfo("No hay notas registradas para este estudiante en el año " + year + ".");
            return;
        }
        String[] headers = {"Materia", "Promedio (I-IV)", "Nota Final"};
        List<String[]> rows = new ArrayList<>();
        for (SubjectGradeSummary s : summaries) {
            if (s == null) continue;
            String avgStr = s.average() != null ? s.average().toPlainString() : "N/A";
            String finalStr = s.finalGrade() != null ? s.finalGrade().toPlainString() : "-";
            rows.add(new String[]{s.subjectName() != null ? s.subjectName() : "-", avgStr, finalStr});
        }
        printTable(headers, rows);
    }

    public void displayAbsenceAlerts(List<AbsenceAlertDetail> alerts, int threshold) {
        banner("Reporte de Inasistencias Semanales", "Umbral semanal: más de " + threshold + " días ausente");
        if (alerts == null || alerts.isEmpty()) {
            showInfo("No se encontraron estudiantes que superen el umbral de ausencias semanales.");
            return;
        }
        String[] headers = {"ID", "Estudiante", "Semana", "Días Ausente", "Total Inasistencias (Año)"};
        List<String[]> rows = new ArrayList<>();
        for (AbsenceAlertDetail a : alerts) {
            if (a == null) continue;
            rows.add(new String[]{
                    String.valueOf(a.studentId()),
                    a.studentName() != null ? a.studentName() : "-",
                    String.valueOf(a.yearWeek()),
                    String.valueOf(a.weeklyAbsenceDays()),
                    String.valueOf(a.totalYearAbsences())
            });
        }
        printTable(headers, rows);
    }
}
