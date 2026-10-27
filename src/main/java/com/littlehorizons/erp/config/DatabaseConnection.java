package com.littlehorizons.erp.config;

import com.littlehorizons.erp.util.CustomExceptions.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Singleton que entrega conexiones JDBC según {@code db.url}, {@code db.user} y {@code db.password}. */
public final class DatabaseConnection {

    private static final DatabaseConnection INSTANCE = new DatabaseConnection();

    private DatabaseConnection() {
    }

    public static DatabaseConnection getInstance() {
        return INSTANCE;
    }

    /** Abre una conexión nueva; quien la recibe debe cerrarla (try-with-resources). */
    public Connection getConnection() {
        AppConfig config = AppConfig.getInstance();
        try {
            return DriverManager.getConnection(
                    config.get("db.url"),
                    config.get("db.user"),
                    config.get("db.password", ""));
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo conectar con la base de datos.", e);
        }
    }
}
