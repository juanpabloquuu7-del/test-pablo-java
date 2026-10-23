package com.littlehorizons.erp.service;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GradeLevelDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentRegistrationDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.GradeLevel;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.GuardianLink;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.InputValidator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class StudentService {

    public record StudentProfile(
            Student student,
            GradeLevel currentGrade,
            Enrollment currentEnrollment,
            List<Enrollment> enrollmentHistory,
            List<GuardianLink> guardians
    ) {
    }

    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final EnrollmentDao enrollmentDao;
    private final GradeLevelDao gradeLevelDao;
    private final StudentRegistrationDao studentRegistrationDao;
    private final SubjectDao subjectDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;

    public StudentService(
            StudentDao studentDao,
            GuardianDao guardianDao,
            EnrollmentDao enrollmentDao,
            GradeLevelDao gradeLevelDao,
            StudentRegistrationDao studentRegistrationDao,
            SubjectDao subjectDao,
            SubjectEnrollmentDao subjectEnrollmentDao) {
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.enrollmentDao = enrollmentDao;
        this.gradeLevelDao = gradeLevelDao;
        this.studentRegistrationDao = studentRegistrationDao;
        this.subjectDao = subjectDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
    }

    public int registerStudent(Student student, List<GuardianLink> guardians, int gradeId, String notes) {
        validateStudentData(student);

        if (studentDao.findByDocument(student.documentNumber()).isPresent()) {
            throw new BusinessException("Ya existe un estudiante con ese número de documento.");
        }

        StudentRules.validateGuardians(guardians);
        Set<String> seenDocs = new HashSet<>();
        for (GuardianLink link : guardians) {
            Guardian g = link.guardian();
            validateGuardianData(g);
            if (!seenDocs.add(g.documentNumber())) {
                throw new BusinessException("No se puede registrar el mismo acudiente más de una vez.");
            }
            if (g.id() <= 0) {
                if (guardianDao.findByDocument(g.documentNumber()).isPresent()) {
                    throw new BusinessException("Ya existe un acudiente con ese número de documento.");
                }
            } else {
                if (guardianDao.findById(g.id()).isEmpty()) {
                    throw new BusinessException("El acudiente no existe.");
                }
            }
        }

        int academicYear = AppConfig.getInstance().getAcademicYear();
        GradeLevel grade = gradeLevelDao.findById(gradeId)
                .orElseThrow(() -> new BusinessException("El grado seleccionado no existe."));

        StudentRules.validateAgeConsistency(student.birthDate(), grade.numericLevel(), academicYear);

        Enrollment enrollment = new Enrollment(
                0,
                0,
                gradeId,
                academicYear,
                "Activa",
                LocalDate.now(),
                null,
                notes
        );

        return studentRegistrationDao.register(student, guardians, enrollment);
    }

    public int updateStudent(Student student) {
        validateStudentData(student);

        if (studentDao.findById(student.id()).isEmpty()) {
            throw new BusinessException("Estudiante no encontrado.");
        }

        Optional<Student> existing = studentDao.findByDocument(student.documentNumber());
        if (existing.isPresent() && existing.get().id() != student.id()) {
            throw new BusinessException("Ya existe un estudiante con ese número de documento.");
        }

        return studentDao.update(student);
    }

    public Student getStudent(int id) {
        if (id <= 0) {
            throw new BusinessException("El ID del estudiante debe ser mayor que cero.");
        }
        return studentDao.findById(id)
                .orElseThrow(() -> new BusinessException("Estudiante no encontrado."));
    }

    public List<Student> searchStudents(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return studentDao.search(text.trim());
    }

    public List<Student> getStudentsByGrade(int gradeId, int year) {
        if (gradeId <= 0) {
            throw new BusinessException("El ID de grado debe ser mayor que cero.");
        }
        if (year <= 0) {
            throw new BusinessException("El año académico debe ser mayor que cero.");
        }
        if (gradeLevelDao.findById(gradeId).isEmpty()) {
            throw new BusinessException("El grado seleccionado no existe.");
        }
        return studentDao.findByGrade(gradeId, year);
    }

    public List<GradeLevel> findAllGrades() {
        return gradeLevelDao.findAll();
    }

    public Optional<GradeLevel> findGradeById(int id) {
        return gradeLevelDao.findById(id);
    }

    public StudentProfile getStudentProfile(int studentId) {
        Student student = getStudent(studentId);
        int academicYear = AppConfig.getInstance().getAcademicYear();
        Optional<Enrollment> currentEnrollment = enrollmentDao.findByStudentAndYear(studentId, academicYear);
        GradeLevel currentGrade = null;
        if (currentEnrollment.isPresent()) {
            currentGrade = gradeLevelDao.findById(currentEnrollment.get().gradeId()).orElse(null);
        }
        List<Enrollment> history = enrollmentDao.findByStudent(studentId);
        List<GuardianLink> guardians = guardianDao.findByStudent(studentId);
        return new StudentProfile(student, currentGrade, currentEnrollment.orElse(null), history, guardians);
    }

    public void changeStudentStatus(int studentId, String targetStatus) {
        Student student = getStudent(studentId);
        if (targetStatus != null && targetStatus.equalsIgnoreCase(student.status())) {
            throw new BusinessException("El estudiante ya se encuentra en estado " + targetStatus + ".");
        }

        int academicYear = AppConfig.getInstance().getAcademicYear();
        Optional<Enrollment> currentEnrollment = enrollmentDao.findByStudentAndYear(studentId, academicYear);

        int numericLevel = -1;
        if (currentEnrollment.isPresent()) {
            numericLevel = gradeLevelDao.findById(currentEnrollment.get().gradeId())
                    .map(GradeLevel::numericLevel)
                    .orElse(-1);
        }

        StudentRules.validateStatusTransition(student.status(), targetStatus, numericLevel);

        if ("Graduado".equalsIgnoreCase(targetStatus)) {
            if (currentEnrollment.isEmpty() || !"Activa".equalsIgnoreCase(currentEnrollment.get().status())) {
                throw new BusinessException("El estudiante no cuenta con matrícula activa para ser graduado.");
            }
            studentDao.updateStatus(studentId, "Graduado");
            enrollmentDao.updateStatus(currentEnrollment.get().id(), "Completada", null);
        } else if ("Retirado".equalsIgnoreCase(targetStatus)) {
            studentDao.updateStatus(studentId, "Retirado");
            if (currentEnrollment.isPresent()) {
                enrollmentDao.updateStatus(currentEnrollment.get().id(), "Retirada", LocalDate.now());
            }
        } else if ("Suspendido".equalsIgnoreCase(targetStatus)) {
            studentDao.updateStatus(studentId, "Suspendido");
        } else if ("Activo".equalsIgnoreCase(targetStatus)) {
            studentDao.updateStatus(studentId, "Activo");
            if (currentEnrollment.isPresent() && !"Activa".equals(currentEnrollment.get().status())) {
                enrollmentDao.updateStatus(currentEnrollment.get().id(), "Activa", null);
            }
        }
    }

    public int enrollOrChangeGrade(int studentId, int gradeId, int year, String notes) {
        Student student = getStudent(studentId);
        if ("Graduado".equalsIgnoreCase(student.status())) {
            throw new BusinessException("Un estudiante graduado no puede ser matriculado.");
        }
        if ("Retirado".equalsIgnoreCase(student.status())) {
            throw new BusinessException("Un estudiante retirado no puede ser matriculado directamente; reactive su estado primero.");
        }

        GradeLevel grade = gradeLevelDao.findById(gradeId)
                .orElseThrow(() -> new BusinessException("El grado seleccionado no existe."));
        StudentRules.validateAgeConsistency(student.birthDate(), grade.numericLevel(), year);

        Optional<Enrollment> existing = enrollmentDao.findByStudentAndYear(studentId, year);
        if (existing.isPresent()) {
            enrollmentDao.updateGrade(existing.get().id(), gradeId);
            if (!"Activa".equals(existing.get().status())) {
                enrollmentDao.updateStatus(existing.get().id(), "Activa", null);
            }
            return existing.get().id();
        } else {
            Enrollment newEnrollment = new Enrollment(
                    0,
                    studentId,
                    gradeId,
                    year,
                    "Activa",
                    LocalDate.now(),
                    null,
                    notes
            );
            return enrollmentDao.insert(newEnrollment);
        }
    }

    public List<GuardianLink> getGuardians(int studentId) {
        getStudent(studentId);
        return guardianDao.findByStudent(studentId);
    }

    public Optional<Guardian> findGuardianByDocument(String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            return Optional.empty();
        }
        return guardianDao.findByDocument(documentNumber.trim());
    }

    public int saveGuardian(Guardian guardian) {
        validateGuardianData(guardian);

        if (guardian.id() <= 0) {
            if (guardianDao.findByDocument(guardian.documentNumber()).isPresent()) {
                throw new BusinessException("Ya existe un acudiente con ese número de documento.");
            }
            return guardianDao.insert(guardian);
        } else {
            Optional<Guardian> existing = guardianDao.findByDocument(guardian.documentNumber());
            if (existing.isPresent() && existing.get().id() != guardian.id()) {
                throw new BusinessException("Ya existe un acudiente con ese número de documento.");
            }
            guardianDao.update(guardian);
            return guardian.id();
        }
    }

    public void linkGuardian(int studentId, int guardianId, boolean primary, boolean emergencyContact) {
        getStudent(studentId);
        guardianDao.findById(guardianId)
                .orElseThrow(() -> new BusinessException("El acudiente no existe."));
        List<GuardianLink> current = guardianDao.findByStudent(studentId);
        boolean alreadyLinked = current.stream().anyMatch(l -> l.guardian().id() == guardianId);
        if (alreadyLinked) {
            throw new BusinessException("El acudiente ya está vinculado al estudiante.");
        }
        boolean makePrimary = primary || current.isEmpty();
        guardianDao.link(studentId, guardianId, makePrimary, emergencyContact);
        if (makePrimary) {
            guardianDao.setPrimary(studentId, guardianId);
        }
    }

    public void unlinkGuardian(int studentId, int guardianId) {
        getStudent(studentId);
        List<GuardianLink> links = guardianDao.findByStudent(studentId);
        if (links.size() <= 1) {
            throw new BusinessException("No se puede desvincular al único acudiente del estudiante.");
        }
        GuardianLink link = links.stream()
                .filter(l -> l.guardian().id() == guardianId)
                .findFirst()
                .orElseThrow(() -> new BusinessException("El acudiente no está vinculado al estudiante."));
        if (link.primary()) {
            throw new BusinessException("No se puede desvincular al acudiente principal sin designar otro antes.");
        }
        guardianDao.unlink(studentId, guardianId);
    }

    public void setPrimaryGuardian(int studentId, int guardianId) {
        getStudent(studentId);
        List<GuardianLink> links = guardianDao.findByStudent(studentId);
        boolean found = links.stream().anyMatch(l -> l.guardian().id() == guardianId);
        if (!found) {
            throw new BusinessException("El acudiente no está vinculado al estudiante.");
        }
        guardianDao.setPrimary(studentId, guardianId);
    }

    public void enrollSubjects(int studentId, List<Integer> subjectIds, int year, String period, String section) {
        StudentRules.validatePeriod(period);
        String sec = (section == null || section.isBlank()) ? "A" : section.trim();

        Student student = getStudent(studentId);
        StudentRules.validateCanEnrollSubjects(student.status());

        Optional<Enrollment> enrollmentOpt = enrollmentDao.findByStudentAndYear(studentId, year);
        if (enrollmentOpt.isEmpty() || !"Activa".equalsIgnoreCase(enrollmentOpt.get().status())) {
            throw new BusinessException("El estudiante no tiene una matrícula activa en el año académico.");
        }

        int studentGradeId = enrollmentOpt.get().gradeId();
        if (subjectIds == null || subjectIds.isEmpty()) {
            throw new BusinessException("Debe seleccionar al menos una materia para inscribir.");
        }

        List<SubjectEnrollment> toEnroll = new ArrayList<>();
        Set<Integer> uniqueSubjectIds = new LinkedHashSet<>();
        for (int subjectId : subjectIds) {
            if (!uniqueSubjectIds.add(subjectId)) {
                throw new BusinessException("No se puede inscribir la misma materia más de una vez en la solicitud.");
            }
            Subject subject = subjectDao.findById(subjectId)
                    .orElseThrow(() -> new BusinessException("La materia no existe."));
            if (!subject.active()) {
                throw new BusinessException("La materia no se encuentra activa.");
            }
            if (subject.gradeId() != studentGradeId) {
                throw new BusinessException("Solo se pueden inscribir materias correspondientes al grado actual del estudiante.");
            }
            if (subjectEnrollmentDao.exists(studentId, subjectId, year, period)) {
                throw new BusinessException("El estudiante ya está inscrito en la materia para este período.");
            }
            toEnroll.add(new SubjectEnrollment(
                    0,
                    studentId,
                    subjectId,
                    year,
                    period,
                    sec,
                    LocalDate.now(),
                    "Activa"
            ));
        }

        subjectEnrollmentDao.insertAll(toEnroll);
    }

    public List<Subject> getSubjectsForGrade(int gradeId) {
        if (gradeId <= 0) {
            return List.of();
        }
        return subjectDao.findByGrade(gradeId);
    }

    public List<SubjectEnrollment> getSubjectEnrollments(int studentId, int year, String period) {
        if (studentId <= 0 || year <= 0) {
            return List.of();
        }
        return subjectEnrollmentDao.findByStudent(studentId, year, period);
    }

    private void validateStudentData(Student student) {
        if (student == null) {
            throw new BusinessException("Los datos del estudiante son obligatorios.");
        }
        if (!InputValidator.isNotBlank(student.firstName())) {
            throw new BusinessException("El primer nombre del estudiante es obligatorio.");
        }
        if (!InputValidator.isNotBlank(student.lastName())) {
            throw new BusinessException("El primer apellido del estudiante es obligatorio.");
        }
        if (!InputValidator.isValidDocument(student.documentNumber())) {
            throw new BusinessException("El documento del estudiante no es válido.");
        }
        StudentRules.validateBirthDate(student.birthDate());
        if (student.phone() != null && !student.phone().isBlank() && !InputValidator.isValidPhone(student.phone())) {
            throw new BusinessException("El formato del teléfono del estudiante no es válido.");
        }
        if (student.email() != null && !student.email().isBlank() && !InputValidator.isValidEmail(student.email())) {
            throw new BusinessException("El formato del correo electrónico del estudiante no es válido.");
        }
    }

    private void validateGuardianData(Guardian g) {
        if (g == null) {
            throw new BusinessException("Los datos del acudiente son obligatorios.");
        }
        if (!InputValidator.isNotBlank(g.firstName())) {
            throw new BusinessException("El nombre del acudiente es obligatorio.");
        }
        if (!InputValidator.isNotBlank(g.lastName())) {
            throw new BusinessException("El apellido del acudiente es obligatorio.");
        }
        if (!InputValidator.isValidDocument(g.documentNumber())) {
            throw new BusinessException("El documento del acudiente no es válido.");
        }
        if (!InputValidator.isNotBlank(g.phone())) {
            throw new BusinessException("El teléfono principal del acudiente es obligatorio.");
        }
        if (!InputValidator.isValidPhone(g.phone())) {
            throw new BusinessException("El formato del teléfono principal del acudiente no es válido.");
        }
        if (g.secondaryPhone() != null && !g.secondaryPhone().isBlank() && !InputValidator.isValidPhone(g.secondaryPhone())) {
            throw new BusinessException("El formato del teléfono secundario del acudiente no es válido.");
        }
        if (g.email() != null && !g.email().isBlank() && !InputValidator.isValidEmail(g.email())) {
            throw new BusinessException("El formato del correo electrónico del acudiente no es válido.");
        }
    }
}
