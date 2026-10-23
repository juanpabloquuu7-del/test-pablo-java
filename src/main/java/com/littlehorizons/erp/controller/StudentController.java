package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.model.GradeLevel;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.GuardianLink;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.StudentService.StudentProfile;
import com.littlehorizons.erp.view.StudentView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class StudentController extends BaseController implements MenuModule {

    private final StudentView view;
    private final StudentService service;

    public StudentController(StudentView view, StudentService service) {
        super(view);
        this.view = view;
        this.service = service;
    }

    @Override
    public int position() {
        return 1;
    }

    @Override
    public void run() {
        while (true) {
            view.showMenu(
                    "Gestión de Estudiantes",
                    "Registrar estudiante",
                    "Buscar estudiante",
                    "Listar por grado",
                    "Gestionar acudientes",
                    "Matricular / cambiar de grado",
                    "Cambiar estado",
                    "Ver ficha completa",
                    "Inscribir en materias"
            );
            int option = view.readOption(8);
            if (option == 0) {
                return;
            }
            safely(() -> handleOption(option));
        }
    }

    private void handleOption(int option) {
        switch (option) {
            case 1 -> registerStudent();
            case 2 -> searchStudent();
            case 3 -> listByGrade();
            case 4 -> manageGuardians();
            case 5 -> enrollOrChangeGrade();
            case 6 -> changeStatus();
            case 7 -> viewProfile();
            case 8 -> enrollSubjects();
            default -> view.showError("Opción no reconocida.");
        }
    }

    private void registerStudent() {
        view.banner("REGISTRO DE ESTUDIANTE");
        Student student = view.readStudentForm();

        List<GradeLevel> grades = service.findAllGrades();
        if (grades == null || grades.isEmpty()) {
            view.showError("No hay grados registrados en el sistema.");
            return;
        }
        GradeLevel grade = view.selectGrade(grades);

        List<GuardianLink> guardians = new ArrayList<>();
        boolean adding = true;
        while (adding) {
            view.showInfo("Opciones de acudiente:");
            int opt = view.readChoice("Seleccione: ", List.of("Buscar acudiente existente por documento", "Registrar nuevo acudiente"));
            Guardian guardian = null;
            if (opt == 0) {
                String doc = view.readRequired("Documento del acudiente: ");
                Optional<Guardian> existing = service.findGuardianByDocument(doc);
                if (existing.isPresent()) {
                    guardian = existing.get();
                    view.showInfo("Acudiente encontrado: " + guardian.fullName());
                } else {
                    view.showInfo("No existe un acudiente con ese documento. Proceda a registrarlo.");
                    guardian = view.readGuardianForm(doc);
                }
            } else {
                guardian = view.readGuardianForm(null);
            }

            final String docNum = guardian.documentNumber();
            boolean alreadyInList = guardians.stream().anyMatch(g -> g.guardian().documentNumber().equals(docNum));
            if (alreadyInList) {
                view.showError("Este acudiente ya fue agregado a la lista del estudiante.");
                adding = view.readYesNo("¿Desea agregar otro acudiente?");
                continue;
            }

            boolean isPrimary;
            if (guardians.isEmpty()) {
                isPrimary = true;
                view.showInfo("Este acudiente será registrado como acudiente principal.");
            } else {
                isPrimary = view.readYesNo("¿Es acudiente principal?");
            }

            if (isPrimary) {
                for (int i = 0; i < guardians.size(); i++) {
                    GuardianLink prev = guardians.get(i);
                    if (prev.primary()) {
                        guardians.set(i, new GuardianLink(prev.guardian(), false, prev.emergencyContact()));
                    }
                }
            }

            boolean emergency = view.readYesNo("¿Es contacto de emergencia?");
            guardians.add(new GuardianLink(guardian, isPrimary, emergency));

            adding = view.readYesNo("¿Desea agregar otro acudiente?");
        }

        if (guardians.isEmpty()) {
            view.showError("Debe registrar al menos un acudiente.");
            return;
        }

        long primaryCount = guardians.stream().filter(GuardianLink::primary).count();
        if (primaryCount != 1) {
            view.showInfo("Ajustando acudiente principal: se designará al primer acudiente como principal.");
            List<GuardianLink> adjusted = new ArrayList<>();
            for (int i = 0; i < guardians.size(); i++) {
                GuardianLink gl = guardians.get(i);
                adjusted.add(new GuardianLink(gl.guardian(), i == 0, gl.emergencyContact()));
            }
            guardians = adjusted;
        }

        String notes = view.readOptional("Observaciones de matrícula");
        int studentId = service.registerStudent(student, guardians, grade.id(), notes);
        view.showSuccess("Estudiante registrado exitosamente con ID " + studentId + ".");
    }

    private void searchStudent() {
        view.banner("BÚSQUEDA DE ESTUDIANTES");
        String text = view.readRequired("Ingrese nombre o documento a buscar: ");
        List<Student> results = service.searchStudents(text);
        if (results == null || results.isEmpty()) {
            view.showInfo("No se encontraron estudiantes que coincidan con la búsqueda.");
            return;
        }
        view.showStudentTable(results, this::resolveGradeName);
    }

    private void listByGrade() {
        view.banner("LISTADO DE ESTUDIANTES POR GRADO");
        List<GradeLevel> grades = service.findAllGrades();
        if (grades == null || grades.isEmpty()) {
            view.showError("No hay grados registrados en el sistema.");
            return;
        }
        GradeLevel grade = view.selectGrade(grades);
        int currentYear = AppConfig.getInstance().getAcademicYear();
        List<Student> students = service.getStudentsByGrade(grade.id(), currentYear);
        if (students == null || students.isEmpty()) {
            view.showInfo("No hay estudiantes matriculados en " + grade.name() + " para el año " + currentYear + ".");
            return;
        }
        view.showStudentTable(students, id -> grade.name());
    }

    private void manageGuardians() {
        view.banner("GESTIÓN DE ACUDIENTES");
        int studentId = view.readInt("Ingrese el ID del estudiante: ", 1, Integer.MAX_VALUE);
        Student student = service.getStudent(studentId);
        view.showInfo("Estudiante: " + student.fullName() + " (Estado: " + student.status() + ")");

        List<GuardianLink> links = service.getGuardians(studentId);
        view.showGuardianList(links);

        view.showInfo("Acciones:");
        int choice = view.readChoice("Seleccione una acción: ", List.of(
                "Vincular acudiente existente",
                "Registrar y vincular nuevo acudiente",
                "Desvincular acudiente",
                "Designar acudiente principal"
        ));

        switch (choice) {
            case 0 -> {
                String doc = view.readRequired("Documento del acudiente: ");
                Optional<Guardian> gOpt = service.findGuardianByDocument(doc);
                if (gOpt.isEmpty()) {
                    view.showError("No se encontró ningún acudiente con ese documento.");
                    return;
                }
                boolean alreadyLinked = links.stream().anyMatch(l -> l.guardian().id() == gOpt.get().id());
                if (alreadyLinked) {
                    view.showError("El acudiente ya se encuentra vinculado a este estudiante.");
                    return;
                }
                boolean primary = view.readYesNo("¿Es acudiente principal?");
                boolean emergency = view.readYesNo("¿Es contacto de emergencia?");
                service.linkGuardian(studentId, gOpt.get().id(), primary, emergency);
                view.showSuccess("Acudiente vinculado con éxito.");
            }
            case 1 -> {
                Guardian g = view.readGuardianForm(null);
                Optional<Guardian> existing = service.findGuardianByDocument(g.documentNumber());
                int guardianId;
                if (existing.isPresent()) {
                    view.showInfo("Ya existe un acudiente registrado con ese número de documento. Se procederá a vincularlo.");
                    guardianId = existing.get().id();
                } else {
                    guardianId = service.saveGuardian(g);
                }
                boolean alreadyLinked = links.stream().anyMatch(l -> l.guardian().id() == guardianId);
                if (alreadyLinked) {
                    view.showError("El acudiente ya se encuentra vinculado a este estudiante.");
                    return;
                }
                boolean primary = view.readYesNo("¿Es acudiente principal?");
                boolean emergency = view.readYesNo("¿Es contacto de emergencia?");
                service.linkGuardian(studentId, guardianId, primary, emergency);
                view.showSuccess("Acudiente registrado y vinculado con éxito.");
            }
            case 2 -> {
                if (links.isEmpty()) {
                    view.showInfo("El estudiante no tiene acudientes para desvincular.");
                    return;
                }
                if (links.size() == 1) {
                    view.showError("El estudiante solo tiene un acudiente; no es posible desvincularlo sin asignar otro.");
                    return;
                }
                int idx = view.readInt("Seleccione el número de acudiente a desvincular (1-" + links.size() + "): ", 1, links.size()) - 1;
                GuardianLink linkToRemove = links.get(idx);
                if (linkToRemove.primary()) {
                    view.showError("No se puede desvincular al acudiente principal. Designe otro como principal primero.");
                    return;
                }
                service.unlinkGuardian(studentId, linkToRemove.guardian().id());
                view.showSuccess("Acudiente desvinculado con éxito.");
            }
            case 3 -> {
                if (links.isEmpty()) {
                    view.showInfo("El estudiante no tiene acudientes.");
                    return;
                }
                if (links.size() == 1) {
                    view.showInfo("El único acudiente ya es el principal.");
                    return;
                }
                int idx = view.readInt("Seleccione el número de acudiente que será el principal (1-" + links.size() + "): ", 1, links.size()) - 1;
                int guardianId = links.get(idx).guardian().id();
                service.setPrimaryGuardian(studentId, guardianId);
                view.showSuccess("Acudiente principal actualizado con éxito.");
            }
        }
    }

    private void enrollOrChangeGrade() {
        view.banner("MATRICULAR / CAMBIAR DE GRADO");
        int studentId = view.readInt("Ingrese el ID del estudiante: ", 1, Integer.MAX_VALUE);
        Student student = service.getStudent(studentId);
        view.showInfo("Estudiante: " + student.fullName() + " (Estado: " + student.status() + ")");

        List<GradeLevel> grades = service.findAllGrades();
        if (grades == null || grades.isEmpty()) {
            view.showError("No hay grados registrados en el sistema.");
            return;
        }
        GradeLevel grade = view.selectGrade(grades);
        int year = AppConfig.getInstance().getAcademicYear();
        String notes = view.readOptional("Observaciones");

        service.enrollOrChangeGrade(studentId, grade.id(), year, notes);
        view.showSuccess("Matrícula / cambio de grado procesado con éxito.");
    }

    private void changeStatus() {
        view.banner("CAMBIAR ESTADO DE ESTUDIANTE");
        int studentId = view.readInt("Ingrese el ID del estudiante: ", 1, Integer.MAX_VALUE);
        Student student = service.getStudent(studentId);
        view.showInfo("Estudiante: " + student.fullName() + " | Estado actual: " + student.status());

        String newStatus = view.selectStudentStatus();
        if (newStatus.equalsIgnoreCase(student.status())) {
            view.showInfo("El estudiante ya se encuentra en estado " + newStatus + ".");
            return;
        }
        service.changeStudentStatus(studentId, newStatus);
        view.showSuccess("Estado del estudiante actualizado a: " + newStatus);
    }

    private void viewProfile() {
        view.banner("VER FICHA COMPLETA");
        int studentId = view.readInt("Ingrese el ID del estudiante: ", 1, Integer.MAX_VALUE);
        StudentProfile profile = service.getStudentProfile(studentId);
        view.showStudentProfile(profile);
    }

    private void enrollSubjects() {
        view.banner("INSCRIBIR EN MATERIAS");
        int studentId = view.readInt("Ingrese el ID del estudiante: ", 1, Integer.MAX_VALUE);
        StudentProfile profile = service.getStudentProfile(studentId);
        if (profile.currentEnrollment() == null || !"Activa".equalsIgnoreCase(profile.currentEnrollment().status())) {
            view.showError("El estudiante no tiene una matrícula activa en el año académico actual.");
            return;
        }

        int currentGradeId = profile.currentEnrollment().gradeId();
        List<Subject> subjects = service.getSubjectsForGrade(currentGradeId);
        if (subjects == null || subjects.isEmpty()) {
            view.showInfo("No hay materias registradas para el grado del estudiante.");
            return;
        }

        boolean hasActive = subjects.stream().anyMatch(Subject::active);
        if (!hasActive) {
            view.showInfo("No hay materias activas disponibles para el grado del estudiante.");
            return;
        }

        String period = view.selectAcademicPeriod();
        String section = view.readOptional("Sección (Enter para 'A')");

        view.showInfo("Materias disponibles:");
        for (int i = 0; i < subjects.size(); i++) {
            Subject sub = subjects.get(i);
            view.showInfo((i + 1) + ". " + sub.code() + " - " + sub.name() + " (" + sub.weeklyHours() + "h)"
                    + (!sub.active() ? " [Inactiva]" : ""));
        }

        List<Integer> selectedSubjectIds = new ArrayList<>();
        while (true) {
            String sel = view.readRequired("Ingrese números separados por coma (ej: 1, 2), 'todas' o '0' para cancelar: ");
            String trimmed = sel.trim();
            if ("0".equals(trimmed)) {
                view.showInfo("Operación cancelada.");
                return;
            }
            if ("todas".equalsIgnoreCase(trimmed)) {
                for (Subject sub : subjects) {
                    if (sub.active()) {
                        selectedSubjectIds.add(sub.id());
                    }
                }
                if (selectedSubjectIds.isEmpty()) {
                    view.showError("No hay materias activas disponibles para inscribir.");
                    return;
                }
                break;
            }

            String[] parts = trimmed.split(",", -1);
            boolean valid = true;
            Set<Integer> chosenIds = new LinkedHashSet<>();
            for (String part : parts) {
                String p = part.trim();
                if (p.isEmpty()) {
                    view.showError("Formato inválido: la lista contiene elementos vacíos entre comas.");
                    valid = false;
                    break;
                }
                int num;
                try {
                    num = Integer.parseInt(p);
                } catch (NumberFormatException e) {
                    view.showError("'" + p + "' no es un número entero válido.");
                    valid = false;
                    break;
                }
                if (num < 1 || num > subjects.size()) {
                    view.showError("El número " + num + " está fuera de rango (1 a " + subjects.size() + ").");
                    valid = false;
                    break;
                }
                Subject sub = subjects.get(num - 1);
                if (!sub.active()) {
                    view.showError("La materia " + sub.code() + " (" + sub.name() + ") está inactiva y no se puede inscribir.");
                    valid = false;
                    break;
                }
                if (!chosenIds.add(sub.id())) {
                    view.showError("La materia " + sub.code() + " fue seleccionada más de una vez.");
                    valid = false;
                    break;
                }
            }

            if (valid) {
                selectedSubjectIds.addAll(chosenIds);
                break;
            }
        }

        if (selectedSubjectIds.isEmpty()) {
            view.showError("No se seleccionó ninguna materia válida.");
            return;
        }

        service.enrollSubjects(studentId, selectedSubjectIds, profile.currentEnrollment().year(), period, section);
        view.showSuccess("Inscripción realizada con éxito (" + selectedSubjectIds.size() + " materias).");
    }

    private String resolveGradeName(int studentId) {
        try {
            StudentProfile profile = service.getStudentProfile(studentId);
            return profile.currentGrade() != null ? profile.currentGrade().name() : "-";
        } catch (Exception e) {
            return "-";
        }
    }
}
