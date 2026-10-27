package com.littlehorizons.erp.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Base de todas las vistas de consola: solo entrada y salida. Toda lectura valida el dato y repite
 * la pregunta hasta que sea correcto. Las vistas de cada módulo extienden esta clase.
 */
public class BaseView {

    protected static final int WIDTH = 46;

    protected final Scanner scanner;

    public BaseView(Scanner scanner) {
        this.scanner = scanner;
    }

    // ---------- Salida ----------

    /** Marco de título: ╔═══╗ / ║ texto ║ / ╚═══╝ */
    public void banner(String... lines) {
        System.out.println();
        System.out.println("╔" + "═".repeat(WIDTH) + "╗");
        for (String line : lines) {
            System.out.println("║ " + pad(line, WIDTH - 2) + " ║");
        }
        System.out.println("╚" + "═".repeat(WIDTH) + "╝");
        System.out.println();
    }

    /** Muestra el submenú numerado y una opción final "0. Volver". */
    public void showMenu(String title, String... options) {
        banner(title);
        for (int i = 0; i < options.length; i++) {
            System.out.println((i + 1) + ". " + options[i]);
        }
        System.out.println("0. Volver");
        System.out.println();
    }

    public void showSuccess(String message) {
        System.out.println("✔ " + message);
    }

    public void showError(String message) {
        System.out.println("✘ " + message);
    }

    public void showInfo(String message) {
        System.out.println("ℹ " + message);
    }

    /** Tabla de texto con encabezado; cada fila debe tener tantas celdas como encabezados. */
    public void printTable(String[] headers, List<String[]> rows) {
        if (rows.isEmpty()) {
            showInfo("No hay registros para mostrar.");
            return;
        }
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < headers.length; i++) {
                widths[i] = Math.max(widths[i], cell(row[i]).length());
            }
        }
        String separator = separator(widths);
        System.out.println(separator);
        System.out.println(line(headers, widths));
        System.out.println(separator);
        for (String[] row : rows) {
            System.out.println(line(row, widths));
        }
        System.out.println(separator);
        System.out.println(rows.size() + " registro(s).");
    }

    public void pause() {
        System.out.print("Presione Enter para continuar...");
        scanner.nextLine();
    }

    // ---------- Entrada validada ----------

    /** Opción de menú entre 0 y {@code max}. */
    public int readOption(int max) {
        return readInt("Seleccione una opción: ", 0, max);
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String text = ask(prompt);
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
                showError("Ingrese un número entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) {
                showError("Ingrese un número entero válido.");
            }
        }
    }

    public double readDouble(String prompt, double min, double max) {
        while (true) {
            String text = ask(prompt).replace(',', '.');
            try {
                double value = Double.parseDouble(text);
                if (value >= min && value <= max) {
                    return value;
                }
                showError("Ingrese un valor entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) {
                showError("Ingrese un número válido.");
            }
        }
    }

    /** Monto en pesos, mayor que cero. */
    public BigDecimal readMoney(String prompt) {
        while (true) {
            String text = ask(prompt).replace(",", "");
            try {
                BigDecimal value = new BigDecimal(text);
                if (value.signum() > 0) {
                    return value;
                }
                showError("El valor debe ser mayor que cero.");
            } catch (NumberFormatException e) {
                showError("Ingrese un monto válido (solo números).");
            }
        }
    }

    /** Texto obligatorio (no vacío). */
    public String readRequired(String prompt) {
        while (true) {
            String text = ask(prompt);
            if (!text.isBlank()) {
                return text;
            }
            showError("Este campo es obligatorio.");
        }
    }

    /** Texto opcional: devuelve {@code null} si el usuario solo presiona Enter. */
    public String readOptional(String prompt) {
        String text = ask(prompt + " (Enter para omitir): ");
        return text.isBlank() ? null : text;
    }

    /** Fecha en formato AAAA-MM-DD. */
    public LocalDate readDate(String prompt) {
        while (true) {
            String text = ask(prompt + " (AAAA-MM-DD): ");
            try {
                return LocalDate.parse(text);
            } catch (DateTimeParseException e) {
                showError("Fecha inválida. Use el formato AAAA-MM-DD, por ejemplo 2026-03-15.");
            }
        }
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String text = ask(prompt + " (s/n): ").toLowerCase();
            if (text.equals("s") || text.equals("si") || text.equals("sí")) {
                return true;
            }
            if (text.equals("n") || text.equals("no")) {
                return false;
            }
            showError("Responda s o n.");
        }
    }

    /** Elige una opción de una lista numerada de 1..n; devuelve el índice base 0. */
    public int readChoice(String prompt, List<String> options) {
        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + options.get(i));
        }
        return readInt(prompt, 1, options.size()) - 1;
    }

    // ---------- Utilidades ----------

    protected String ask(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static String pad(String text, int width) {
        String value = text.length() > width ? text.substring(0, width) : text;
        return value + " ".repeat(width - value.length());
    }

    private static String cell(String value) {
        return value == null ? "" : value;
    }

    private static String separator(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w + 2)).append('+');
        }
        return sb.toString();
    }

    private static String line(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            sb.append(' ').append(pad(cell(cells[i]), widths[i])).append(" |");
        }
        return sb.toString();
    }
}
