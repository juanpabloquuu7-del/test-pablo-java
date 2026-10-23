package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.CustomExceptions.DataAccessException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class StudentDaoImpl extends BaseJdbcDao implements StudentDao {

    @Override
    public int insert(Student student) {
        try (Connection c = open()) {
            return insert(c, student);
        } catch (SQLException e) {
            throw new DataAccessException("Error insertando estudiante.", e);
        }
    }

    @Override
    public int update(Student student) {
        try (Connection c = open()) {
            return update(c, student);
        } catch (SQLException e) {
            throw new DataAccessException("Error actualizando estudiante.", e);
        }
    }

    @Override
    public Optional<Student> findById(int id) {
        if (id <= 0) {
            return Optional.empty();
        }
        return queryOne("SELECT * FROM estudiantes WHERE id_estudiante = ?", StudentDaoImpl::map, id);
    }

    @Override
    public Optional<Student> findByDocument(String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            return Optional.empty();
        }
        return queryOne("SELECT * FROM estudiantes WHERE numero_documento = ?", StudentDaoImpl::map, documentNumber.trim());
    }

    @Override
    public List<Student> search(String text) {
        String pattern = "%" + (text == null ? "" : text.trim()) + "%";
        String sql = "SELECT * FROM estudiantes WHERE " +
                "CONCAT_WS(' ', primer_nombre, segundo_nombre, primer_apellido, segundo_apellido) LIKE ? " +
                "OR CONCAT_WS(' ', primer_apellido, segundo_apellido, primer_nombre, segundo_nombre) LIKE ? " +
                "OR numero_documento LIKE ? " +
                "ORDER BY primer_apellido, primer_nombre LIMIT 50";
        return query(sql, StudentDaoImpl::map, pattern, pattern, pattern);
    }

    @Override
    public List<Student> findByGrade(int gradeId, int year) {
        if (gradeId <= 0 || year <= 0) {
            return List.of();
        }
        String sql = "SELECT e.* FROM estudiantes e " +
                "JOIN matriculas m ON e.id_estudiante = m.id_estudiante " +
                "WHERE m.id_grado = ? AND m.año_academico = ? AND m.estado_matricula = 'Activa' " +
                "ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre";
        return query(sql, StudentDaoImpl::map, gradeId, year);
    }

    @Override
    public int updateStatus(int studentId, String status) {
        if (studentId <= 0) {
            return 0;
        }
        return update("UPDATE estudiantes SET estado = ? WHERE id_estudiante = ?", status, studentId);
    }

    public static int insert(Connection c, Student s) throws SQLException {
        String sql = "INSERT INTO estudiantes (" +
                "tipo_documento, numero_documento, primer_nombre, segundo_nombre, " +
                "primer_apellido, segundo_apellido, fecha_nacimiento, genero, " +
                "direccion, telefono, correo_electronico, tipo_sangre, " +
                "alergias, condiciones_medicas, fecha_ingreso, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return insert(c, sql,
                s.documentType(),
                s.documentNumber(),
                s.firstName(),
                s.middleName(),
                s.lastName(),
                s.secondLastName(),
                s.birthDate() != null ? Date.valueOf(s.birthDate()) : null,
                s.gender(),
                s.address(),
                s.phone(),
                s.email(),
                s.bloodType(),
                s.allergies(),
                s.medicalConditions(),
                s.admissionDate() != null ? Date.valueOf(s.admissionDate()) : null,
                s.status() != null ? s.status() : "Activo"
        );
    }

    public static int update(Connection c, Student s) throws SQLException {
        String sql = "UPDATE estudiantes SET " +
                "tipo_documento = ?, numero_documento = ?, primer_nombre = ?, segundo_nombre = ?, " +
                "primer_apellido = ?, segundo_apellido = ?, fecha_nacimiento = ?, genero = ?, " +
                "direccion = ?, telefono = ?, correo_electronico = ?, tipo_sangre = ?, " +
                "alergias = ?, condiciones_medicas = ?, fecha_ingreso = ?, estado = ? " +
                "WHERE id_estudiante = ?";
        return update(c, sql,
                s.documentType(),
                s.documentNumber(),
                s.firstName(),
                s.middleName(),
                s.lastName(),
                s.secondLastName(),
                s.birthDate() != null ? Date.valueOf(s.birthDate()) : null,
                s.gender(),
                s.address(),
                s.phone(),
                s.email(),
                s.bloodType(),
                s.allergies(),
                s.medicalConditions(),
                s.admissionDate() != null ? Date.valueOf(s.admissionDate()) : null,
                s.status(),
                s.id()
        );
    }

    public static int updateStatus(Connection c, int studentId, String status) throws SQLException {
        return update(c, "UPDATE estudiantes SET estado = ? WHERE id_estudiante = ?", status, studentId);
    }

    public static Student map(ResultSet rs) throws SQLException {
        return new Student(
                rs.getInt("id_estudiante"),
                rs.getString("tipo_documento"),
                rs.getString("numero_documento"),
                rs.getString("primer_nombre"),
                rs.getString("segundo_nombre"),
                rs.getString("primer_apellido"),
                rs.getString("segundo_apellido"),
                rs.getObject("fecha_nacimiento", LocalDate.class),
                rs.getString("genero"),
                rs.getString("direccion"),
                rs.getString("telefono"),
                rs.getString("correo_electronico"),
                rs.getString("tipo_sangre"),
                rs.getString("alergias"),
                rs.getString("condiciones_medicas"),
                rs.getObject("fecha_ingreso", LocalDate.class),
                rs.getString("estado")
        );
    }
}
