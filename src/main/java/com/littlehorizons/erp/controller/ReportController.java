package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.service.ReportService;
import com.littlehorizons.erp.view.ReportView;

/** Módulo 7 del menú: reportes y estadísticas de solo lectura. */
public class ReportController extends BaseController implements MenuModule {

    private final ReportView reportView;
    private final ReportService service;

    public ReportController(ReportView view, ReportService service) {
        super(view);
        this.reportView = view;
        this.service = service;
    }

    @Override
    public int position() {
        return 7;
    }

    @Override
    public void run() {
        while (true) {
            int option = reportView.showReportMenu();
            if (option == 0) {
                return;
            }
            safely(() -> execute(option));
        }
    }

    private void execute(int option) {
        switch (option) {
            case 1 -> reportView.showReport(service.studentsByGrade());
            case 2 -> reportView.showReport(service.topAverages());
            case 3 -> reportView.showReport(service.clubParticipation());
            case 4 -> reportView.showReport(service.eventParticipation());
            case 5 -> reportView.showReport(service.incomeByConcept());
            case 6 -> reportView.showReport(service.incomeByMonth());
            case 7 -> reportView.showReport(service.resourceStatus());
            case 8 -> reportView.showReport(service.overdueLoans());
            default -> reportView.showError("Opción no válida.");
        }
    }
}
