package com.littlehorizons.erp.util;

import com.littlehorizons.erp.config.AppConfig;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Registra errores en {@code logs/error_log.txt} (ruta en {@code paths.logFile}). Nunca lanza excepciones. */
public final class LoggerUtility {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LoggerUtility() {
    }

    public static synchronized void logError(String context, Throwable error) {
        try {
            Path file = Path.of(AppConfig.getInstance().get("paths.logFile", "logs/error_log.txt"));
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            StringWriter trace = new StringWriter();
            error.printStackTrace(new PrintWriter(trace));
            String entry = "[" + LocalDateTime.now().format(STAMP) + "] " + context + System.lineSeparator()
                    + "Mensaje: " + error.getMessage() + System.lineSeparator()
                    + trace + System.lineSeparator();
            Files.writeString(file, entry, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException e) {
            System.err.println("No se pudo escribir el log: " + e.getMessage());
        }
    }
}
