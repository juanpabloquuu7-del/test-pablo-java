package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.StudentRef;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class StudentLookupDaoImpl extends BaseJdbcDao implements StudentLookupDao {

    private static final String SELECT = "SELECT id_estudiante, numero_documento, estado, "
            + "CONCAT_WS(' ', primer_nombre, segundo_nombre, primer_apellido, segundo_apellido) AS nombre "
            + "FROM estudiantes ";

    @Override
    public Optional<StudentRef> findById(int id) {
        return queryOne(SELECT + "WHERE id_estudiante = ?", this::map, id);
    }

    @Override
    public Optional<StudentRef> findByDocument(String document) {
        return queryOne(SELECT + "WHERE numero_documento = ?", this::map, document);
    }

    @Override
    public int countSiblings(int idStudent, int year) {
        return queryOne("SELECT COUNT(DISTINCT other.id_estudiante) FROM estudiante_acudiente own "
                        + "JOIN estudiante_acudiente other ON other.id_acudiente = own.id_acudiente "
                        + "AND other.id_estudiante <> own.id_estudiante "
                        + "JOIN matriculas m ON m.id_estudiante = other.id_estudiante "
                        + "AND m.año_academico = ? AND m.estado_matricula = 'Activa' "
                        + "WHERE own.id_estudiante = ?",
                rs -> rs.getInt(1), year, idStudent).orElse(0);
    }

    @Override
    public Optional<String> findGradeName(int idStudent, int year) {
        return queryOne("SELECT g.nombre_grado FROM matriculas m JOIN grados g ON g.id_grado = m.id_grado "
                        + "WHERE m.id_estudiante = ? AND m.año_academico = ?",
                rs -> rs.getString(1), idStudent, year);
    }

    private StudentRef map(ResultSet rs) throws SQLException {
        return new StudentRef(rs.getInt("id_estudiante"), rs.getString("numero_documento"),
                rs.getString("nombre"), "Activo".equals(rs.getString("estado")));
    }
}
