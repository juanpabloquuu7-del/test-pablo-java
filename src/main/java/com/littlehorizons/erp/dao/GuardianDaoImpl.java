package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.GuardianLink;
import com.littlehorizons.erp.util.CustomExceptions.DataAccessException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class GuardianDaoImpl extends BaseJdbcDao implements GuardianDao {

    @Override
    public int insert(Guardian guardian) {
        try (Connection c = open()) {
            return insert(c, guardian);
        } catch (SQLException e) {
            throw new DataAccessException("Error insertando acudiente.", e);
        }
    }

    @Override
    public int update(Guardian guardian) {
        try (Connection c = open()) {
            return update(c, guardian);
        } catch (SQLException e) {
            throw new DataAccessException("Error actualizando acudiente.", e);
        }
    }

    @Override
    public Optional<Guardian> findById(int id) {
        if (id <= 0) {
            return Optional.empty();
        }
        return queryOne("SELECT * FROM acudientes WHERE id_acudiente = ?", GuardianDaoImpl::map, id);
    }

    @Override
    public Optional<Guardian> findByDocument(String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            return Optional.empty();
        }
        return queryOne("SELECT * FROM acudientes WHERE numero_documento = ?", GuardianDaoImpl::map, documentNumber.trim());
    }

    @Override
    public List<GuardianLink> findByStudent(int studentId) {
        if (studentId <= 0) {
            return List.of();
        }
        String sql = "SELECT a.*, ea.es_principal, ea.es_contacto_emergencia " +
                "FROM acudientes a " +
                "JOIN estudiante_acudiente ea ON a.id_acudiente = ea.id_acudiente " +
                "WHERE ea.id_estudiante = ? " +
                "ORDER BY ea.es_principal DESC, a.primer_apellido, a.primer_nombre";
        return query(sql, GuardianDaoImpl::mapLink, studentId);
    }

    @Override
    public void link(int studentId, int guardianId, boolean primary, boolean emergencyContact) {
        try (Connection c = open()) {
            link(c, studentId, guardianId, primary, emergencyContact);
        } catch (SQLException e) {
            throw new DataAccessException("Error vinculando acudiente.", e);
        }
    }

    @Override
    public void unlink(int studentId, int guardianId) {
        if (studentId <= 0 || guardianId <= 0) {
            return;
        }
        update("DELETE FROM estudiante_acudiente WHERE id_estudiante = ? AND id_acudiente = ?", studentId, guardianId);
    }

    @Override
    public void setPrimary(int studentId, int guardianId) {
        if (studentId <= 0 || guardianId <= 0) {
            return;
        }
        inTransaction(c -> {
            update(c, "UPDATE estudiante_acudiente SET es_principal = FALSE WHERE id_estudiante = ?", studentId);
            update(c, "UPDATE estudiante_acudiente SET es_principal = TRUE WHERE id_estudiante = ? AND id_acudiente = ?", studentId, guardianId);
            return null;
        });
    }

    @Override
    public int countStudents(int guardianId) {
        if (guardianId <= 0) {
            return 0;
        }
        List<Integer> list = query(
                "SELECT COUNT(*) AS c FROM estudiante_acudiente WHERE id_acudiente = ?",
                rs -> rs.getInt("c"),
                guardianId
        );
        return list.isEmpty() ? 0 : list.get(0);
    }

    public static int insert(Connection c, Guardian g) throws SQLException {
        String sql = "INSERT INTO acudientes (" +
                "tipo_documento, numero_documento, primer_nombre, segundo_nombre, " +
                "primer_apellido, segundo_apellido, parentesco, telefono_principal, " +
                "telefono_secundario, correo_electronico, direccion, ocupacion, es_responsable_financiero) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return insert(c, sql,
                g.documentType() != null ? g.documentType() : "CC",
                g.documentNumber(),
                g.firstName(),
                g.middleName(),
                g.lastName(),
                g.secondLastName(),
                g.relationship(),
                g.phone(),
                g.secondaryPhone(),
                g.email(),
                g.address(),
                g.occupation(),
                g.financialResponsible()
        );
    }

    public static int update(Connection c, Guardian g) throws SQLException {
        String sql = "UPDATE acudientes SET " +
                "tipo_documento = ?, numero_documento = ?, primer_nombre = ?, segundo_nombre = ?, " +
                "primer_apellido = ?, segundo_apellido = ?, parentesco = ?, telefono_principal = ?, " +
                "telefono_secundario = ?, correo_electronico = ?, direccion = ?, ocupacion = ?, " +
                "es_responsable_financiero = ? " +
                "WHERE id_acudiente = ?";
        return update(c, sql,
                g.documentType(),
                g.documentNumber(),
                g.firstName(),
                g.middleName(),
                g.lastName(),
                g.secondLastName(),
                g.relationship(),
                g.phone(),
                g.secondaryPhone(),
                g.email(),
                g.address(),
                g.occupation(),
                g.financialResponsible(),
                g.id()
        );
    }

    public static void link(Connection c, int studentId, int guardianId, boolean primary, boolean emergencyContact) throws SQLException {
        String sql = "INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal, es_contacto_emergencia) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE es_principal = VALUES(es_principal), es_contacto_emergencia = VALUES(es_contacto_emergencia)";
        update(c, sql, studentId, guardianId, primary, emergencyContact);
    }

    public static Guardian map(ResultSet rs) throws SQLException {
        return new Guardian(
                rs.getInt("id_acudiente"),
                rs.getString("tipo_documento"),
                rs.getString("numero_documento"),
                rs.getString("primer_nombre"),
                rs.getString("segundo_nombre"),
                rs.getString("primer_apellido"),
                rs.getString("segundo_apellido"),
                rs.getString("parentesco"),
                rs.getString("telefono_principal"),
                rs.getString("telefono_secundario"),
                rs.getString("correo_electronico"),
                rs.getString("direccion"),
                rs.getString("ocupacion"),
                rs.getBoolean("es_responsable_financiero")
        );
    }

    public static GuardianLink mapLink(ResultSet rs) throws SQLException {
        Guardian g = map(rs);
        return new GuardianLink(
                g,
                rs.getBoolean("es_principal"),
                rs.getBoolean("es_contacto_emergencia")
        );
    }
}
