package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.GradeLevel;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.GuardianLink;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.service.StudentService.StudentProfile;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.InputValidator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

public class StudentView extends BaseView {

    private static final List<String> DOC_TYPES_STUDENT = List.of("TI", "CC", "CE", "Pasaporte");
    private static final List<String> DOC_TYPES_GUARDIAN = List.of("CC", "CE", "Pasaporte");
    private static final List<String> GENDERS = List.of("M", "F", "Otro");
    private static final List<String> STATUS_OPTIONS = List.of("Activo", "Suspendido", "Retirado", "Graduado");
    private static final List<String> PERIOD_OPTIONS = List.of("I", "II", "III", "IV");

    public StudentView(Scanner scanner) {
        super(scanner);
    }

    public void showStudentTable(List<Student> students, Function<Integer, String> gradeResolver) {
        if (students == null || students.isEmpty()) {
            showInfo("No hay registros para mostrar.");
            return;
        }
        String[] headers = {"ID", "Nombre", "Grado", "Estado"};
        List<String[]> rows = new ArrayList<>();
        for (Student s : students) {
            String gradeName = gradeResolver != null ? gradeResolver.apply(s.id()) : "-";
            rows.add(new String[]{
                    String.valueOf(s.id()),
                    s.fullName(),
                    gradeName != null ? gradeName : "-",
                    s.status() != null ? s.status() : "-"
            });
        }
        printTable(headers, rows);
    }

    public void showStudentProfile(StudentProfile profile) {
        if (profile == null || profile.student() == null) {
            showError("No hay datos de perfil para mostrar.");
            return;
        }
        Student s = profile.student();
        banner("FICHA COMPLETA DEL ESTUDIANTE");
        System.out.println("ID:                  " + s.id());
        System.out.println("Nombre completo:     " + s.fullName());
        System.out.println("Documento:           " + (s.documentType() != null ? s.documentType() : "-") + " " + (s.documentNumber() != null ? s.documentNumber() : "-"));
        System.out.println("Fecha de nacimiento: " + (s.birthDate() != null ? s.birthDate() : "-"));
        System.out.println("Género:              " + (s.gender() != null ? s.gender() : "-"));
        System.out.println("Fecha de ingreso:    " + (s.admissionDate() != null ? s.admissionDate() : "-"));
        System.out.println("Estado actual:       " + (s.status() != null ? s.status() : "-"));
        System.out.println("Dirección:           " + (s.address() != null ? s.address() : "-"));
        System.out.println("Teléfono:            " + (s.phone() != null ? s.phone() : "-"));
        System.out.println("Correo electrónico:  " + (s.email() != null ? s.email() : "-"));
        System.out.println("Tipo de sangre:      " + (s.bloodType() != null ? s.bloodType() : "-"));
        System.out.println("Alergias:            " + (s.allergies() != null ? s.allergies() : "-"));
        System.out.println("Condiciones médicas: " + (s.medicalConditions() != null ? s.medicalConditions() : "-"));
        System.out.println();

        System.out.println("--- Matrícula Vigente ---");
        if (profile.currentEnrollment() != null) {
            System.out.println("Grado:            " + (profile.currentGrade() != null ? profile.currentGrade().name() : "-"));
            System.out.println("Año académico:    " + profile.currentEnrollment().year());
            System.out.println("Estado matrícula: " + profile.currentEnrollment().status());
            System.out.println("Fecha matrícula:  " + profile.currentEnrollment().enrollmentDate());
            if (profile.currentEnrollment().withdrawalDate() != null) {
                System.out.println("Fecha de retiro:  " + profile.currentEnrollment().withdrawalDate());
            }
            if (profile.currentEnrollment().notes() != null) {
                System.out.println("Observaciones:    " + profile.currentEnrollment().notes());
            }
        } else {
            System.out.println("No tiene matrícula registrada en el año académico actual.");
        }
        System.out.println();

        System.out.println("--- Acudientes Vinculados ---");
        if (profile.guardians() == null || profile.guardians().isEmpty()) {
            System.out.println("No tiene acudientes registrados.");
        } else {
            for (GuardianLink link : profile.guardians()) {
                if (link == null || link.guardian() == null) continue;
                Guardian g = link.guardian();
                System.out.println("• " + g.fullName() + " (" + (g.relationship() != null ? g.relationship() : "-") + ")");
                System.out.println("  Documento: " + (g.documentType() != null ? g.documentType() : "-") + " " + (g.documentNumber() != null ? g.documentNumber() : "-"));
                System.out.println("  Teléfono:  " + (g.phone() != null ? g.phone() : "-") + (g.secondaryPhone() != null ? " / " + g.secondaryPhone() : ""));
                System.out.println("  Correo:    " + (g.email() != null ? g.email() : "-"));
                System.out.println("  Dirección: " + (g.address() != null ? g.address() : "-"));
                System.out.println("  Ocupación: " + (g.occupation() != null ? g.occupation() : "-"));
                System.out.println("  Principal: " + (link.primary() ? "SÍ" : "NO") + " | Emergencia: " + (link.emergencyContact() ? "SÍ" : "NO") + " | Resp. Financiero: " + (g.financialResponsible() ? "SÍ" : "NO"));
            }
        }
        System.out.println();
    }

    public Student readStudentForm() {
        System.out.println("Tipo de documento:");
        int docIndex = readChoice("Seleccione el tipo de documento: ", DOC_TYPES_STUDENT);
        String docType = DOC_TYPES_STUDENT.get(docIndex);

        String docNumber = readDocument("Número de documento: ");
        String firstName = readRequired("Primer nombre: ");
        String middleName = readOptional("Segundo nombre");
        String lastName = readRequired("Primer apellido: ");
        String secondLastName = readOptional("Segundo apellido");
        LocalDate birthDate = readBirthDate();

        System.out.println("Género:");
        int genderIndex = readChoice("Seleccione el género: ", GENDERS);
        String gender = GENDERS.get(genderIndex);

        String address = readOptional("Dirección");
        String phone = readOptionalPhone("Teléfono");
        String email = readOptionalEmail("Correo electrónico");
        String bloodType = readOptional("Tipo de sangre (ej: O+, A-)");
        String allergies = readOptional("Alergias");
        String medicalConditions = readOptional("Condiciones médicas");

        return new Student(
                0,
                docType,
                docNumber,
                firstName,
                middleName,
                lastName,
                secondLastName,
                birthDate,
                gender,
                address,
                phone,
                email,
                bloodType,
                allergies,
                medicalConditions,
                LocalDate.now(),
                "Activo"
        );
    }

    public Guardian readGuardianForm(String docNumberPrefill) {
        System.out.println("Tipo de documento del acudiente:");
        int docIndex = readChoice("Seleccione el tipo de documento: ", DOC_TYPES_GUARDIAN);
        String docType = DOC_TYPES_GUARDIAN.get(docIndex);

        String docNumber = docNumberPrefill != null ? docNumberPrefill : readDocument("Número de documento: ");
        String firstName = readRequired("Primer nombre: ");
        String middleName = readOptional("Segundo nombre");
        String lastName = readRequired("Primer apellido: ");
        String secondLastName = readOptional("Segundo apellido");
        String relationship = readRequired("Parentesco (ej: Padre, Madre, Tutor Legal): ");
        String phone = readRequiredPhone("Teléfono principal: ");
        String secondaryPhone = readOptionalPhone("Teléfono secundario");
        String email = readOptionalEmail("Correo electrónico");
        String address = readOptional("Dirección");
        String occupation = readOptional("Ocupación");
        boolean financialResponsible = readYesNo("¿Es responsable financiero?");

        return new Guardian(
                0,
                docType,
                docNumber,
                firstName,
                middleName,
                lastName,
                secondLastName,
                relationship,
                phone,
                secondaryPhone,
                email,
                address,
                occupation,
                financialResponsible
        );
    }

    public GradeLevel selectGrade(List<GradeLevel> grades) {
        if (grades == null || grades.isEmpty()) {
            throw new BusinessException("No hay grados disponibles.");
        }
        List<String> names = grades.stream().map(GradeLevel::name).toList();
        System.out.println("Grados disponibles:");
        int idx = readChoice("Seleccione el grado: ", names);
        return grades.get(idx);
    }

    public String selectStudentStatus() {
        System.out.println("Estados disponibles:");
        int idx = readChoice("Seleccione el nuevo estado: ", STATUS_OPTIONS);
        return STATUS_OPTIONS.get(idx);
    }

    public String selectAcademicPeriod() {
        System.out.println("Períodos académicos:");
        int idx = readChoice("Seleccione el período: ", PERIOD_OPTIONS);
        return PERIOD_OPTIONS.get(idx);
    }

    public void showGuardianList(List<GuardianLink> guardians) {
        if (guardians == null || guardians.isEmpty()) {
            showInfo("El estudiante no tiene acudientes registrados.");
            return;
        }
        for (int i = 0; i < guardians.size(); i++) {
            GuardianLink link = guardians.get(i);
            if (link == null || link.guardian() == null) continue;
            Guardian g = link.guardian();
            System.out.println((i + 1) + ". " + g.fullName() + " (" + (g.relationship() != null ? g.relationship() : "-") + ")"
                    + (link.primary() ? " [Principal]" : "")
                    + (link.emergencyContact() ? " [Emergencia]" : "")
                    + " - ID Acudiente: " + g.id());
        }
    }

    public String readDocument(String prompt) {
        while (true) {
            String doc = readRequired(prompt);
            if (InputValidator.isValidDocument(doc)) {
                return doc;
            }
            showError("Documento inválido. Debe tener entre 5 y 20 caracteres alfanuméricos sin espacios.");
        }
    }

    public LocalDate readBirthDate() {
        while (true) {
            LocalDate date = readDate("Fecha de nacimiento");
            if (date.isBefore(LocalDate.now())) {
                return date;
            }
            showError("La fecha de nacimiento debe ser anterior a la fecha actual.");
        }
    }

    public String readRequiredPhone(String prompt) {
        while (true) {
            String text = readRequired(prompt);
            if (InputValidator.isValidPhone(text)) {
                return text;
            }
            showError("Teléfono inválido. Debe contener entre 7 y 15 dígitos (opcional prefijo +).");
        }
    }

    public String readOptionalPhone(String prompt) {
        while (true) {
            String text = readOptional(prompt);
            if (text == null || InputValidator.isValidPhone(text)) {
                return text;
            }
            showError("Teléfono inválido. Debe contener entre 7 y 15 dígitos (opcional prefijo +).");
        }
    }

    public String readOptionalEmail(String prompt) {
        while (true) {
            String text = readOptional(prompt);
            if (text == null || InputValidator.isValidEmail(text)) {
                return text;
            }
            showError("Correo electrónico inválido. Formato esperado: usuario@dominio.com.");
        }
    }
}
