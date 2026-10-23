package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.ReportTable;

import java.util.Scanner;

/** Entrada y salida de consola del módulo de reportes. */
public class ReportView extends BaseView {

    public ReportView(Scanner scanner) {
        super(scanner);
    }

    public int showReportMenu() {
        showMenu("Reportes y Estadísticas",
                "Estudiantes por grado",
                "Mejores promedios",
                "Participación en clubes",
                "Participación en eventos",
                "Ingresos por concepto",
                "Ingresos por mes",
                "Estado de los recursos",
                "Préstamos vencidos");
        return readOption(8);
    }

    public void showReport(ReportTable report) {
        banner(report.title());
        printTable(report.headers(), report.rows());
    }
}
