package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.util.CustomExceptions.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base de todos los DAO JDBC. Centraliza PreparedStatement, try-with-resources y la traducción de
 * {@link SQLException} a {@link DataAccessException}. Todo el SQL del proyecto vive en clases que extienden esta.
 */
public abstract class BaseJdbcDao {

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    public interface TransactionWork<T> {
        T run(Connection connection) throws SQLException;
    }

    protected final Connection open() {
        return DatabaseConnection.getInstance().getConnection();
    }

    /** SELECT que devuelve cero o más filas. */
    protected <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection c = open()) {
            return query(c, sql, mapper, params);
        } catch (SQLException e) {
            throw new DataAccessException("Error consultando la base de datos.", e);
        }
    }

    /** SELECT que devuelve a lo sumo una fila. */
    protected <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** INSERT, UPDATE o DELETE; devuelve las filas afectadas. */
    protected int update(String sql, Object... params) {
        try (Connection c = open()) {
            return update(c, sql, params);
        } catch (SQLException e) {
            throw new DataAccessException("Error escribiendo en la base de datos.", e);
        }
    }

    /** INSERT que devuelve la clave autogenerada. */
    protected int insert(String sql, Object... params) {
        try (Connection c = open()) {
            return insert(c, sql, params);
        } catch (SQLException e) {
            throw new DataAccessException("Error insertando en la base de datos.", e);
        }
    }

    /** Ejecuta varias operaciones en una transacción: commit si termina bien, rollback si falla. */
    protected <T> T inTransaction(TransactionWork<T> work) {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error en la transacción.", e);
        }
    }

    protected static <T> List<T> query(Connection c, String sql, RowMapper<T> mapper, Object... params)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
                return rows;
            }
        }
    }

    protected static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    protected static int insert(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
