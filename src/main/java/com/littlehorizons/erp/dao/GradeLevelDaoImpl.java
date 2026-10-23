package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.GradeLevel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class GradeLevelDaoImpl extends BaseJdbcDao implements GradeLevelDao {

    @Override
    public List<GradeLevel> findAll() {
        return query(
                "SELECT id_grado, nombre_grado, nivel_numerico FROM grados ORDER BY nivel_numerico ASC",
                this::map
        );
    }

    @Override
    public Optional<GradeLevel> findById(int id) {
        if (id <= 0) {
            return Optional.empty();
        }
        return queryOne(
                "SELECT id_grado, nombre_grado, nivel_numerico FROM grados WHERE id_grado = ?",
                this::map,
                id
        );
    }

    private GradeLevel map(ResultSet rs) throws SQLException {
        return new GradeLevel(
                rs.getInt("id_grado"),
                rs.getString("nombre_grado"),
                rs.getInt("nivel_numerico")
        );
    }
}
