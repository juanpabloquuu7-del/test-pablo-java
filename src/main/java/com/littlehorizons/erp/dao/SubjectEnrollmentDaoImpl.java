package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SubjectEnrollment;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class SubjectEnrollmentDaoImpl extends BaseJdbcDao implements SubjectEnrollmentDao {

    @Override
    public int insert(SubjectEnrollment enrollment) {
        String sql = "INSERT INTO inscripciones_materia (id_estudiante, id_materia, año_academico, periodo_academico, seccion, fecha_inscripcion, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        return insert(sql,
                enrollment.studentId(),
                enrollment.subjectId(),
                enrollment.year(),
                enrollment.period(),
                enrollment.section(),
                enrollment.enrollmentDate() != null ? Date.valueOf(enrollment.enrollmentDate()) : null,
                enrollment.status());
    }

    @Override
    public void insertAll(List<SubjectEnrollment> enrollments) {
        if (enrollments == null || enrollments.isEmpty()) {
            return;
        }
        inTransaction(connection -> {
            String sql = "INSERT INTO inscripciones_materia (id_estudiante, id_materia, año_academico, periodo_academico, seccion, fecha_inscripcion, estado) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            for (SubjectEnrollment enrollment : enrollments) {
                insert(connection, sql,
                        enrollment.studentId(),
                        enrollment.subjectId(),
                        enrollment.year(),
                        enrollment.period(),
                        enrollment.section(),
                        enrollment.enrollmentDate() != null ? Date.valueOf(enrollment.enrollmentDate()) : null,
                        enrollment.status());
            }
            return null;
        });
    }

    @Override
    public List<SubjectEnrollment> findByStudent(int studentId, int year, String period) {
        if (studentId <= 0 || year <= 0) {
            return List.of();
        }
        String sql = "SELECT * FROM inscripciones_materia WHERE id_estudiante = ? AND año_academico = ? AND periodo_academico = ? ORDER BY id_materia";
        return query(sql, this::map, studentId, year, period);
    }

    @Override
    public List<SubjectEnrollment> findByCourse(int subjectId, int year, String period, String section) {
        if (subjectId <= 0 || year <= 0) {
            return List.of();
        }
        if (section == null || section.isBlank()) {
            String sql = "SELECT * FROM inscripciones_materia WHERE id_materia = ? AND año_academico = ? AND periodo_academico = ? AND estado = 'Activa' ORDER BY id_estudiante";
            return query(sql, this::map, subjectId, year, period);
        }
        String sql = "SELECT * FROM inscripciones_materia WHERE id_materia = ? AND año_academico = ? AND periodo_academico = ? AND seccion = ? AND estado = 'Activa' ORDER BY id_estudiante";
        return query(sql, this::map, subjectId, year, period, section);
    }

    @Override
    public boolean exists(int studentId, int subjectId, int year, String period) {
        if (studentId <= 0 || subjectId <= 0 || year <= 0) {
            return false;
        }
        String sql = "SELECT 1 FROM inscripciones_materia WHERE id_estudiante = ? AND id_materia = ? AND año_academico = ? AND periodo_academico = ? LIMIT 1";
        return queryOne(sql, rs -> true, studentId, subjectId, year, period).orElse(false);
    }

    @Override
    public boolean existsInYear(int studentId, int subjectId, int year) {
        if (studentId <= 0 || subjectId <= 0 || year <= 0) {
            return false;
        }
        String sql = "SELECT 1 FROM inscripciones_materia WHERE id_estudiante = ? AND id_materia = ? AND año_academico = ? AND periodo_academico IN ('I', 'II', 'III', 'IV') LIMIT 1";
        return queryOne(sql, rs -> true, studentId, subjectId, year).orElse(false);
    }

    @Override
    public int updateStatus(int enrollmentId, String status) {
        if (enrollmentId <= 0) {
            return 0;
        }
        String sql = "UPDATE inscripciones_materia SET estado = ? WHERE id_inscripcion = ?";
        return update(sql, status, enrollmentId);
    }

    private SubjectEnrollment map(ResultSet rs) throws SQLException {
        Date date = rs.getDate("fecha_inscripcion");
        LocalDate enrollmentDate = date != null ? date.toLocalDate() : null;
        return new SubjectEnrollment(
                rs.getInt("id_inscripcion"),
                rs.getInt("id_estudiante"),
                rs.getInt("id_materia"),
                rs.getInt("año_academico"),
                rs.getString("periodo_academico"),
                rs.getString("seccion"),
                enrollmentDate,
                rs.getString("estado")
        );
    }
}
