package com.littlehorizons.erp.service;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.model.ReportTable;

import java.time.LocalDate;

/** Reportes y estadísticas: solo lectura, usando el año académico configurado. */
public class ReportService {

    private static final int TOP_LIMIT = 10;

    private final ReportDao reports;

    public ReportService(ReportDao reports) {
        this.reports = reports;
    }

    private static int year() {
        return AppConfig.getInstance().getAcademicYear();
    }

    public ReportTable studentsByGrade() {
        return reports.studentsByGrade(year());
    }

    public ReportTable topAverages() {
        return reports.topAverages(year(), TOP_LIMIT);
    }

    public ReportTable clubParticipation() {
        return reports.clubParticipation();
    }

    public ReportTable eventParticipation() {
        return reports.eventParticipation();
    }

    public ReportTable incomeByConcept() {
        return reports.incomeByConcept(year());
    }

    public ReportTable incomeByMonth() {
        return reports.incomeByMonth(year());
    }

    public ReportTable resourceStatus() {
        return reports.resourceStatus();
    }

    public ReportTable overdueLoans() {
        return reports.overdueLoans(LocalDate.now());
    }
}
