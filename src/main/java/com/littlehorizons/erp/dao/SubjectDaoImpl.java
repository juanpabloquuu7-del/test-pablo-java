package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Subject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class SubjectDaoImpl extends BaseJdbcDao implements SubjectDao {

    @Override
    public int insert(Subject subject) {
        String sql = "INSERT INTO materias (codigo_materia, nombre_materia, descripcion, id_grado, horas_semanales, esta_activa) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        return insert(sql,
                subject.code(),
                subject.name(),
                subject.description(),
                subject.gradeId(),
                subject.weeklyHours(),
                subject.active());
    }

    @Override
    public Optional<Subject> findById(int id) {
        if (id <= 0) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM materias WHERE id_materia = ?";
        return queryOne(sql, this::map, id);
    }

    @Override
    public List<Subject> findByGrade(int gradeId) {
        if (gradeId <= 0) {
            return List.of();
        }
        String sql = "SELECT * FROM materias WHERE id_grado = ? ORDER BY nombre_materia";
        return query(sql, this::map, gradeId);
    }

    @Override
    public List<Subject> findActive() {
        String sql = "SELECT * FROM materias WHERE esta_activa = TRUE ORDER BY id_grado, nombre_materia";
        return query(sql, this::map);
    }

    private Subject map(ResultSet rs) throws SQLException {
        return new Subject(
                rs.getInt("id_materia"),
                rs.getString("codigo_materia"),
                rs.getString("nombre_materia"),
                rs.getString("descripcion"),
                rs.getInt("id_grado"),
                rs.getInt("horas_semanales"),
                rs.getBoolean("esta_activa")
        );
    }
}
