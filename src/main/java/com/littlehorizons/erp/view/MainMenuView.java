package com.littlehorizons.erp.view;

import java.util.Scanner;
import java.util.Set;

/** Menú principal. Los módulos que aún no están registrados se marcan "(en desarrollo)". */
public class MainMenuView extends BaseView {

    public static final String[] TITLES = {
        "Gestión de Estudiantes",
        "Gestión Académica",
        "Gestión de Profesores",
        "Actividades Extracurriculares",
        "Gestión Financiera",
        "Gestión de Recursos",
        "Reportes y Estadísticas"
    };

    public static final int EXIT = TITLES.length + 1;

    public MainMenuView(Scanner scanner) {
        super(scanner);
    }

    public int showMainMenu(Set<Integer> availablePositions) {
        banner("ERP Colegio Little Horizons", "Sistema de Gestión Educativa");
        for (int i = 0; i < TITLES.length; i++) {
            String suffix = availablePositions.contains(i + 1) ? "" : "  (en desarrollo)";
            System.out.println((i + 1) + ". " + TITLES[i] + suffix);
        }
        System.out.println(EXIT + ". Salir");
        System.out.println();
        return readInt("Seleccione una opción: ", 1, EXIT);
    }

    public void showUnavailable() {
        showInfo("Este módulo aún no está disponible.");
    }

    public void showGoodbye() {
        showInfo("Cerrando el sistema. ¡Hasta pronto!");
    }
}
