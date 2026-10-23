package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.AverageLine;
import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.model.TeachingAssignment;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcademicServiceTest {

    private FakeAcademicRecordDao academicRecordDao;
    private FakeAttendanceDao attendanceDao;
    private FakeStudentDao studentDao;
    private FakeSubjectDao subjectDao;
    private FakeSubjectEnrollmentDao subjectEnrollmentDao;
    private FakeAssignmentDao assignmentDao;
    private AcademicService service;

    private final Student activeStudent = new Student(
            1, "TI", "1010101010", "Juan", "Carlos", "Pérez", "Gómez",
            LocalDate.of(2010, 5, 15), "M", "Calle 1", "3001112233", "juan@test.com",
            "O+", "Ninguna", "Ninguna", LocalDate.of(2020, 2, 1), "Activo"
    );

    private final Student suspendedStudent = new Student(
            2, "TI", "2020202020", "María", null, "Rodríguez", null,
            LocalDate.of(2010, 8, 20), "F", "Calle 2", "3002223344", "maria@test.com",
            "A+", "Ninguna", "Ninguna", LocalDate.of(2020, 2, 1), "Suspendido"
    );

    private final Student retiredStudent = new Student(
            3, "TI", "3030303030", "Carlos", null, "López", null,
            LocalDate.of(2010, 3, 10), "M", "Calle 3", "3003334455", "carlos@test.com",
            "B+", "Ninguna", "Ninguna", LocalDate.of(2020, 2, 1), "Retirado"
    );

    private final Subject mathSubject = new Subject(10, "MAT-01", "Matemáticas", "Álgebra y geometría", 1, 4, true);
    private final Subject spanishSubject = new Subject(20, "ESP-01", "Español", "Lengua castellana", 1, 4, true);

    @BeforeEach
    void setUp() {
        academicRecordDao = new FakeAcademicRecordDao();
        attendanceDao = new FakeAttendanceDao();
        studentDao = new FakeStudentDao();
        subjectDao = new FakeSubjectDao();
        subjectEnrollmentDao = new FakeSubjectEnrollmentDao();
        assignmentDao = new FakeAssignmentDao();

        studentDao.save(activeStudent);
        studentDao.save(suspendedStudent);
        studentDao.save(retiredStudent);

        subjectDao.save(mathSubject);
        subjectDao.save(spanishSubject);

        // Profesor 5 asignado a Matemáticas y Español en 2026
        assignmentDao.assignTeacher(5, 10, 2026);
        assignmentDao.assignTeacher(5, 20, 2026);

        // Estudiante 1 inscrito en Matemáticas en períodos I y II de 2026
        subjectEnrollmentDao.enroll(1, 10, 2026, "I");
        subjectEnrollmentDao.enroll(1, 10, 2026, "II");
        // Estudiante 1 inscrito en Español en período I de 2026
        subjectEnrollmentDao.enroll(1, 20, 2026, "I");

        service = new AcademicService(
                academicRecordDao,
                attendanceDao,
                studentDao,
                subjectDao,
                subjectEnrollmentDao,
                assignmentDao
        );
    }

    // ==========================================
    // PRUEBAS DE NOTAS (Paso 5)
    // ==========================================

    @Test
    @DisplayName("Registrar nota válida 4.5 es aceptada")
    void testRegisterValidGrade() {
        AcademicRecord record = new AcademicRecord(
                0, 1, 10, 5, 2026, "I",
                new BigDecimal("4.5"), "Buen desempeño", LocalDate.now()
        );
        int id = service.registerGrade(record);
        assertTrue(id > 0);
        Optional<AcademicRecord> found = service.findGrade(1, 10, 2026, "I");
        assertTrue(found.isPresent());
        assertEquals(new BigDecimal("4.5"), found.get().grade());
    }

    @Test
    @DisplayName("Notas mayores a 5.0 (5.5) y con más de un decimal (4.25) son rechazadas")
    void testRejectInvalidGrades() {
        // 5.5 rechazado
        AcademicRecord record55 = new AcademicRecord(
                0, 1, 10, 5, 2026, "I",
                new BigDecimal("5.5"), null, LocalDate.now()
        );
        BusinessException ex1 = assertThrows(BusinessException.class, () -> service.registerGrade(record55));
        assertTrue(ex1.getMessage().contains("entre 0.0 y 5.0 con un solo decimal"));

        // 4.25 rechazado
        AcademicRecord record425 = new AcademicRecord(
                0, 1, 10, 5, 2026, "I",
                new BigDecimal("4.25"), null, LocalDate.now()
        );
        BusinessException ex2 = assertThrows(BusinessException.class, () -> service.registerGrade(record425));
        assertTrue(ex2.getMessage().contains("entre 0.0 y 5.0 con un solo decimal"));

        // -0.5 rechazado
        AcademicRecord recordNeg = new AcademicRecord(
                0, 1, 10, 5, 2026, "I",
                new BigDecimal("-0.5"), null, LocalDate.now()
        );
        assertThrows(BusinessException.class, () -> service.registerGrade(recordNeg));
    }

    @Test
    @DisplayName("No se permiten notas para estudiantes que no estén en estado Activo")
    void testRejectNonActiveStudents() {
        // Suspendido
        AcademicRecord recSuspended = new AcademicRecord(
                0, 2, 10, 5, 2026, "I",
                new BigDecimal("4.0"), null, LocalDate.now()
        );
        BusinessException ex1 = assertThrows(BusinessException.class, () -> service.registerGrade(recSuspended));
        assertTrue(ex1.getMessage().contains("Activo"));

        // Retirado
        AcademicRecord recRetired = new AcademicRecord(
                0, 3, 10, 5, 2026, "I",
                new BigDecimal("4.0"), null, LocalDate.now()
        );
        BusinessException ex2 = assertThrows(BusinessException.class, () -> service.registerGrade(recRetired));
        assertTrue(ex2.getMessage().contains("Activo"));
    }

    @Test
    @DisplayName("Períodos I a IV exigen que el estudiante esté inscrito en esa materia y período")
    void testEnrollmentRequiredForPeriods() {
        // No está inscrito en período III
        AcademicRecord recPeriod3 = new AcademicRecord(
                0, 1, 10, 5, 2026, "III",
                new BigDecimal("4.0"), null, LocalDate.now()
        );
        BusinessException ex = assertThrows(BusinessException.class, () -> service.registerGrade(recPeriod3));
        assertTrue(ex.getMessage().contains("no está inscrito"));
    }

    @Test
    @DisplayName("Período Final exige que el estudiante esté inscrito en la materia en al menos un período del año")
    void testFinalPeriodEnrollmentRequirement() {
        // Estudiante 1 está inscrito en Matemáticas en período I y II: nota Final permitida
        AcademicRecord recFinal = new AcademicRecord(
                0, 1, 10, 5, 2026, "Final",
                new BigDecimal("4.8"), "Examen final", LocalDate.now()
        );
        assertDoesNotThrow(() -> service.registerGrade(recFinal));

        // Pero Estudiante 1 NO está inscrito en ninguna materia con id 99
        subjectDao.save(new Subject(99, "SOC-01", "Sociales", "Ciencias sociales", 1, 2, true));
        assignmentDao.assignTeacher(5, 99, 2026);
        AcademicRecord recFinalNotInSubject = new AcademicRecord(
                0, 1, 99, 5, 2026, "Final",
                new BigDecimal("4.0"), null, LocalDate.now()
        );
        BusinessException ex = assertThrows(BusinessException.class, () -> service.registerGrade(recFinalNotInSubject));
        assertTrue(ex.getMessage().contains("al menos un período del año"));
    }

    @Test
    @DisplayName("El profesor debe tener asignada la materia en ese año")
    void testTeacherMustHaveSubjectAssigned() {
        // Profesor 99 no tiene asignada Matemáticas
        AcademicRecord recWrongTeacher = new AcademicRecord(
                0, 1, 10, 99, 2026, "I",
                new BigDecimal("4.0"), null, LocalDate.now()
        );
        BusinessException ex = assertThrows(BusinessException.class, () -> service.registerGrade(recWrongTeacher));
        assertTrue(ex.getMessage().contains("profesor no tiene asignada la materia"));
    }

    @Test
    @DisplayName("Una sola nota por estudiante, materia, año y período")
    void testSingleGradePerPeriod() {
        AcademicRecord rec = new AcademicRecord(
                0, 1, 10, 5, 2026, "I",
                new BigDecimal("3.5"), null, LocalDate.now()
        );
        service.registerGrade(rec);

        // Intento de registrar otra nota en el mismo período lanza BusinessException
        AcademicRecord duplicate = new AcademicRecord(
                0, 1, 10, 5, 2026, "I",
                new BigDecimal("4.0"), null, LocalDate.now()
        );
        BusinessException ex = assertThrows(BusinessException.class, () -> service.registerGrade(duplicate));
        assertTrue(ex.getMessage().contains("Ya existe una calificación"));

        // Pero actualizarla con updateGrade es permitido
        AcademicRecord toUpdate = new AcademicRecord(
                rec.id(), 1, 10, 5, 2026, "I",
                new BigDecimal("4.0"), "Corregida", LocalDate.now()
        );
        assertDoesNotThrow(() -> service.updateGrade(toUpdate));
        assertEquals(new BigDecimal("4.0"), service.findGrade(1, 10, 2026, "I").get().grade());
    }

    @Test
    @DisplayName("Cálculo correcto de los 3 promedios contra cálculo a mano (redondeo a 1 decimal)")
    void testThreeAveragesCalculation() {
        // Estudiante 1 en Matemáticas:
        // Periodo I: 4.5
        // Periodo II: 3.5
        // Periodo Final: 4.8 (¡NO debe entrar en el promedio!)
        subjectEnrollmentDao.enroll(1, 10, 2026, "III");
        subjectEnrollmentDao.enroll(1, 10, 2026, "IV");
        service.registerGrade(new AcademicRecord(0, 1, 10, 5, 2026, "I", new BigDecimal("4.5"), null, LocalDate.now()));
        service.registerGrade(new AcademicRecord(0, 1, 10, 5, 2026, "II", new BigDecimal("3.5"), null, LocalDate.now()));
        service.registerGrade(new AcademicRecord(0, 1, 10, 5, 2026, "III", new BigDecimal("4.0"), null, LocalDate.now()));
        service.registerGrade(new AcademicRecord(0, 1, 10, 5, 2026, "IV", new BigDecimal("5.0"), null, LocalDate.now()));
        service.registerGrade(new AcademicRecord(0, 1, 10, 5, 2026, "Final", new BigDecimal("4.8"), null, LocalDate.now()));

        // 1. Promedio por materia de un estudiante:
        // (4.5 + 3.5 + 4.0 + 5.0) / 4 = 17.0 / 4 = 4.25 -> ROUND(..., 1) = 4.3
        Optional<BigDecimal> subjAvg = service.getSubjectAverage(1, 10, 2026);
        assertTrue(subjAvg.isPresent());
        assertEquals(new BigDecimal("4.3"), subjAvg.get());

        // Estudiante 1 también tiene Español en Período I:
        service.registerGrade(new AcademicRecord(0, 1, 20, 5, 2026, "I", new BigDecimal("3.8"), null, LocalDate.now()));

        // 2. Promedio general del estudiante en un período (Período I):
        // Materias en Período I: Matemáticas (4.5), Español (3.8)
        // (4.5 + 3.8) / 2 = 8.3 / 2 = 4.15 -> ROUND(..., 1) = 4.2
        Optional<BigDecimal> periodAvg = service.getStudentPeriodAverage(1, 2026, "I");
        assertTrue(periodAvg.isPresent());
        assertEquals(new BigDecimal("4.2"), periodAvg.get());

        // 3. Promedio del curso por materia (Matemáticas en Período I):
        // Inscribir a otro estudiante en Matemáticas Período I
        Student studentB = new Student(
                4, "TI", "4040404040", "Luis", null, "Ramírez", null,
                LocalDate.of(2010, 4, 12), "M", "Calle 4", "3004445566", "luis@test.com",
                "O-", "Ninguna", "Ninguna", LocalDate.of(2020, 2, 1), "Activo"
        );
        studentDao.save(studentB);
        subjectEnrollmentDao.enroll(4, 10, 2026, "I");
        service.registerGrade(new AcademicRecord(0, 4, 10, 5, 2026, "I", new BigDecimal("3.5"), null, LocalDate.now()));

        // Estudiantes en Matemáticas Período I: Estudiante 1 (4.5), Estudiante 4 (3.5)
        // (4.5 + 3.5) / 2 = 8.0 / 2 = 4.0 -> ROUND(..., 1) = 4.0
        Optional<BigDecimal> courseAvg = service.getCourseAverage(10, 2026, "I");
        assertTrue(courseAvg.isPresent());
        assertEquals(new BigDecimal("4.0"), courseAvg.get());
    }

    @Test
    @DisplayName("Historial académico completo del estudiante ordenado por año y período")
    void testAcademicHistory() {
        assignmentDao.assignTeacher(5, 10, 2025);
        subjectEnrollmentDao.enroll(1, 10, 2025, "I");
        service.registerGrade(new AcademicRecord(0, 1, 10, 5, 2025, "I", new BigDecimal("3.5"), null, LocalDate.now()));
        service.registerGrade(new AcademicRecord(0, 1, 10, 5, 2026, "I", new BigDecimal("4.5"), null, LocalDate.now()));

        List<AcademicRecord> history = service.getAcademicHistory(1);
        assertEquals(2, history.size());
        assertEquals(2025, history.get(0).year());
        assertEquals(2026, history.get(1).year());
    }

    // ==========================================
    // PRUEBAS DE ASISTENCIA (Paso 6)
    // ==========================================

    @Test
    @DisplayName("Fecha de asistencia futura es rechazada")
    void testRejectFutureAttendanceDate() {
        Attendance future = new Attendance(
                0, 1, 10, LocalDate.now().plusDays(1), "Presente", null, 5
        );
        BusinessException ex = assertThrows(BusinessException.class, () -> service.registerAttendance(future));
        assertTrue(ex.getMessage().contains("no puede ser futura"));
    }

    @Test
    @DisplayName("Estados válidos de asistencia permitidos y estado inválido rechazado")
    void testAttendanceStatuses() {
        LocalDate today = LocalDate.now();
        // Inválido
        Attendance invalid = new Attendance(0, 1, 10, today, "Justificada", null, 5);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.registerAttendance(invalid));
        assertTrue(ex.getMessage().contains("Estado de asistencia inválido"));

        // Válidos
        Attendance p = new Attendance(0, 1, 10, today, "Presente", null, 5);
        assertDoesNotThrow(() -> service.registerAttendance(p));
    }

    @Test
    @DisplayName("Profesor que registra asistencia debe tener asignada la materia")
    void testAttendanceRegisteredByTeacher() {
        Attendance a = new Attendance(0, 1, 10, LocalDate.now(), "Presente", null, 99);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.registerAttendance(a));
        assertTrue(ex.getMessage().contains("no tiene asignada la materia"));
    }

    @Test
    @DisplayName("Solo una asistencia por estudiante, materia y fecha")
    void testSingleAttendancePerStudentSubjectDate() {
        LocalDate today = LocalDate.now();
        Attendance a1 = new Attendance(0, 1, 10, today, "Presente", null, 5);
        service.registerAttendance(a1);

        Attendance a2 = new Attendance(0, 1, 10, today, "Ausente", null, 5);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.registerAttendance(a2));
        assertTrue(ex.getMessage().contains("Ya existe un registro de asistencia"));
    }

    @Test
    @DisplayName("Registro masivo guarda en una transacción y rechaza duplicados")
    void testCourseMassAttendance() {
        LocalDate today = LocalDate.now();
        Student studentB = new Student(
                4, "TI", "4040404040", "Luis", null, "Ramírez", null,
                LocalDate.of(2010, 4, 12), "M", "Calle 4", "3004445566", "luis@test.com",
                "O-", "Ninguna", "Ninguna", LocalDate.of(2020, 2, 1), "Activo"
        );
        studentDao.save(studentB);

        List<Attendance> list = List.of(
                new Attendance(0, 1, 10, today, "Presente", null, 5),
                new Attendance(0, 4, 10, today, "Ausente", "Sin excusa", 5)
        );
        assertDoesNotThrow(() -> service.registerCourseAttendance(list));

        // Intento de re-registrar genera error
        assertThrows(BusinessException.class, () -> service.registerCourseAttendance(list));
    }

    @Test
    @DisplayName("Reporte de ausencias: 4 días distintos en misma semana generan alerta; 3 días no; 5 ausencias el mismo día tampoco")
    void testWeeklyAbsenceThresholdAlert() {
        // Semana de prueba: Lunes a Domingo
        LocalDate monday = LocalDate.of(2026, 3, 2);
        LocalDate tuesday = LocalDate.of(2026, 3, 3);
        LocalDate wednesday = LocalDate.of(2026, 3, 4);
        LocalDate thursday = LocalDate.of(2026, 3, 5);

        // Caso 1: Estudiante 1 tiene 3 días ausente en la semana
        attendanceDao.save(new Attendance(1, 1, 10, monday, "Ausente", null, 5));
        attendanceDao.save(new Attendance(2, 1, 10, tuesday, "Ausente", null, 5));
        attendanceDao.save(new Attendance(3, 1, 10, wednesday, "Ausente", null, 5));

        // Con 3 días ausente y umbral 3 -> NO genera alerta (debe ser estrictamente más que 3)
        List<AcademicService.AbsenceAlertDetail> alerts = service.getAbsenceAlerts(3, 2026);
        assertTrue(alerts.isEmpty(), "Con 3 ausencias no debe generar alerta");

        // Caso 2: Estudiante 2 tiene 5 ausencias en diferentes materias pero EL MISMO DÍA (lunes)
        Student studentB = new Student(
                20, "TI", "5050505050", "Pedro", null, "Sánchez", null,
                LocalDate.of(2010, 1, 1), "M", "Calle 5", "3005556677", "pedro@test.com",
                "A-", "Ninguna", "Ninguna", LocalDate.of(2020, 2, 1), "Activo"
        );
        studentDao.save(studentB);
        for (int m = 1; m <= 5; m++) {
            attendanceDao.save(new Attendance(10 + m, 20, m, monday, "Ausente", null, 5));
        }

        alerts = service.getAbsenceAlerts(3, 2026);
        assertTrue(alerts.isEmpty(), "5 ausencias el mismo día cuentan como 1 solo día, no debe alertar");

        // Caso 3: Agregamos el 4to día ausente para el Estudiante 1 (jueves)
        attendanceDao.save(new Attendance(4, 1, 10, thursday, "Ausente", null, 5));

        alerts = service.getAbsenceAlerts(3, 2026);
        assertEquals(1, alerts.size(), "Con 4 días distintos en la semana debe alertar");
        AcademicService.AbsenceAlertDetail alert = alerts.get(0);
        assertEquals(1, alert.studentId());
        assertEquals("Juan Carlos Pérez Gómez", alert.studentName());
        assertEquals(4, alert.weeklyAbsenceDays());
        assertEquals(4, alert.totalYearAbsences());
    }

    // ==========================================
    // PRUEBAS DE ENDURECIMIENTO Y VALIDACIONES EXTRA
    // ==========================================

    @Test
    @DisplayName("Validación de prerrequisitos de calificación rechaza IDs inexistentes o años inválidos")
    void testValidateGradePrerequisitesHardening() {
        // Año inválido
        assertThrows(BusinessException.class, () ->
                service.validateGradePrerequisites(1, 10, -1, "I", 5));
        assertThrows(BusinessException.class, () ->
                service.validateGradePrerequisites(1, 10, 0, "I", 5));

        // Período inválido o nulo
        assertThrows(BusinessException.class, () ->
                service.validateGradePrerequisites(1, 10, 2026, null, 5));
        assertThrows(BusinessException.class, () ->
                service.validateGradePrerequisites(1, 10, 2026, "V", 5));

        // Estudiante inexistente
        assertThrows(BusinessException.class, () ->
                service.validateGradePrerequisites(9999, 10, 2026, "I", 5));

        // Estudiante no activo (suspendido)
        assertThrows(BusinessException.class, () ->
                service.validateGradePrerequisites(2, 10, 2026, "I", 5));

        // Materia inexistente
        assertThrows(BusinessException.class, () ->
                service.validateGradePrerequisites(1, 9999, 2026, "I", 5));

        // Profesor sin la materia asignada
        assertThrows(BusinessException.class, () ->
                service.validateGradePrerequisites(1, 10, 2026, "I", 9999));

        // Prerrequisitos válidos no lanzan excepción
        assertDoesNotThrow(() ->
                service.validateGradePrerequisites(1, 10, 2026, "I", 5));
    }

    @Test
    @DisplayName("Consultas de promedios rechazan IDs inexistentes y años inválidos")
    void testAveragesWithInvalidIds() {
        // Estudiante inexistente en promedio de período
        assertThrows(BusinessException.class, () ->
                service.getStudentPeriodAverage(9999, 2026, "I"));

        // Año inválido en promedio de período
        assertThrows(BusinessException.class, () ->
                service.getStudentPeriodAverage(1, -2026, "I"));

        // Período inválido en promedio de período
        assertThrows(BusinessException.class, () ->
                service.getStudentPeriodAverage(1, 2026, "Final"));

        // Materia inexistente en promedio de curso
        assertThrows(BusinessException.class, () ->
                service.getCourseAverage(9999, 2026, "I"));

        // Año inválido en promedio de curso
        assertThrows(BusinessException.class, () ->
                service.getCourseAverage(10, 0, "I"));

        // Estudiante inexistente en promedio por materia
        assertThrows(BusinessException.class, () ->
                service.getSubjectAverage(9999, 10, 2026));

        // Materia inexistente en promedio por materia
        assertThrows(BusinessException.class, () ->
                service.getSubjectAverage(1, 9999, 2026));

        // Estudiante inexistente en resúmenes o lista de promedios
        assertThrows(BusinessException.class, () ->
                service.getSubjectSummariesByStudent(9999, 2026));
        assertThrows(BusinessException.class, () ->
                service.getSubjectAveragesByStudent(9999, 2026));

        // Estudiante inexistente en total de ausencias
        assertThrows(BusinessException.class, () ->
                service.getTotalAbsences(9999, 2026));
    }

    @Test
    @DisplayName("Prerrequisitos de asistencia de curso validan año de fecha y asignación docente")
    void testCourseAttendancePrerequisitesHardening() {
        LocalDate today = LocalDate.now();

        // Año <= 0
        assertThrows(BusinessException.class, () ->
                service.validateCourseAttendancePrerequisites(10, 5, -1, today));

        // Fecha nula o futura
        assertThrows(BusinessException.class, () ->
                service.validateCourseAttendancePrerequisites(10, 5, 2026, null));
        assertThrows(BusinessException.class, () ->
                service.validateCourseAttendancePrerequisites(10, 5, 2026, today.plusDays(2)));

        // Año de la fecha no coincide con el año académico seleccionado
        LocalDate date2025 = LocalDate.of(2025, 5, 10);
        assertThrows(BusinessException.class, () ->
                service.validateCourseAttendancePrerequisites(10, 5, 2026, date2025));

        // Materia inexistente
        assertThrows(BusinessException.class, () ->
                service.validateCourseAttendancePrerequisites(9999, 5, 2026, today));

        // Profesor no asignado
        assertThrows(BusinessException.class, () ->
                service.validateCourseAttendancePrerequisites(10, 9999, 2026, today));

        // Prerrequisitos válidos no fallan
        assertDoesNotThrow(() ->
                service.validateCourseAttendancePrerequisites(10, 5, today.getYear(), today));
    }

    @Test
    @DisplayName("Consulta de estudiantes inscritos valida materia, año y período")
    void testGetEnrolledStudentsValidation() {
        assertThrows(BusinessException.class, () ->
                service.getEnrolledStudents(9999, 2026, "I", "A"));
        assertThrows(BusinessException.class, () ->
                service.getEnrolledStudents(10, -1, "I", "A"));
        assertThrows(BusinessException.class, () ->
                service.getEnrolledStudents(10, 2026, null, "A"));
        assertThrows(BusinessException.class, () ->
                service.getEnrolledStudents(10, 2026, "Invalido", "A"));

        List<Student> enrolled = service.getEnrolledStudents(10, 2026, "I", "A");
        assertNotNull(enrolled);
        assertEquals(1, enrolled.size());
        assertEquals(1, enrolled.get(0).id());
    }

    @Test
    @DisplayName("Verificación de asistencia existente (hasAttendance)")
    void testHasAttendanceCheck() {
        LocalDate today = LocalDate.now();
        assertFalse(service.hasAttendance(1, 10, today));

        Attendance att = new Attendance(0, 1, 10, today, "Presente", null, 5);
        service.registerAttendance(att);

        assertTrue(service.hasAttendance(1, 10, today));
        assertFalse(service.hasAttendance(1, 10, today.minusDays(1)));
    }

    @Test
    @DisplayName("Reporte de alertas valida umbral y año mayores o iguales a cero")
    void testAbsenceAlertsValidation() {
        assertThrows(BusinessException.class, () -> service.getAbsenceAlerts(-1, 2026));
        assertThrows(BusinessException.class, () -> service.getAbsenceAlerts(3, 0));
        assertThrows(BusinessException.class, () -> service.getAbsenceAlerts(3, -2026));
    }

    @Test
    @DisplayName("Registro de nota rechaza registros nulos, notas fuera de rango 0.0-5.0 y fechas futuras")
    void testGradeRecordValidationHardening() {
        assertThrows(BusinessException.class, () -> service.registerGrade(null));

        // Nota negativa
        AcademicRecord recNeg = new AcademicRecord(0, 1, 10, 5, 2026, "I", new BigDecimal("-0.1"), null, LocalDate.now());
        assertThrows(BusinessException.class, () -> service.registerGrade(recNeg));

        // Nota mayor a 5.0
        AcademicRecord recOver = new AcademicRecord(0, 1, 10, 5, 2026, "I", new BigDecimal("5.1"), null, LocalDate.now());
        assertThrows(BusinessException.class, () -> service.registerGrade(recOver));

        // Fecha de registro futura
        AcademicRecord recFuture = new AcademicRecord(0, 1, 10, 5, 2026, "I", new BigDecimal("4.0"), null, LocalDate.now().plusDays(1));
        assertThrows(BusinessException.class, () -> service.registerGrade(recFuture));
    }

    // ==========================================
    // FAKE DAOS PARA PRUEBAS SIN BASE DE DATOS
    // ==========================================

    private static class FakeAcademicRecordDao implements AcademicRecordDao {
        private final List<AcademicRecord> records = new ArrayList<>();
        private int seq = 1;

        @Override
        public int insert(AcademicRecord record) {
            AcademicRecord inserted = new AcademicRecord(
                    seq++, record.studentId(), record.subjectId(), record.teacherId(),
                    record.year(), record.period(), record.grade(), record.observations(), record.recordDate()
            );
            records.add(inserted);
            return inserted.id();
        }

        @Override
        public int update(AcademicRecord record) {
            records.removeIf(r -> (record.id() > 0 && r.id() == record.id())
                    || (r.studentId() == record.studentId() && r.subjectId() == record.subjectId()
                    && r.year() == record.year() && r.period().equals(record.period())));
            records.add(record);
            return 1;
        }

        @Override
        public Optional<AcademicRecord> find(int studentId, int subjectId, int year, String period) {
            return records.stream()
                    .filter(r -> r.studentId() == studentId && r.subjectId() == subjectId
                            && r.year() == year && r.period().equalsIgnoreCase(period))
                    .findFirst();
        }

        @Override
        public List<AcademicRecord> findByStudent(int studentId) {
            return records.stream()
                    .filter(r -> r.studentId() == studentId)
                    .sorted((a, b) -> {
                        int cmp = Integer.compare(a.year(), b.year());
                        return cmp != 0 ? cmp : a.period().compareTo(b.period());
                    })
                    .toList();
        }

        @Override
        public List<AcademicRecord> findByStudentAndYear(int studentId, int year) {
            return records.stream()
                    .filter(r -> r.studentId() == studentId && r.year() == year)
                    .toList();
        }

        @Override
        public List<AcademicRecord> findByCourse(int subjectId, int year, String period) {
            return records.stream()
                    .filter(r -> r.subjectId() == subjectId && r.year() == year && r.period().equalsIgnoreCase(period))
                    .toList();
        }

        @Override
        public Optional<BigDecimal> findAverageByStudentAndSubject(int studentId, int subjectId, int year) {
            List<AcademicRecord> matching = records.stream()
                    .filter(r -> r.studentId() == studentId && r.subjectId() == subjectId
                            && r.year() == year && !"Final".equalsIgnoreCase(r.period()))
                    .toList();
            if (matching.isEmpty()) return Optional.empty();
            BigDecimal sum = matching.stream().map(AcademicRecord::grade).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = sum.divide(BigDecimal.valueOf(matching.size()), 4, RoundingMode.HALF_UP)
                    .setScale(1, RoundingMode.HALF_UP);
            return Optional.of(avg);
        }

        @Override
        public Optional<BigDecimal> findAverageByStudentAndPeriod(int studentId, int year, String period) {
            List<AcademicRecord> matching = records.stream()
                    .filter(r -> r.studentId() == studentId && r.year() == year
                            && r.period().equalsIgnoreCase(period) && !"Final".equalsIgnoreCase(r.period()))
                    .toList();
            if (matching.isEmpty()) return Optional.empty();
            BigDecimal sum = matching.stream().map(AcademicRecord::grade).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = sum.divide(BigDecimal.valueOf(matching.size()), 4, RoundingMode.HALF_UP)
                    .setScale(1, RoundingMode.HALF_UP);
            return Optional.of(avg);
        }

        @Override
        public Optional<BigDecimal> findCourseAverage(int subjectId, int year, String period) {
            List<AcademicRecord> matching = records.stream()
                    .filter(r -> r.subjectId() == subjectId && r.year() == year
                            && (period == null || r.period().equalsIgnoreCase(period))
                            && !"Final".equalsIgnoreCase(r.period()))
                    .toList();
            if (matching.isEmpty()) return Optional.empty();
            BigDecimal sum = matching.stream().map(AcademicRecord::grade).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = sum.divide(BigDecimal.valueOf(matching.size()), 4, RoundingMode.HALF_UP)
                    .setScale(1, RoundingMode.HALF_UP);
            return Optional.of(avg);
        }

        @Override
        public List<AverageLine> findSubjectAveragesByStudent(int studentId, int year) {
            return List.of();
        }

        @Override
        public List<AverageLine> findCourseAveragesByYearAndPeriod(int year, String period) {
            return List.of();
        }
    }

    private static class FakeAttendanceDao implements AttendanceDao {
        private final List<Attendance> list = new ArrayList<>();
        private int seq = 1;

        public void save(Attendance a) {
            list.add(a);
        }

        @Override
        public int insert(Attendance attendance) {
            Attendance a = new Attendance(
                    seq++, attendance.studentId(), attendance.subjectId(), attendance.date(),
                    attendance.status(), attendance.observations(), attendance.registeredBy()
            );
            list.add(a);
            return a.id();
        }

        @Override
        public void insertAll(List<Attendance> attendances) {
            attendances.forEach(this::insert);
        }

        @Override
        public int update(Attendance attendance) {
            list.removeIf(a -> a.id() == attendance.id()
                    || (a.studentId() == attendance.studentId() && a.subjectId() == attendance.subjectId() && a.date().equals(attendance.date())));
            list.add(attendance);
            return 1;
        }

        @Override
        public Optional<Attendance> find(int studentId, int subjectId, LocalDate date) {
            return list.stream()
                    .filter(a -> a.studentId() == studentId && a.subjectId() == subjectId && a.date().equals(date))
                    .findFirst();
        }

        @Override
        public boolean exists(int studentId, int subjectId, LocalDate date) {
            return find(studentId, subjectId, date).isPresent();
        }

        @Override
        public List<Attendance> findByStudentAndRange(int studentId, LocalDate start, LocalDate end) {
            return list.stream()
                    .filter(a -> a.studentId() == studentId && !a.date().isBefore(start) && !a.date().isAfter(end))
                    .toList();
        }

        @Override
        public int countAbsenceDaysByWeek(int studentId, int year, int week) {
            int targetYearWeek = year * 100 + week;
            return (int) list.stream()
                    .filter(a -> a.studentId() == studentId && "Ausente".equalsIgnoreCase(a.status()))
                    .filter(a -> toYearWeek(a.date()) == targetYearWeek)
                    .map(Attendance::date)
                    .distinct()
                    .count();
        }

        @Override
        public int countAbsenceDaysByDate(int studentId, LocalDate date) {
            int targetYearWeek = toYearWeek(date);
            return (int) list.stream()
                    .filter(a -> a.studentId() == studentId && "Ausente".equalsIgnoreCase(a.status()))
                    .filter(a -> toYearWeek(a.date()) == targetYearWeek)
                    .map(Attendance::date)
                    .distinct()
                    .count();
        }

        @Override
        public List<WeeklyAbsenceAlert> findStudentsAboveThreshold(int threshold, int year) {
            // Group by studentId and yearWeek, count distinct dates with Ausente
            Map<String, Set<LocalDate>> grouped = new HashMap<>();
            for (Attendance a : list) {
                if ("Ausente".equalsIgnoreCase(a.status())) {
                    int yw = toYearWeek(a.date());
                    String key = a.studentId() + "-" + yw;
                    grouped.computeIfAbsent(key, k -> new HashSet<>()).add(a.date());
                }
            }
            List<WeeklyAbsenceAlert> alerts = new ArrayList<>();
            for (Map.Entry<String, Set<LocalDate>> entry : grouped.entrySet()) {
                if (entry.getValue().size() > threshold) {
                    String[] parts = entry.getKey().split("-");
                    int stId = Integer.parseInt(parts[0]);
                    int yw = Integer.parseInt(parts[1]);
                    alerts.add(new WeeklyAbsenceAlert(stId, yw, entry.getValue().size()));
                }
            }
            return alerts;
        }

        @Override
        public int countTotalAbsences(int studentId, int year) {
            return (int) list.stream()
                    .filter(a -> a.studentId() == studentId && "Ausente".equalsIgnoreCase(a.status()) && a.date().getYear() == year)
                    .count();
        }

        private int toYearWeek(LocalDate date) {
            int year = date.get(WeekFields.ISO.weekBasedYear());
            int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
            return year * 100 + week;
        }
    }

    private static class FakeStudentDao implements StudentDao {
        private final Map<Integer, Student> students = new HashMap<>();

        public void save(Student s) {
            students.put(s.id(), s);
        }

        @Override
        public int insert(Student student) {
            students.put(student.id(), student);
            return student.id();
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
            return students.values().stream().filter(s -> s.documentNumber().equals(documentNumber)).findFirst();
        }

        @Override
        public List<Student> search(String text) {
            return students.values().stream().toList();
        }

        @Override
        public List<Student> findByGrade(int gradeId, int year) {
            return List.of();
        }

        @Override
        public int updateStatus(int studentId, String status) {
            return 1;
        }
    }

    private static class FakeSubjectDao implements SubjectDao {
        private final Map<Integer, Subject> subjects = new HashMap<>();

        public void save(Subject s) {
            subjects.put(s.id(), s);
        }

        @Override
        public int insert(Subject subject) {
            subjects.put(subject.id(), subject);
            return subject.id();
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
    }

    private static class FakeSubjectEnrollmentDao implements SubjectEnrollmentDao {
        private final List<SubjectEnrollment> enrollments = new ArrayList<>();
        private int seq = 1;

        public void enroll(int studentId, int subjectId, int year, String period) {
            enrollments.add(new SubjectEnrollment(seq++, studentId, subjectId, year, period, "A", LocalDate.now(), "Activa"));
        }

        @Override
        public int insert(SubjectEnrollment enrollment) {
            enrollments.add(enrollment);
            return seq++;
        }

        @Override
        public void insertAll(List<SubjectEnrollment> list) {
            enrollments.addAll(list);
        }

        @Override
        public List<SubjectEnrollment> findByStudent(int studentId, int year, String period) {
            return enrollments.stream()
                    .filter(e -> e.studentId() == studentId && e.year() == year && e.period().equalsIgnoreCase(period))
                    .toList();
        }

        @Override
        public List<SubjectEnrollment> findByCourse(int subjectId, int year, String period, String section) {
            return enrollments.stream()
                    .filter(e -> e.subjectId() == subjectId && e.year() == year
                            && e.period().equalsIgnoreCase(period)
                            && (section == null || e.section().equalsIgnoreCase(section)))
                    .toList();
        }

        @Override
        public boolean exists(int studentId, int subjectId, int year, String period) {
            return enrollments.stream().anyMatch(e -> e.studentId() == studentId && e.subjectId() == subjectId
                    && e.year() == year && e.period().equalsIgnoreCase(period));
        }

        @Override
        public boolean existsInYear(int studentId, int subjectId, int year) {
            return enrollments.stream().anyMatch(e -> e.studentId() == studentId && e.subjectId() == subjectId && e.year() == year);
        }

        @Override
        public int updateStatus(int enrollmentId, String status) {
            return 1;
        }
    }

    private static class FakeAssignmentDao implements AssignmentDao {
        private final Set<String> teacherAssignments = new HashSet<>();

        public void assignTeacher(int teacherId, int subjectId, int year) {
            teacherAssignments.add(teacherId + "-" + subjectId + "-" + year);
        }

        @Override
        public int insert(TeachingAssignment assignment) {
            return 1;
        }

        @Override
        public Optional<TeachingAssignment> findById(int id) {
            return Optional.empty();
        }

        @Override
        public List<TeachingAssignment> findByTeacher(int teacherId, int year) {
            return List.of();
        }

        @Override
        public List<TeachingAssignment> findBySubject(int subjectId, int year) {
            return List.of();
        }

        @Override
        public boolean exists(int teacherId, int subjectId, int year, String section) {
            return false;
        }

        @Override
        public boolean teacherHasSubject(int teacherId, int subjectId, int year) {
            return teacherAssignments.contains(teacherId + "-" + subjectId + "-" + year);
        }

        @Override
        public boolean hasGrades(int assignmentId) {
            return false;
        }

        @Override
        public int delete(int assignmentId) {
            return 0;
        }

        @Override
        public int insertSchedule(ClassSchedule schedule) {
            return 0;
        }

        @Override
        public List<ClassSchedule> findSchedulesByAssignment(int assignmentId) {
            return List.of();
        }

        @Override
        public List<ClassSchedule> findSchedulesByTeacher(int teacherId, int year) {
            return List.of();
        }

        @Override
        public int deleteSchedule(int scheduleId) {
            return 0;
        }
    }
}
