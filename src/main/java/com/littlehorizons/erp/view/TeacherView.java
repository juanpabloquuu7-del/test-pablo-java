package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.service.TeacherService.TeacherAssignmentDetail;
import com.littlehorizons.erp.util.InputValidator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

public class TeacherView extends BaseView {

    private static final List<String> DOC_TYPES = List.of("CC", "CE", "Pasaporte");
    private static final List<String> STATUS_OPTIONS = List.of("Activo", "Inactivo", "Licencia");
    private static final List<String> DAYS_OF_WEEK = List.of(
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"
    );

    private static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .optionalEnd()
            .toFormatter();

    public TeacherView(Scanner scanner) {
        super(scanner);
    }

    public void showTeachers(List<Teacher> teachers) {
        if (teachers == null || teachers.isEmpty()) {
            showInfo("No hay profesores registrados.");
            return;
        }
        String[] headers = {"ID", "Nombre", "Especialización", "Estado"};
        List<String[]> rows = teachers.stream()
                .filter(Objects::nonNull)
                .map(t -> new String[]{
                        String.valueOf(t.id()),
                        t.fullName(),
                        t.specialization() != null ? t.specialization() : "",
                        t.status() != null ? t.status() : ""
                })
                .toList();
        printTable(headers, rows);
    }

    public void showWorkload(Teacher teacher, List<TeacherAssignmentDetail> workload) {
        banner("Carga Académica: " + (teacher != null ? teacher.fullName() : "Desconocido"),
                "ID: " + (teacher != null ? String.valueOf(teacher.id()) : "-")
                        + " | Especialización: " + (teacher != null && teacher.specialization() != null ? teacher.specialization() : "N/A"));

        if (workload == null || workload.isEmpty()) {
            showInfo("El profesor no tiene materias asignadas en el año académico.");
            return;
        }

        String[] headers = {"ID Asig", "Materia", "Código", "Sección", "Horas Sem.", "Horarios"};
        List<String[]> rows = workload.stream()
                .filter(Objects::nonNull)
                .map(detail -> {
                    String idAsig = detail.assignment() != null ? String.valueOf(detail.assignment().id()) : "-";
                    String subjectName = detail.subject() != null
                            ? detail.subject().name()
                            : (detail.assignment() != null ? "Materia #" + detail.assignment().subjectId() : "-");
                    String subjectCode = detail.subject() != null ? detail.subject().code() : "-";
                    String section = detail.assignment() != null && detail.assignment().section() != null
                            ? detail.assignment().section()
                            : "-";
                    String weeklyHours = detail.subject() != null ? String.valueOf(detail.subject().weeklyHours()) : "-";
                    String schedulesText;
                    if (detail.schedules() == null || detail.schedules().isEmpty()) {
                        schedulesText = "(Sin horario)";
                    } else {
                        schedulesText = detail.schedules().stream()
                                .filter(Objects::nonNull)
                                .map(s -> s.day() + " " + s.startTime() + "-" + s.endTime())
                                .collect(Collectors.joining("; "));
                    }

                    return new String[]{
                            idAsig,
                            subjectName,
                            subjectCode,
                            section,
                            weeklyHours,
                            schedulesText
                    };
                })
                .toList();

        printTable(headers, rows);
    }

    public String readDocumentType() {
        System.out.println("Seleccione el tipo de documento:");
        int idx = readChoice("Opción: ", DOC_TYPES);
        return DOC_TYPES.get(idx);
    }

    public String readDocumentNumber() {
        while (true) {
            String doc = readRequired("Número de documento: ");
            if (InputValidator.isValidDocument(doc)) {
                return doc;
            }
            showError("Número de documento inválido (debe tener entre 5 y 20 caracteres alfanuméricos).");
        }
    }

    public String readFirstName() {
        return readRequired("Primer nombre: ");
    }

    public String readMiddleName() {
        return readOptional("Segundo nombre");
    }

    public String readLastName() {
        return readRequired("Primer apellido: ");
    }

    public String readSecondLastName() {
        return readOptional("Segundo apellido");
    }

    public String readEmail() {
        while (true) {
            String email = readRequired("Correo electrónico: ");
            if (InputValidator.isValidEmail(email)) {
                return email;
            }
            showError("Correo electrónico inválido. Use el formato nombre@dominio.com.");
        }
    }

    public String readPhone() {
        while (true) {
            String phone = readOptional("Teléfono");
            if (phone == null || InputValidator.isValidPhone(phone)) {
                return phone;
            }
            showError("Teléfono inválido. Ingrese entre 7 y 15 dígitos.");
        }
    }

    public String readSpecialization() {
        return readOptional("Especialización");
    }

    public LocalDate readHireDate() {
        return readDate("Fecha de contratación");
    }

    public int readTeacherId() {
        return readInt("ID del profesor: ", 1, Integer.MAX_VALUE);
    }

    public int readSubjectId() {
        return readInt("ID de la materia: ", 1, Integer.MAX_VALUE);
    }

    public String readSection() {
        while (true) {
            String sec = readOptional("Sección");
            if (sec == null || sec.trim().length() <= 10) {
                return sec != null ? sec.trim() : null;
            }
            showError("La sección no puede superar los 10 caracteres.");
        }
    }

    public int readAssignmentId() {
        return readInt("ID de la asignación: ", 1, Integer.MAX_VALUE);
    }

    public String readDayOfWeek() {
        System.out.println("Seleccione el día de la semana:");
        int idx = readChoice("Opción: ", DAYS_OF_WEEK);
        return DAYS_OF_WEEK.get(idx);
    }

    public LocalTime readTime(String prompt) {
        while (true) {
            String text = ask(prompt);
            try {
                return LocalTime.parse(text, TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                showError("Hora inválida. Use el formato HH:mm (por ejemplo 08:00 o 14:30).");
            }
        }
    }

    public String readStatus() {
        System.out.println("Seleccione el nuevo estado:");
        int idx = readChoice("Opción: ", STATUS_OPTIONS);
        return STATUS_OPTIONS.get(idx);
    }
}
