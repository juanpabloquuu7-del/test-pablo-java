package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.TeachingAssignment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public class AssignmentDaoImpl extends BaseJdbcDao implements AssignmentDao {

    @Override
    public int insert(TeachingAssignment assignment) {
        String sql = "INSERT INTO asignaciones_profesor (id_profesor, id_materia, año_academico, seccion) VALUES (?, ?, ?, ?)";
        return insert(sql,
                assignment.teacherId(),
                assignment.subjectId(),
                assignment.year(),
                assignment.section());
    }

    @Override
    public Optional<TeachingAssignment> findById(int id) {
        String sql = "SELECT * FROM asignaciones_profesor WHERE id_asignacion = ?";
        return queryOne(sql, this::mapAssignment, id);
    }

    @Override
    public List<TeachingAssignment> findByTeacher(int teacherId, int year) {
        String sql = "SELECT * FROM asignaciones_profesor WHERE id_profesor = ? AND año_academico = ? ORDER BY id_materia, seccion";
        return query(sql, this::mapAssignment, teacherId, year);
    }

    @Override
    public List<TeachingAssignment> findBySubject(int subjectId, int year) {
        String sql = "SELECT * FROM asignaciones_profesor WHERE id_materia = ? AND año_academico = ? ORDER BY seccion, id_profesor";
        return query(sql, this::mapAssignment, subjectId, year);
    }

    @Override
    public boolean exists(int teacherId, int subjectId, int year, String section) {
        String sql = "SELECT 1 FROM asignaciones_profesor WHERE id_profesor = ? AND id_materia = ? AND año_academico = ? AND seccion = ? LIMIT 1";
        return queryOne(sql, rs -> true, teacherId, subjectId, year, section).orElse(false);
    }

    @Override
    public boolean teacherHasSubject(int teacherId, int subjectId, int year) {
        String sql = "SELECT 1 FROM asignaciones_profesor WHERE id_profesor = ? AND id_materia = ? AND año_academico = ? LIMIT 1";
        return queryOne(sql, rs -> true, teacherId, subjectId, year).orElse(false);
    }

    @Override
    public boolean hasGrades(int assignmentId) {
        String sql = "SELECT 1 FROM registros_academicos r " +
                "JOIN asignaciones_profesor a ON r.id_profesor = a.id_profesor " +
                "AND r.id_materia = a.id_materia AND r.año_academico = a.año_academico " +
                "WHERE a.id_asignacion = ? LIMIT 1";
        return queryOne(sql, rs -> true, assignmentId).orElse(false);
    }

    @Override
    public int delete(int assignmentId) {
        String sql = "DELETE FROM asignaciones_profesor WHERE id_asignacion = ?";
        return update(sql, assignmentId);
    }

    @Override
    public int insertSchedule(ClassSchedule schedule) {
        String sql = "INSERT INTO horarios_clase (id_asignacion, dia_semana, hora_inicio, hora_fin) VALUES (?, ?, ?, ?)";
        return insert(sql,
                schedule.assignmentId(),
                schedule.day(),
                schedule.startTime(),
                schedule.endTime());
    }

    @Override
    public List<ClassSchedule> findSchedulesByAssignment(int assignmentId) {
        String sql = "SELECT * FROM horarios_clase WHERE id_asignacion = ? " +
                "ORDER BY FIELD(dia_semana, 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'), hora_inicio";
        return query(sql, this::mapSchedule, assignmentId);
    }

    @Override
    public List<ClassSchedule> findSchedulesByTeacher(int teacherId, int year) {
        String sql = "SELECT h.* FROM horarios_clase h " +
                "JOIN asignaciones_profesor a ON h.id_asignacion = a.id_asignacion " +
                "WHERE a.id_profesor = ? AND a.año_academico = ? " +
                "ORDER BY FIELD(h.dia_semana, 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'), h.hora_inicio";
        return query(sql, this::mapSchedule, teacherId, year);
    }

    @Override
    public int deleteSchedule(int scheduleId) {
        String sql = "DELETE FROM horarios_clase WHERE id_horario = ?";
        return update(sql, scheduleId);
    }

    private TeachingAssignment mapAssignment(ResultSet rs) throws SQLException {
        return new TeachingAssignment(
                rs.getInt("id_asignacion"),
                rs.getInt("id_profesor"),
                rs.getInt("id_materia"),
                rs.getInt("año_academico"),
                rs.getString("seccion")
        );
    }

    private ClassSchedule mapSchedule(ResultSet rs) throws SQLException {
        LocalTime start = null;
        try {
            start = rs.getObject("hora_inicio", LocalTime.class);
        } catch (Exception ignored) {
        }
        if (start == null) {
            java.sql.Time t = rs.getTime("hora_inicio");
            if (t != null) {
                start = t.toLocalTime();
            }
        }

        LocalTime end = null;
        try {
            end = rs.getObject("hora_fin", LocalTime.class);
        } catch (Exception ignored) {
        }
        if (end == null) {
            java.sql.Time t = rs.getTime("hora_fin");
            if (t != null) {
                end = t.toLocalTime();
            }
        }

        return new ClassSchedule(
                rs.getInt("id_horario"),
                rs.getInt("id_asignacion"),
                rs.getString("dia_semana"),
                start,
                end
        );
    }
}
