package com.littlehorizons.erp.service;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeachingAssignment;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.InputValidator;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TeacherService {

    public static final LocalTime MIN_SCHEDULE_TIME = LocalTime.of(6, 0);
    public static final LocalTime MAX_SCHEDULE_TIME = LocalTime.of(18, 0);

    public static final List<String> VALID_DAYS = List.of(
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"
    );

    private final TeacherDao teacherDao;
    private final AssignmentDao assignmentDao;
    private final SubjectDao subjectDao;
    private final int academicYear;

    public TeacherService(TeacherDao teacherDao, AssignmentDao assignmentDao, SubjectDao subjectDao) {
        this(teacherDao, assignmentDao, subjectDao, AppConfig.getInstance().getAcademicYear());
    }

    public TeacherService(TeacherDao teacherDao, AssignmentDao assignmentDao, SubjectDao subjectDao, int academicYear) {
        this.teacherDao = Objects.requireNonNull(teacherDao, "teacherDao no puede ser nulo");
        this.assignmentDao = Objects.requireNonNull(assignmentDao, "assignmentDao no puede ser nulo");
        this.subjectDao = Objects.requireNonNull(subjectDao, "subjectDao no puede ser nulo");
        this.academicYear = academicYear;
    }

    public static boolean overlaps(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        if (!start1.isBefore(end1) || !start2.isBefore(end2)) {
            return false;
        }
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    public static boolean hasScheduleConflict(List<ClassSchedule> existingSchedules, String day, LocalTime start, LocalTime end) {
        if (existingSchedules == null || day == null || start == null || end == null) {
            return false;
        }
        String normDay = normalizeDay(day);
        for (ClassSchedule existing : existingSchedules) {
            if (existing != null && existing.day() != null) {
                String existingNormDay = normalizeDay(existing.day());
                if (existingNormDay.equalsIgnoreCase(normDay) && overlaps(start, end, existing.startTime(), existing.endTime())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isValidDay(String day) {
        if (day == null || day.isBlank()) {
            return false;
        }
        String d = day.trim();
        for (String valid : VALID_DAYS) {
            if (valid.equalsIgnoreCase(d)) {
                return true;
            }
        }
        return "miercoles".equalsIgnoreCase(d) || "sabado".equalsIgnoreCase(d);
    }

    public static String normalizeDay(String day) {
        if (day == null) {
            return null;
        }
        String d = day.trim();
        if ("miercoles".equalsIgnoreCase(d) || "miércoles".equalsIgnoreCase(d)) {
            return "Miércoles";
        }
        if ("sabado".equalsIgnoreCase(d) || "sábado".equalsIgnoreCase(d)) {
            return "Sábado";
        }
        for (String valid : VALID_DAYS) {
            if (valid.equalsIgnoreCase(d)) {
                return valid;
            }
        }
        return d;
    }

    public static boolean isValidTimeWindow(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return false;
        }
        if (!start.isBefore(end)) {
            return false;
        }
        return !start.isBefore(MIN_SCHEDULE_TIME) && !end.isAfter(MAX_SCHEDULE_TIME);
    }

    public Teacher registerTeacher(Teacher teacher) {
        if (teacher == null) {
            throw new BusinessException("Los datos del profesor son obligatorios.");
        }
        if (!InputValidator.isNotBlank(teacher.firstName())) {
            throw new BusinessException("El primer nombre es obligatorio.");
        }
        if (!InputValidator.isNotBlank(teacher.lastName())) {
            throw new BusinessException("El primer apellido es obligatorio.");
        }
        String docNumber = teacher.documentNumber() != null ? teacher.documentNumber().trim() : null;
        if (!InputValidator.isValidDocument(docNumber)) {
            throw new BusinessException("El formato del número de documento no es válido.");
        }
        String email = teacher.email() != null ? teacher.email().trim() : null;
        if (!InputValidator.isValidEmail(email)) {
            throw new BusinessException("El formato del correo electrónico no es válido.");
        }
        String phone = teacher.phone() != null ? teacher.phone().trim() : null;
        if (phone != null && !phone.isBlank() && !InputValidator.isValidPhone(phone)) {
            throw new BusinessException("El formato del teléfono no es válido.");
        }
        if (teacher.documentType() == null || (!teacher.documentType().equals("CC") &&
                !teacher.documentType().equals("CE") && !teacher.documentType().equals("Pasaporte"))) {
            throw new BusinessException("Tipo de documento inválido. Debe ser CC, CE o Pasaporte.");
        }
        if (teacher.hireDate() == null) {
            throw new BusinessException("La fecha de contratación es obligatoria.");
        }
        if (teacherDao.findByDocument(docNumber).isPresent()) {
            throw new BusinessException("Ya existe un profesor con ese número de documento.");
        }
        if (teacherDao.findByEmail(email).isPresent()) {
            throw new BusinessException("Ya existe un profesor con ese correo electrónico.");
        }

        String status = (teacher.status() == null || teacher.status().isBlank()) ? "Activo" : teacher.status();
        if (!status.equals("Activo") && !status.equals("Inactivo") && !status.equals("Licencia")) {
            throw new BusinessException("Estado inválido. Debe ser Activo, Inactivo o Licencia.");
        }
        Teacher toInsert = new Teacher(
                0,
                teacher.documentType(),
                docNumber,
                teacher.firstName().trim(),
                InputValidator.isNotBlank(teacher.middleName()) ? teacher.middleName().trim() : null,
                teacher.lastName().trim(),
                InputValidator.isNotBlank(teacher.secondLastName()) ? teacher.secondLastName().trim() : null,
                email,
                InputValidator.isNotBlank(phone) ? phone : null,
                InputValidator.isNotBlank(teacher.specialization()) ? teacher.specialization().trim() : null,
                teacher.hireDate(),
                status
        );

        int id = teacherDao.insert(toInsert);
        return new Teacher(
                id,
                toInsert.documentType(),
                toInsert.documentNumber(),
                toInsert.firstName(),
                toInsert.middleName(),
                toInsert.lastName(),
                toInsert.secondLastName(),
                toInsert.email(),
                toInsert.phone(),
                toInsert.specialization(),
                toInsert.hireDate(),
                toInsert.status()
        );
    }

    public List<Teacher> listActiveTeachers() {
        return teacherDao.findAllActive();
    }

    public List<Teacher> listAllTeachers() {
        return teacherDao.findAll();
    }

    public Optional<Teacher> findTeacherById(int id) {
        if (id <= 0) {
            return Optional.empty();
        }
        return teacherDao.findById(id);
    }

    public Optional<TeachingAssignment> findAssignmentById(int id) {
        if (id <= 0) {
            return Optional.empty();
        }
        return assignmentDao.findById(id);
    }

    public void changeTeacherStatus(int teacherId, String newStatus) {
        if (newStatus == null || (!newStatus.equals("Activo") && !newStatus.equals("Inactivo") && !newStatus.equals("Licencia"))) {
            throw new BusinessException("Estado inválido. Debe ser Activo, Inactivo o Licencia.");
        }
        findTeacherById(teacherId)
                .orElseThrow(() -> new BusinessException("Profesor no encontrado."));
        teacherDao.updateStatus(teacherId, newStatus);
    }

    public TeachingAssignment assignSubject(int teacherId, int subjectId, String section) {
        return assignSubject(teacherId, subjectId, section, this.academicYear);
    }

    public TeachingAssignment assignSubject(int teacherId, int subjectId, String section, int year) {
        if (teacherId <= 0) {
            throw new BusinessException("Profesor no encontrado.");
        }
        if (subjectId <= 0) {
            throw new BusinessException("Materia no encontrada.");
        }
        if (year <= 0) {
            throw new BusinessException("El año académico debe ser mayor que cero.");
        }

        Teacher teacher = teacherDao.findById(teacherId)
                .orElseThrow(() -> new BusinessException("Profesor no encontrado."));

        if (!"Activo".equals(teacher.status())) {
            throw new BusinessException("Solo los profesores con estado Activo pueden recibir asignaciones.");
        }

        Subject subject = subjectDao.findById(subjectId)
                .orElseThrow(() -> new BusinessException("Materia no encontrada."));

        if (!subject.active()) {
            throw new BusinessException("La materia no está activa.");
        }

        String sec = (section == null || section.isBlank()) ? "A" : section.trim().toUpperCase();
        if (sec.length() > 10) {
            throw new BusinessException("La sección no puede superar los 10 caracteres.");
        }

        if (assignmentDao.exists(teacherId, subjectId, year, sec)) {
            throw new BusinessException("El profesor ya tiene asignada esta materia en la sección " + sec + " para el año " + year + ".");
        }

        TeachingAssignment assignment = new TeachingAssignment(0, teacherId, subjectId, year, sec);
        int id = assignmentDao.insert(assignment);
        return new TeachingAssignment(id, teacherId, subjectId, year, sec);
    }

    public void deleteAssignment(int assignmentId) {
        if (assignmentId <= 0) {
            throw new BusinessException("Asignación no encontrada.");
        }

        assignmentDao.findById(assignmentId)
                .orElseThrow(() -> new BusinessException("Asignación no encontrada."));

        if (assignmentDao.hasGrades(assignmentId)) {
            throw new BusinessException("No se puede eliminar la asignación porque ya tiene notas registradas.");
        }

        assignmentDao.delete(assignmentId);
    }

    public ClassSchedule registerSchedule(int assignmentId, String day, LocalTime startTime, LocalTime endTime) {
        if (assignmentId <= 0) {
            throw new BusinessException("Asignación no encontrada.");
        }

        TeachingAssignment assignment = assignmentDao.findById(assignmentId)
                .orElseThrow(() -> new BusinessException("Asignación no encontrada."));

        if (!isValidDay(day)) {
            throw new BusinessException("El día debe ser de Lunes a Sábado.");
        }

        String normalizedDay = normalizeDay(day);

        if (!isValidTimeWindow(startTime, endTime)) {
            throw new BusinessException("El horario debe ser de Lunes a Sábado entre las 06:00 y las 18:00, con hora de fin posterior a la de inicio.");
        }

        List<ClassSchedule> teacherSchedules = assignmentDao.findSchedulesByTeacher(assignment.teacherId(), assignment.year());
        if (hasScheduleConflict(teacherSchedules, normalizedDay, startTime, endTime)) {
            throw new BusinessException("Conflicto de horario: el profesor ya tiene una clase asignada que se traslapa con ese horario.");
        }

        ClassSchedule schedule = new ClassSchedule(0, assignmentId, normalizedDay, startTime, endTime);
        int id = assignmentDao.insertSchedule(schedule);
        return new ClassSchedule(id, assignmentId, normalizedDay, startTime, endTime);
    }

    public void deleteSchedule(int scheduleId) {
        if (scheduleId <= 0) {
            return;
        }
        assignmentDao.deleteSchedule(scheduleId);
    }

    public List<TeacherAssignmentDetail> getTeacherWorkload(int teacherId) {
        return getTeacherWorkload(teacherId, this.academicYear);
    }

    public List<TeacherAssignmentDetail> getTeacherWorkload(int teacherId, int year) {
        if (teacherId <= 0) {
            throw new BusinessException("Profesor no encontrado.");
        }
        if (year <= 0) {
            throw new BusinessException("El año académico debe ser mayor que cero.");
        }

        teacherDao.findById(teacherId)
                .orElseThrow(() -> new BusinessException("Profesor no encontrado."));

        List<TeachingAssignment> assignments = assignmentDao.findByTeacher(teacherId, year);
        List<TeacherAssignmentDetail> details = new ArrayList<>();
        if (assignments != null) {
            for (TeachingAssignment a : assignments) {
                if (a != null) {
                    Subject s = subjectDao.findById(a.subjectId()).orElse(null);
                    List<ClassSchedule> schedules = assignmentDao.findSchedulesByAssignment(a.id());
                    details.add(new TeacherAssignmentDetail(a, s, schedules != null ? schedules : List.of()));
                }
            }
        }
        return details;
    }

    public int getAcademicYear() {
        return academicYear;
    }

    public record TeacherAssignmentDetail(
            TeachingAssignment assignment,
            Subject subject,
            List<ClassSchedule> schedules
    ) {}
}
