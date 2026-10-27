package com.littlehorizons.erp.util;

import java.util.regex.Pattern;

/** Validaciones de formato reutilizables. Devuelven {@code false} ante {@code null}. */
public final class InputValidator {

    private static final Pattern DOCUMENT = Pattern.compile("^[A-Za-z0-9]{5,20}$");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{7,15}$");

    private InputValidator() {
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    public static boolean isValidDocument(String value) {
        return value != null && DOCUMENT.matcher(value).matches();
    }

    public static boolean isValidEmail(String value) {
        return value != null && EMAIL.matcher(value).matches();
    }

    public static boolean isValidPhone(String value) {
        return value != null && PHONE.matcher(value).matches();
    }

    /** Nota válida: entre 0.0 y 5.0 con un solo decimal. */
    public static boolean isValidGrade(double value) {
        if (value < 0.0 || value > 5.0) {
            return false;
        }
        return Math.abs(value * 10 - Math.round(value * 10)) < 1e-9;
    }
}
