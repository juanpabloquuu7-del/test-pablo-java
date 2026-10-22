package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.GuardianRef;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class GuardianLookupDaoImpl extends BaseJdbcDao implements GuardianLookupDao {

    private static final String SELECT = "SELECT a.id_acudiente, a.numero_documento, a.direccion, "
            + "CONCAT_WS(' ', a.primer_nombre, a.segundo_nombre, a.primer_apellido, a.segundo_apellido) AS nombre "
            + "FROM acudientes a ";

    @Override
    public Optional<GuardianRef> findById(int id) {
        return queryOne(SELECT + "WHERE a.id_acudiente = ?", this::map, id);
    }

    @Override
    public Optional<GuardianRef> findByDocument(String document) {
        return queryOne(SELECT + "WHERE a.numero_documento = ?", this::map, document);
    }

    @Override
    public Optional<GuardianRef> findPrincipalGuardian(int idStudent) {
        return queryOne(SELECT + "JOIN estudiante_acudiente ea ON ea.id_acudiente = a.id_acudiente "
                + "WHERE ea.id_estudiante = ? "
                + "ORDER BY ea.es_principal DESC, a.es_responsable_financiero DESC, a.id_acudiente LIMIT 1",
                this::map, idStudent);
    }

    @Override
    public List<GuardianRef> findByStudent(int idStudent) {
        return query(SELECT + "JOIN estudiante_acudiente ea ON ea.id_acudiente = a.id_acudiente "
                + "WHERE ea.id_estudiante = ? ORDER BY ea.es_principal DESC, a.id_acudiente", this::map, idStudent);
    }

    @Override
    public boolean isGuardianOf(int idGuardian, int idStudent) {
        return queryOne("SELECT 1 FROM estudiante_acudiente WHERE id_acudiente = ? AND id_estudiante = ?",
                rs -> rs.getInt(1), idGuardian, idStudent).isPresent();
    }

    private GuardianRef map(ResultSet rs) throws SQLException {
        return new GuardianRef(rs.getInt("id_acudiente"), rs.getString("numero_documento"),
                rs.getString("nombre"), rs.getString("direccion"));
    }
}
