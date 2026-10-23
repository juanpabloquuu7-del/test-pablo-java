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
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.InputValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AcademicService {

    public record SubjectGradeSummary(int subjectId, String subjectName, BigDecimal average, BigDecimal finalGrade) {
    }

    public record AbsenceAlertDetail(int studentId, String studentName, int yearWeek, int weeklyAbsenceDays, int totalYearAbsences) {
    }

    private static final Set<String> VALID_PERIODS = Set.of("I", "II", "III", "IV", "Final");
    private static final Set<String> REGULAR_PERIODS = Set.of("I", "II", "III", "IV");
    private static final Set<String> VALID_ATTENDANCE_STATUSES = Set.of("Presente", "Ausente", "Tardanza", "Excusa Médica");

    private final AcademicRecordDao academicRecordDao;
    private final AttendanceDao attendanceDao;
    private final StudentDao studentDao;
    private final SubjectDao subjectDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;
    private final AssignmentDao assignmentDao;

    public AcademicService(
            AcademicRecordDao academicRecordDao,
            AttendanceDao attendanceDao,
            StudentDao studentDao,
            SubjectDao subjectDao,
            SubjectEnrollmentDao subjectEnrollmentDao,
            AssignmentDao assignmentDao) {
        this.academicRecordDao = academicRecordDao;
        this.attendanceDao = attendanceDao;
        this.studentDao = studentDao;
        this.subjectDao = subjectDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
        this.assignmentDao = assignmentDao;
    }

    // ==========================================
    // NOTAS Y PROMEDIOS (Paso 5)
    // ==========================================

    public int registerGrade(AcademicRecord record) {
        validateGradeRecord(record);
        if (academicRecordDao.find(record.studentId(), record.subjectId(), record.year(), record.period()).isPresent()) {
            throw new BusinessException("Ya existe una calificación registrada para este estudiante, materia y período.");
        }
        return academicRecordDao.insert(record);
    }

    public int updateGrade(AcademicRecord record) {
        validateGradeRecord(record);
        return academicRecordDao.update(record);
    }

    public Optional<AcademicRecord> findGrade(int studentId, int subjectId, int year, String period) {
        return academicRecordDao.find(studentId, subjectId, year, period);
    }

    public List<AcademicRecord> getAcademicHistory(int studentId) {
        studentDao.findById(studentId).orElseThrow(() -> new BusinessException("Estudiante no encontrado."));
        return academicRecordDao.findByStudent(studentId);
    }

    public Optional<BigDecimal> getSubjectAverage(int studentId, int subjectId, int year) {
        studentDao.findById(studentId).orElseThrow(() -> new BusinessException("Estudiante no encontrado."));
        subjectDao.findById(subjectId).orElseThrow(() -> new BusinessException("Materia no encontrada."));
        if (year <= 0) {
            throw new BusinessException("El año académico debe ser mayor que cero.");
        }
        return academicRecordDao.findAverageByStudentAndSubject(studentId, subjectId, year);
    }

    public Optional<BigDecimal> getStudentPeriodAverage(int studentId, int year, String period) {
        studentDao.findById(studentId).orElseThrow(() -> new BusinessException("Estudiante no encontrado."));
        if (year <= 0) {
            throw new BusinessException("El año académico debe ser mayor que cero.");
        }
        if (period == null || !REGULAR_PERIODS.contains(period)) {
            throw new BusinessException("Los promedios por período solo aplican para períodos I a IV.");
        }
        return academicRecordDao.findAverageByStudentAndPeriod(studentId, year, period);
    }

    public Optional<BigDecimal> getCourseAverage(int subjectId, int year, String period) {
        subjectDao.findById(subjectId).orElseThrow(() -> new BusinessException("Materia no encontrada."));
        if (year <= 0) {
            throw new BusinessException("El año académico debe ser mayor que cero.");
        }
        if (period != null && !period.isBlank() && !REGULAR_PERIODS.contains(period)) {
            throw new BusinessException("Los promedios de curso solo aplican para períodos I a IV o año completo.");
        }
        return academicRecordDao.findCourseAverage(subjectId, year, period);
    }

    public List<AverageLine> getSubjectAveragesByStudent(int studentId, int year) {
        studentDao.findById(studentId).orElseThrow(() -> new BusinessException("Estudiante no encontrado."));
        if (year <= 0) {
            throw new BusinessException("El año académico debe ser mayor que cero.");
        }
        return academicRecordDao.findSubjectAveragesByStudent(studentId, year);
    }

    public List<SubjectGradeSummary> getSubjectSummariesByStudent(int studentId, int year) {
        studentDao.findById(studentId).orElseThrow(() -> new BusinessException("Estudiante no encontrado."));
        if (year <= 0) {
            throw new BusinessException("El año académico debe ser mayor que cero.");
        }
        List<AcademicRecord> records = academicRecordDao.findByStudentAndYear(studentId, year);
        Set<Integer> subjectIds = records.stream()
                .map(AcademicRecord::subjectId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<SubjectGradeSummary> summaries = new ArrayList<>();
        for (int subjectId : subjectIds) {
            Subject subject = subjectDao.findById(subjectId).orElse(null);
            String subjectName = subject != null ? subject.name() : "Materia #" + subjectId;
            BigDecimal average = academicRecordDao.findAverageByStudentAndSubject(studentId, subjectId, year).orElse(null);
            BigDecimal finalGrade = records.stream()
                    .filter(r -> r.subjectId() == subjectId && "Final".equalsIgnoreCase(r.period()))
                    .map(AcademicRecord::grade)
                    .findFirst()
                    .orElse(null);
            summaries.add(new SubjectGradeSummary(subjectId, subjectName, average, finalGrade));
        }
        return summaries;
    }

    public Optional<Student> findStudent(int studentId) {
        return studentDao.findById(studentId);
    }

    public Optional<Subject> findSubject(int subjectId) {
        return subjectDao.findById(subjectId);
    }

    public void validateGradePrerequisites(int studentId, int subjectId, int year, String period, int teacherId) {
        if (year <= 0) {
            throw new BusinessException("El año académico debe ser mayor que cero.");
        }
        if (period == null || !VALID_PERIODS.contains(period)) {
            throw new BusinessException("El período académico debe ser I, II, III, IV o Final.");
        }
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new BusinessException("Estudiante no encontrado."));
        if (!"Activo".equalsIgnoreCase(student.status())) {
            throw new BusinessException("El estudiante no se encuentra en estado Activo.");
        }
        subjectDao.findById(subjectId)
                .orElseThrow(() -> new BusinessException("Materia no encontrada."));

        if ("Final".equalsIgnoreCase(period)) {
            if (!subjectEnrollmentDao.existsInYear(studentId, subjectId, year)) {
                throw new BusinessException("Una nota Final exige que el estudiante esté inscrito en la materia en al menos un período del año.");
            }
        } else {
            if (!subjectEnrollmentDao.exists(studentId, subjectId, year, period)) {
                throw new BusinessException("El estudiante no está inscrito en la materia para el período " + period + ".");
            }
        }

        if (!assignmentDao.teacherHasSubject(teacherId, subjectId, year)) {
            throw new BusinessException("El profesor no tiene asignada la materia en el año académico.");
        }
    }

    private void validateGradeRecord(AcademicRecord record) {
        if (record == null) {
            throw new BusinessException("El registro de nota es obligatorio.");
        }
        if (record.grade() == null
                || record.grade().compareTo(BigDecimal.ZERO) < 0
                || record.grade().compareTo(new BigDecimal("5.0")) > 0
                || !InputValidator.isValidGrade(record.grade().doubleValue())) {
            throw new BusinessException("La nota debe estar entre 0.0 y 5.0 con un solo decimal.");
        }
        if (record.recordDate() != null && record.recordDate().isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha de registro de la nota no puede ser futura.");
        }
        validateGradePrerequisites(record.studentId(), record.subjectId(), record.year(), record.period(), record.teacherId());
    }

    // ==========================================
    // ASISTENCIA (Paso 6)
    // ==========================================

    public int registerAttendance(Attendance attendance) {
        validateAttendance(attendance);
        if (attendanceDao.exists(attendance.studentId(), attendance.subjectId(), attendance.date())) {
            throw new BusinessException("Ya existe un registro de asistencia para este estudiante, materia y fecha.");
        }
        return attendanceDao.insert(attendance);
    }

    public void registerCourseAttendance(List<Attendance> attendances) {
        if (attendances == null || attendances.isEmpty()) {
            throw new BusinessException("No se indicaron registros de asistencia.");
        }
        Set<String> uniqueKeys = new HashSet<>();
        for (Attendance a : attendances) {
            validateAttendance(a);
            String key = a.studentId() + "-" + a.subjectId() + "-" + a.date();
            if (!uniqueKeys.add(key)) {
                throw new BusinessException("Se encontraron registros duplicados de asistencia en la lista para el mismo estudiante y fecha.");
            }
            if (attendanceDao.exists(a.studentId(), a.subjectId(), a.date())) {
                throw new BusinessException("Ya existe un registro de asistencia para la fecha seleccionada en este curso.");
            }
        }
        attendanceDao.insertAll(attendances);
    }

    public boolean hasAttendance(int studentId, int subjectId, LocalDate date) {
        return attendanceDao.exists(studentId, subjectId, date);
    }

    public void validateCourseAttendancePrerequisites(int subjectId, int teacherId, int year, LocalDate date) {
        if (year <= 0) {
            throw new BusinessException("El año académico debe ser mayor que cero.");
        }
        if (date == null || date.isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha de asistencia no puede ser futura.");
        }
        if (date.getYear() != year) {
            throw new BusinessException("El año de la fecha de asistencia (" + date.getYear() + ") no coincide con el año académico (" + year + ").");
        }
        subjectDao.findById(subjectId).orElseThrow(() -> new BusinessException("Materia no encontrada."));
        if (!assignmentDao.teacherHasSubject(teacherId, subjectId, year)) {
            throw new BusinessException("El profesor no tiene asignada la materia en el año correspondiente.");
        }
    }

    public List<Student> getEnrolledStudents(int subjectId, int year, String period, String section) {
        subjectDao.findById(subjectId).orElseThrow(() -> new BusinessException("Materia no encontrada."));
        if (year <= 0) {
            throw new BusinessException("El año académico debe ser mayor que cero.");
        }
        if (period == null || !REGULAR_PERIODS.contains(period)) {
            throw new BusinessException("El período debe ser I, II, III o IV.");
        }
        List<SubjectEnrollment> enrollments = subjectEnrollmentDao.findByCourse(subjectId, year, period, section);
        List<Student> students = new ArrayList<>();
        for (SubjectEnrollment se : enrollments) {
            studentDao.findById(se.studentId()).ifPresent(students::add);
        }
        return students;
    }

    public List<AbsenceAlertDetail> getAbsenceAlerts(int threshold, int year) {
        if (threshold < 0) {
            throw new BusinessException("El umbral de inasistencias debe ser mayor o igual a cero.");
        }
        if (year <= 0) {
            throw new BusinessException("El año académico debe ser mayor que cero.");
        }
        List<AttendanceDao.WeeklyAbsenceAlert> alerts = attendanceDao.findStudentsAboveThreshold(threshold, year);
        List<AbsenceAlertDetail> details = new ArrayList<>();
        for (AttendanceDao.WeeklyAbsenceAlert alert : alerts) {
            Student student = studentDao.findById(alert.studentId()).orElse(null);
            String name = student != null ? student.fullName() : "Estudiante #" + alert.studentId();
            int total = attendanceDao.countTotalAbsences(alert.studentId(), year);
            details.add(new AbsenceAlertDetail(alert.studentId(), name, alert.yearWeek(), alert.absenceDays(), total));
        }
        return details;
    }

    public int getTotalAbsences(int studentId, int year) {
        studentDao.findById(studentId).orElseThrow(() -> new BusinessException("Estudiante no encontrado."));
        if (year <= 0) {
            throw new BusinessException("El año académico debe ser mayor que cero.");
        }
        return attendanceDao.countTotalAbsences(studentId, year);
    }

    private void validateAttendance(Attendance attendance) {
        if (attendance == null) {
            throw new BusinessException("El registro de asistencia es obligatorio.");
        }
        if (attendance.date() == null || attendance.date().isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha de asistencia no puede ser futura.");
        }
        if (attendance.status() == null || !VALID_ATTENDANCE_STATUSES.contains(attendance.status())) {
            throw new BusinessException("Estado de asistencia inválido. Debe ser: Presente, Ausente, Tardanza o Excusa Médica.");
        }
        studentDao.findById(attendance.studentId())
                .orElseThrow(() -> new BusinessException("Estudiante no encontrado."));
        subjectDao.findById(attendance.subjectId())
                .orElseThrow(() -> new BusinessException("Materia no encontrada."));

        int year = attendance.date().getYear();
        if (attendance.registeredBy() == null || !assignmentDao.teacherHasSubject(attendance.registeredBy(), attendance.subjectId(), year)) {
            throw new BusinessException("El profesor que registra la asistencia no tiene asignada la materia en el año correspondiente.");
        }
    }
}
