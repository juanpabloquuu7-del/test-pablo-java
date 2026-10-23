package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.util.CustomExceptions.DataAccessException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class EnrollmentDaoImpl extends BaseJdbcDao implements EnrollmentDao {

    @Override
    public int insert(Enrollment enrollment) {
        try (Connection c = open()) {
            return insert(c, enrollment);
        } catch (SQLException e) {
            throw new DataAccessException("Error insertando matrícula.", e);
        }
    }

    @Override
    public Optional<Enrollment> findByStudentAndYear(int studentId, int year) {
        if (studentId <= 0 || year <= 0) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM matriculas WHERE id_estudiante = ? AND año_academico = ?";
        return queryOne(sql, EnrollmentDaoImpl::map, studentId, year);
    }

    @Override
    public List<Enrollment> findByStudent(int studentId) {
        if (studentId <= 0) {
            return List.of();
        }
        String sql = "SELECT * FROM matriculas WHERE id_estudiante = ? ORDER BY año_academico DESC, fecha_matricula DESC";
        return query(sql, EnrollmentDaoImpl::map, studentId);
    }

    @Override
    public int updateGrade(int enrollmentId, int gradeId) {
        if (enrollmentId <= 0 || gradeId <= 0) {
            return 0;
        }
        return update("UPDATE matriculas SET id_grado = ? WHERE id_matricula = ?", gradeId, enrollmentId);
    }

    @Override
    public int updateStatus(int enrollmentId, String status, LocalDate withdrawalDate) {
        if (enrollmentId <= 0) {
            return 0;
        }
        Date date = withdrawalDate != null ? Date.valueOf(withdrawalDate) : null;
        return update("UPDATE matriculas SET estado_matricula = ?, fecha_retiro = ? WHERE id_matricula = ?", status, date, enrollmentId);
    }

    public static int insert(Connection c, Enrollment e) throws SQLException {
        String sql = "INSERT INTO matriculas (" +
                "id_estudiante, id_grado, año_academico, estado_matricula, " +
                "fecha_matricula, fecha_retiro, observaciones) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        return insert(c, sql,
                e.studentId(),
                e.gradeId(),
                e.year(),
                e.status() != null ? e.status() : "Activa",
                e.enrollmentDate() != null ? Date.valueOf(e.enrollmentDate()) : null,
                e.withdrawalDate() != null ? Date.valueOf(e.withdrawalDate()) : null,
                e.notes()
        );
    }

    public static int updateGrade(Connection c, int enrollmentId, int gradeId) throws SQLException {
        return update(c, "UPDATE matriculas SET id_grado = ? WHERE id_matricula = ?", gradeId, enrollmentId);
    }

    public static int updateStatus(Connection c, int enrollmentId, String status, LocalDate withdrawalDate) throws SQLException {
        Date date = withdrawalDate != null ? Date.valueOf(withdrawalDate) : null;
        return update(c, "UPDATE matriculas SET estado_matricula = ?, fecha_retiro = ? WHERE id_matricula = ?", status, date, enrollmentId);
    }

    public static Enrollment map(ResultSet rs) throws SQLException {
        return new Enrollment(
                rs.getInt("id_matricula"),
                rs.getInt("id_estudiante"),
                rs.getInt("id_grado"),
                rs.getInt("año_academico"),
                rs.getString("estado_matricula"),
                rs.getObject("fecha_matricula", LocalDate.class),
                rs.getObject("fecha_retiro", LocalDate.class),
                rs.getString("observaciones")
        );
    }
}
