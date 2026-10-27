package com.littlehorizons.erp.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Singleton con la configuración de la aplicación. Lee {@code config.properties} del classpath,
 * lo sobrescribe con un {@code config.properties} de la carpeta de ejecución (si existe) y toma
 * la contraseña de la base de datos de la variable de entorno {@code LH_DB_PASSWORD}.
 */
public final class AppConfig {

    private static final AppConfig INSTANCE = new AppConfig();
    private static final String FILE_NAME = "config.properties";

    private final Properties props = new Properties();

    private AppConfig() {
        try (InputStream in = AppConfig.class.getResourceAsStream("/" + FILE_NAME)) {
            if (in != null) {
                load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
            Path local = Path.of(FILE_NAME);
            if (Files.isRegularFile(local)) {
                try (Reader reader = Files.newBufferedReader(local, StandardCharsets.UTF_8)) {
                    load(reader);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + FILE_NAME, e);
        }
        String envPassword = System.getenv("LH_DB_PASSWORD");
        if (envPassword != null) {
            props.setProperty("db.password", envPassword);
        }
    }

    private void load(Reader reader) throws IOException {
        props.load(reader);
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public String get(String key) {
        return props.getProperty(key);
    }

    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Año académico vigente: {@code business.academicYear}, o el año actual si vale 0. */
    public int getAcademicYear() {
        int year = getInt("business.academicYear", 0);
        return year > 0 ? year : java.time.Year.now().getValue();
    }
}
