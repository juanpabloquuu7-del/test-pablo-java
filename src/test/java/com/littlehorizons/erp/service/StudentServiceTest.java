package com.littlehorizons.erp.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentServiceTest {

    private final Map<Integer, Student> students = new HashMap<>();
    private final Map<Integer, Guardian> guardians = new HashMap<>();
    private final Map<Integer, List<GuardianLink>> studentGuardians = new HashMap<>();
    private final Map<Integer, Enrollment> enrollments = new HashMap<>();
    private final Map<Integer, GradeLevel> gradeLevels = new HashMap<>();
    private final Map<Integer, Subject> subjects = new HashMap<>();
    private final List<SubjectEnrollment> subjectEnrollments = new ArrayList<>();

    private final AtomicInteger studentSeq = new AtomicInteger(100);
    private final AtomicInteger guardianSeq = new AtomicInteger(200);
    private final AtomicInteger enrollmentSeq = new AtomicInteger(300);
    private final AtomicInteger subjectSeq = new AtomicInteger(400);

    private StudentService service;

    @BeforeEach
    void setUp() {
        students.clear();
        guardians.clear();
        studentGuardians.clear();
        enrollments.clear();
        gradeLevels.clear();
        subjects.clear();
        subjectEnrollments.clear();

        // 12 grades
        String[] names = {
                "Preescolar", "Primero", "Segundo", "Tercero", "Cuarto", "Quinto",
                "Sexto", "SÃ©ptimo", "Octavo", "Noveno", "DÃ©cimo", "Once"
        };
        for (int i = 0; i < names.length; i++) {
            int id = i + 1;
            gradeLevels.put(id, new GradeLevel(id, names[i], i));
        }

        StudentDao studentDao = new StudentDao() {
            @Override
            public int insert(Student student) {
                int id = studentSeq.incrementAndGet();
                Student saved = new Student(id, student.documentType(), student.documentNumber(),
                        student.firstName(), student.middleName(), student.lastName(), student.secondLastName(),
                        student.birthDate(), student.gender(), student.address(), student.phone(), student.email(),
                        student.bloodType(), student.allergies(), student.medicalConditions(), student.admissionDate(),
                        student.status());
                students.put(id, saved);
                return id;
            }

            @Override
            public int update(Student student) {
                students.put(student.id(), student);
                return 1;
            }

            @Override
            public Optional<Student> findById(int id) {
                return Optional.ofNullable(students.get(id));
            }

            @Override
            public Optional<Student> findByDocument(String documentNumber) {
                return students.values().stream()
                        .filter(s -> s.documentNumber().equals(documentNumber))
                        .findFirst();
            }

            @Override
            public List<Student> search(String text) {
                return students.values().stream()
                        .filter(s -> s.fullName().contains(text) || s.documentNumber().contains(text))
                        .toList();
            }

            @Override
            public List<Student> findByGrade(int gradeId, int year) {
                List<Integer> sIds = enrollments.values().stream()
                        .filter(e -> e.gradeId() == gradeId && e.year() == year && "Activa".equalsIgnoreCase(e.status()))
                        .map(Enrollment::studentId)
                        .toList();
                return sIds.stream().map(students::get).toList();
            }

            @Override
            public int updateStatus(int studentId, String status) {
                Student s = students.get(studentId);
                if (s != null) {
                    students.put(studentId, new Student(s.id(), s.documentType(), s.documentNumber(),
                            s.firstName(), s.middleName(), s.lastName(), s.secondLastName(), s.birthDate(),
                            s.gender(), s.address(), s.phone(), s.email(), s.bloodType(), s.allergies(),
                            s.medicalConditions(), s.admissionDate(), status));
                    return 1;
                }
                return 0;
            }
        };

        GuardianDao guardianDao = new GuardianDao() {
            @Override
            public int insert(Guardian guardian) {
                int id = guardianSeq.incrementAndGet();
                Guardian saved = new Guardian(id, guardian.documentType(), guardian.documentNumber(),
                        guardian.firstName(), guardian.middleName(), guardian.lastName(), guardian.secondLastName(),
                        guardian.relationship(), guardian.phone(), guardian.secondaryPhone(), guardian.email(),
                        guardian.address(), guardian.occupation(), guardian.financialResponsible());
                guardians.put(id, saved);
                return id;
            }

            @Override
            public int update(Guardian guardian) {
                guardians.put(guardian.id(), guardian);
                return 1;
            }

            @Override
            public Optional<Guardian> findById(int id) {
                return Optional.ofNullable(guardians.get(id));
            }

            @Override
            public Optional<Guardian> findByDocument(String documentNumber) {
                return guardians.values().stream()
                        .filter(g -> g.documentNumber().equals(documentNumber))
                        .findFirst();
            }

            @Override
            public List<GuardianLink> findByStudent(int studentId) {
                return new ArrayList<>(studentGuardians.getOrDefault(studentId, List.of()));
            }

            @Override
            public void link(int studentId, int guardianId, boolean primary, boolean emergencyContact) {
                Guardian g = guardians.get(guardianId);
                List<GuardianLink> list = studentGuardians.computeIfAbsent(studentId, k -> new ArrayList<>());
                list.removeIf(l -> l.guardian().id() == guardianId);
                list.add(new GuardianLink(g, primary, emergencyContact));
            }

            @Override
            public void unlink(int studentId, int guardianId) {
                List<GuardianLink> list = studentGuardians.get(studentId);
                if (list != null) {
                    list.removeIf(l -> l.guardian().id() == guardianId);
                }
            }

            @Override
            public void setPrimary(int studentId, int guardianId) {
                List<GuardianLink> list = studentGuardians.get(studentId);
                if (list != null) {
                    List<GuardianLink> updated = new ArrayList<>();
                    for (GuardianLink link : list) {
                        updated.add(new GuardianLink(link.guardian(), link.guardian().id() == guardianId, link.emergencyContact()));
                    }
                    studentGuardians.put(studentId, updated);
                }
            }

            @Override
            public int countStudents(int guardianId) {
                int count = 0;
                for (List<GuardianLink> links : studentGuardians.values()) {
                    if (links.stream().anyMatch(l -> l.guardian().id() == guardianId)) count++;
                }
                return count;
            }
        };

        EnrollmentDao enrollmentDao = new EnrollmentDao() {
            @Override
            public int insert(Enrollment enrollment) {
                int id = enrollmentSeq.incrementAndGet();
                Enrollment saved = new Enrollment(id, enrollment.studentId(), enrollment.gradeId(),
                        enrollment.year(), enrollment.status(), enrollment.enrollmentDate(),
                        enrollment.withdrawalDate(), enrollment.notes());
                enrollments.put(id, saved);
                return id;
            }

            @Override
            public Optional<Enrollment> findByStudentAndYear(int studentId, int year) {
                return enrollments.values().stream()
                        .filter(e -> e.studentId() == studentId && e.year() == year)
                        .findFirst();
            }

            @Override
            public List<Enrollment> findByStudent(int studentId) {
                return enrollments.values().stream()
                        .filter(e -> e.studentId() == studentId)
                        .toList();
            }

            @Override
            public int updateGrade(int enrollmentId, int gradeId) {
                Enrollment e = enrollments.get(enrollmentId);
                if (e != null) {
                    enrollments.put(enrollmentId, new Enrollment(e.id(), e.studentId(), gradeId, e.year(),
                            e.status(), e.enrollmentDate(), e.withdrawalDate(), e.notes()));
                    return 1;
                }
                return 0;
            }

            @Override
            public int updateStatus(int enrollmentId, String status, LocalDate withdrawalDate) {
                Enrollment e = enrollments.get(enrollmentId);
                if (e != null) {
                    enrollments.put(enrollmentId, new Enrollment(e.id(), e.studentId(), e.gradeId(), e.year(),
                            status, e.enrollmentDate(), withdrawalDate, e.notes()));
                    return 1;
                }
                return 0;
            }
        };

        GradeLevelDao gradeLevelDao = new GradeLevelDao() {
            @Override
            public List<GradeLevel> findAll() {
                return new ArrayList<>(gradeLevels.values());
            }

            @Override
            public Optional<GradeLevel> findById(int id) {
                return Optional.ofNullable(gradeLevels.get(id));
            }
        };

        StudentRegistrationDao studentRegistrationDao = (student, links, enrollment) -> {
            int sId = studentDao.insert(student);
            for (GuardianLink link : links) {
                int gId = link.guardian().id();
                if (gId <= 0) {
                    gId = guardianDao.insert(link.guardian());
                }
                guardianDao.link(sId, gId, link.primary(), link.emergencyContact());
            }
            Enrollment e = new Enrollment(0, sId, enrollment.gradeId(), enrollment.year(), enrollment.status(),
                    enrollment.enrollmentDate(), enrollment.withdrawalDate(), enrollment.notes());
            enrollmentDao.insert(e);
            return sId;
        };

        SubjectDao subjectDao = new SubjectDao() {
            @Override
            public int insert(Subject subject) {
                int id = subjectSeq.incrementAndGet();
                Subject saved = new Subject(id, subject.code(), subject.name(), subject.description(),
                        subject.gradeId(), subject.weeklyHours(), subject.active());
                subjects.put(id, saved);
                return id;
            }

            @Override
            public Optional<Subject> findById(int id) {
                return Optional.ofNullable(subjects.get(id));
            }

            @Override
            public List<Subject> findByGrade(int gradeId) {
                return subjects.values().stream().filter(s -> s.gradeId() == gradeId).toList();
            }

            @Override
            public List<Subject> findActive() {
                return subjects.values().stream().filter(Subject::active).toList();
            }
        };

        SubjectEnrollmentDao subjectEnrollmentDao = new SubjectEnrollmentDao() {
            @Override
            public int insert(SubjectEnrollment se) {
                subjectEnrollments.add(se);
                return 1;
            }

            @Override
            public void insertAll(List<SubjectEnrollment> list) {
                subjectEnrollments.addAll(list);
            }

            @Override
            public List<SubjectEnrollment> findByStudent(int studentId, int year, String period) {
                return subjectEnrollments.stream()
                        .filter(se -> se.studentId() == studentId && se.year() == year && se.period().equals(period))
                        .toList();
            }

            @Override
            public List<SubjectEnrollment> findByCourse(int subjectId, int year, String period, String section) {
                return subjectEnrollments.stream()
                        .filter(se -> se.subjectId() == subjectId && se.year() == year && se.period().equals(period)
                                && (section == null || section.equals(se.section())))
                        .toList();
            }

            @Override
            public boolean exists(int studentId, int subjectId, int year, String period) {
                return subjectEnrollments.stream()
                        .anyMatch(se -> se.studentId() == studentId && se.subjectId() == subjectId
                                && se.year() == year && se.period().equals(period));
            }

            @Override
            public boolean existsInYear(int studentId, int subjectId, int year) {
                return subjectEnrollments.stream()
                        .anyMatch(se -> se.studentId() == studentId && se.subjectId() == subjectId && se.year() == year);
            }

            @Override
            public int updateStatus(int enrollmentId, String status) {
                return 1;
            }
        };

        service = new StudentService(
                studentDao,
                guardianDao,
                enrollmentDao,
                gradeLevelDao,
                studentRegistrationDao,
                subjectDao,
                subjectEnrollmentDao
        );
    }

    @Test
    void testRegisterStudentSuccess() {
        Student student = new Student(0, "TI", "1098765432", "Carlos", "Eduardo", "Gomez", "Silva",
                LocalDate.of(2015, 6, 15), "M", "Calle 10 # 20-30", "3111234567", "carlos@correo.com",
                "O+", null, null, LocalDate.now(), "Activo");

        Guardian g = new Guardian(0, "CC", "52001122", "Marta", null, "Silva", null, "Madre",
                "3109876543", null, "marta@correo.com", "Calle 10 # 20-30", "Ingeniera", true);

        List<GuardianLink> guardians = List.of(new GuardianLink(g, true, true));

        // Grade id 7 is SÃ©ptimo (numeric level 6 -> ref age 11). Born 2015-06-15, in 2026 age is 10. |10-11|=1 <= 3 -> ok
        int sId = service.registerStudent(student, guardians, 7, "MatrÃ­cula inicial");
        assertTrue(sId > 0);

        Student created = service.getStudent(sId);
        assertNotNull(created);
        assertEquals("Carlos", created.firstName());

        List<GuardianLink> links = service.getGuardians(sId);
        assertEquals(1, links.size());
        assertTrue(links.get(0).primary());
    }

    @Test
    void testRegisterStudentDuplicateDocumentThrows() {
        Student s1 = new Student(0, "TI", "1098765432", "Carlos", null, "Gomez", null,
                LocalDate.of(2015, 6, 15), "M", null, null, null, null, null, null, LocalDate.now(), "Activo");
        Guardian g = new Guardian(0, "CC", "52001122", "Marta", null, "Silva", null, "Madre",
                "3109876543", null, null, null, null, true);
        service.registerStudent(s1, List.of(new GuardianLink(g, true, true)), 7, null);

        Student s2 = new Student(0, "TI", "1098765432", "Andres", null, "Perez", null,
                LocalDate.of(2015, 6, 15), "M", null, null, null, null, null, null, LocalDate.now(), "Activo");
        assertThrows(BusinessException.class, () ->
                service.registerStudent(s2, List.of(new GuardianLink(g, true, true)), 7, null));
    }

    @Test
    void testRegisterStudentAgeInconsistentThrows() {
        // Grade 1 is Preescolar (level 0 -> ref age 5). Born 2010 -> age 16 -> diff 11 > 3 -> throws
        Student s = new Student(0, "TI", "1098765432", "Carlos", null, "Gomez", null,
                LocalDate.of(2010, 6, 15), "M", null, null, null, null, null, null, LocalDate.now(), "Activo");
        Guardian g = new Guardian(0, "CC", "52001122", "Marta", null, "Silva", null, "Madre",
                "3109876543", null, null, null, null, true);

        assertThrows(BusinessException.class, () ->
                service.registerStudent(s, List.of(new GuardianLink(g, true, true)), 1, null));
    }

    @Test
    void testUnlinkGuardianRules() {
        Student s = new Student(0, "TI", "1098765432", "Carlos", null, "Gomez", null,
                LocalDate.of(2015, 6, 15), "M", null, null, null, null, null, null, LocalDate.now(), "Activo");
        Guardian g1 = new Guardian(0, "CC", "52001122", "Marta", null, "Silva", null, "Madre",
                "3109876543", null, null, null, null, true);
        int sId = service.registerStudent(s, List.of(new GuardianLink(g1, true, true)), 7, null);

        int g1Id = service.getGuardians(sId).get(0).guardian().id();

        // Cannot unlink the only guardian
        assertThrows(BusinessException.class, () -> service.unlinkGuardian(sId, g1Id));

        // Add a second guardian (non-primary)
        Guardian g2 = new Guardian(0, "CC", "53001122", "Jorge", null, "Gomez", null, "Padre",
                "3119876543", null, null, null, null, true);
        int g2Id = service.saveGuardian(g2);
        service.linkGuardian(sId, g2Id, false, true);

        // Cannot unlink primary guardian without changing primary first
        assertThrows(BusinessException.class, () -> service.unlinkGuardian(sId, g1Id));

        // Can unlink non-primary guardian
        assertDoesNotThrow(() -> service.unlinkGuardian(sId, g2Id));
    }

    @Test
    void testChangeStatusGraduadoAndRetirado() {
        Student s = new Student(0, "TI", "1098765432", "Carlos", null, "Gomez", null,
                LocalDate.of(2009, 6, 15), "M", null, null, null, null, null, null, LocalDate.now(), "Activo");
        Guardian g = new Guardian(0, "CC", "52001122", "Marta", null, "Silva", null, "Madre",
                "3109876543", null, null, null, null, true);

        // Grade 12 is Once (numeric level 11 -> ref age 16). Born 2009 -> age 16 in 2026 -> ok
        int sId = service.registerStudent(s, List.of(new GuardianLink(g, true, true)), 12, null);

        // Graduate student from Once
        assertDoesNotThrow(() -> service.changeStudentStatus(sId, "Graduado"));
        assertEquals("Graduado", service.getStudent(sId).status());

        // Once graduated, cannot change to Activo
        assertThrows(BusinessException.class, () -> service.changeStudentStatus(sId, "Activo"));
    }

    @Test
    void testGraduationForbiddenIfNotOnce() {
        Student s = new Student(0, "TI", "1098765432", "Carlos", null, "Gomez", null,
                LocalDate.of(2015, 6, 15), "M", null, null, null, null, null, null, LocalDate.now(), "Activo");
        Guardian g = new Guardian(0, "CC", "52001122", "Marta", null, "Silva", null, "Madre",
                "3109876543", null, null, null, null, true);

        // Grade 7 is SÃ©ptimo (level 6)
        int sId = service.registerStudent(s, List.of(new GuardianLink(g, true, true)), 7, null);

        // Cannot graduate from SÃ©ptimo
        assertThrows(BusinessException.class, () -> service.changeStudentStatus(sId, "Graduado"));
    }

    @Test
    void testEnrollSubjectsRules() {
        Student s = new Student(0, "TI", "1098765432", "Carlos", null, "Gomez", null,
                LocalDate.of(2015, 6, 15), "M", null, null, null, null, null, null, LocalDate.now(), "Activo");
        Guardian g = new Guardian(0, "CC", "52001122", "Marta", null, "Silva", null, "Madre",
                "3109876543", null, null, null, null, true);
        int sId = service.registerStudent(s, List.of(new GuardianLink(g, true, true)), 7, null);

        // Add subjects
        Subject subGrade7 = new Subject(10, "MAT-7", "MatemÃ¡ticas 7", null, 7, 4, true);
        subjects.put(10, subGrade7);

        Subject subGrade8 = new Subject(11, "MAT-8", "MatemÃ¡ticas 8", null, 8, 4, true);
        subjects.put(11, subGrade8);

        // Cannot enroll in subject of grade 8 (student is in grade 7)
        assertThrows(BusinessException.class, () ->
                service.enrollSubjects(sId, List.of(11), 2026, "I", "A"));

        // Successfully enroll in subject of grade 7
        assertDoesNotThrow(() ->
                service.enrollSubjects(sId, List.of(10), 2026, "I", "A"));

        // Cannot enroll again in same subject and period (duplicate)
        assertThrows(BusinessException.class, () ->
                service.enrollSubjects(sId, List.of(10), 2026, "I", "A"));

        // Suspend student -> cannot enroll
        service.changeStudentStatus(sId, "Suspendido");
        Subject subCiencias7 = new Subject(12, "CIE-7", "Ciencias 7", null, 7, 4, true);
        subjects.put(12, subCiencias7);

        assertThrows(BusinessException.class, () ->
                service.enrollSubjects(sId, List.of(12), 2026, "I", "A"));
    }

    @Test
    void testGetStudentInvalidIdThrows() {
        assertThrows(BusinessException.class, () -> service.getStudent(-1));
        assertThrows(BusinessException.class, () -> service.getStudent(0));
        assertThrows(BusinessException.class, () -> service.getStudent(99999));
    }

    @Test
    void testSearchStudentsEmpty() {
        assertTrue(service.searchStudents(null).isEmpty());
        assertTrue(service.searchStudents("").isEmpty());
        assertTrue(service.searchStudents("   ").isEmpty());
    }

    @Test
    void testSaveGuardianValidations() {
        assertThrows(BusinessException.class, () -> service.saveGuardian(null));

        // Missing names
        Guardian gNoName = new Guardian(0, "CC", "52001122", "", null, "Silva", null, "Madre", "3109876543", null, null, null, null, true);
        assertThrows(BusinessException.class, () -> service.saveGuardian(gNoName));

        // Invalid document
        Guardian gBadDoc = new Guardian(0, "CC", "123", "Marta", null, "Silva", null, "Madre", "3109876543", null, null, null, null, true);
        assertThrows(BusinessException.class, () -> service.saveGuardian(gBadDoc));

        // Invalid phone
        Guardian gBadPhone = new Guardian(0, "CC", "52001122", "Marta", null, "Silva", null, "Madre", "abc", null, null, null, null, true);
        assertThrows(BusinessException.class, () -> service.saveGuardian(gBadPhone));

        // Duplicate document
        Guardian gValid = new Guardian(0, "CC", "52001122", "Marta", null, "Silva", null, "Madre", "3109876543", null, null, null, null, true);
        service.saveGuardian(gValid);
        Guardian gDup = new Guardian(0, "CC", "52001122", "Maria", null, "Silva", null, "Madre", "3109876544", null, null, null, null, true);
        assertThrows(BusinessException.class, () -> service.saveGuardian(gDup));
    }

    @Test
    void testLinkGuardianAlreadyLinkedThrows() {
        Student s = new Student(0, "TI", "1098765432", "Carlos", null, "Gomez", null,
                LocalDate.of(2015, 6, 15), "M", null, null, null, null, null, null, LocalDate.now(), "Activo");
        Guardian g = new Guardian(0, "CC", "52001122", "Marta", null, "Silva", null, "Madre",
                "3109876543", null, null, null, null, true);
        int sId = service.registerStudent(s, List.of(new GuardianLink(g, true, true)), 7, null);
        int gId = service.getGuardians(sId).get(0).guardian().id();

        assertThrows(BusinessException.class, () -> service.linkGuardian(sId, gId, false, false));
    }

    @Test
    void testEnrollSubjectsDuplicateIdsThrows() {
        Student s = new Student(0, "TI", "1098765432", "Carlos", null, "Gomez", null,
                LocalDate.of(2015, 6, 15), "M", null, null, null, null, null, null, LocalDate.now(), "Activo");
        Guardian g = new Guardian(0, "CC", "52001122", "Marta", null, "Silva", null, "Madre",
                "3109876543", null, null, null, null, true);
        int sId = service.registerStudent(s, List.of(new GuardianLink(g, true, true)), 7, null);

        Subject subGrade7 = new Subject(10, "MAT-7", "Matemáticas 7", null, 7, 4, true);
        subjects.put(10, subGrade7);

        assertThrows(BusinessException.class, () ->
                service.enrollSubjects(sId, List.of(10, 10), 2026, "I", "A"));
    }

    @Test
    void testChangeStatusToSameStatusThrows() {
        Student s = new Student(0, "TI", "1098765432", "Carlos", null, "Gomez", null,
                LocalDate.of(2015, 6, 15), "M", null, null, null, null, null, null, LocalDate.now(), "Activo");
        Guardian g = new Guardian(0, "CC", "52001122", "Marta", null, "Silva", null, "Madre",
                "3109876543", null, null, null, null, true);
        int sId = service.registerStudent(s, List.of(new GuardianLink(g, true, true)), 7, null);

        assertThrows(BusinessException.class, () -> service.changeStudentStatus(sId, "Activo"));
    }
}