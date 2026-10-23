package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ReportTable;

import java.time.LocalDate;

/** Consultas de solo lectura para los reportes y estadísticas. */
public interface ReportDao {

    ReportTable studentsByGrade(int year);

    ReportTable topAverages(int year, int limit);

    ReportTable clubParticipation();

    ReportTable eventParticipation();

    ReportTable incomeByConcept(int year);

    ReportTable incomeByMonth(int year);

    ReportTable resourceStatus();

    ReportTable overdueLoans(LocalDate today);
}
