package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.TeacherRef;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class TeacherLookupDaoImpl extends BaseJdbcDao implements TeacherLookupDao {

    private static final String SELECT = "SELECT id_profesor, "
            + "CONCAT_WS(' ', primer_nombre, segundo_nombre, primer_apellido, segundo_apellido) AS nombre "
            + "FROM profesores ";

    @Override
    public Optional<TeacherRef> findById(int id) {
        return queryOne(SELECT + "WHERE id_profesor = ?", this::map, id);
    }

    @Override
    public Optional<TeacherRef> findActiveById(int id) {
        return queryOne(SELECT + "WHERE id_profesor = ? AND estado = 'Activo'", this::map, id);
    }

    private TeacherRef map(ResultSet rs) throws SQLException {
        return new TeacherRef(rs.getInt("id_profesor"), rs.getString("nombre"));
    }
}
