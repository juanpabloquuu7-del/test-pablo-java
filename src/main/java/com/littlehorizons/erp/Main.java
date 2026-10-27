package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.MainController;
import com.littlehorizons.erp.controller.MenuModule;
import com.littlehorizons.erp.util.LoggerUtility;
import com.littlehorizons.erp.view.MainMenuView;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Punto de entrada: arma los módulos de cada área y lanza el menú principal. No se edita por módulo. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        useUtf8Console();
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

        List<MenuModule> modules = new ArrayList<>();
        modules.addAll(AreaAModules.create(scanner));
        modules.addAll(AreaBModules.create(scanner));

        MainController app = new MainController(new MainMenuView(scanner), modules);
        try {
            app.run();
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Cerrando el sistema.");
        } catch (RuntimeException e) {
            LoggerUtility.logError("Main", e);
            System.out.println("Error fatal. Revise logs/error_log.txt.");
        }
    }

    /** La consola de Windows usa una página de códigos antigua; 65001 (UTF-8) muestra bien tildes y marcos. */
    private static void useUtf8Console() {
        // Sin consola interactiva (entrada por pipe o redirigida) el proceso hijo heredaría stdin y se la consumiría.
        if (!System.getProperty("os.name", "").toLowerCase().contains("win") || System.console() == null) {
            return;
        }
        try {
            new ProcessBuilder("cmd", "/c", "chcp 65001 >nul").inheritIO().start().waitFor();
        } catch (IOException e) {
            LoggerUtility.logError("Main.useUtf8Console", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
