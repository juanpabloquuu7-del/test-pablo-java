package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.model.ReportTable;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportServiceTest {

    private final List<String> calls = new ArrayList<>();

    private final ReportDao fake = new ReportDao() {
        private ReportTable t(String call) {
            calls.add(call);
            return new ReportTable(call, new String[]{"a"}, List.of());
        }
        public ReportTable studentsByGrade(int y) { return t("grade:" + y); }
        public ReportTable topAverages(int y, int l) { return t("top:" + y + ":" + l); }
        public ReportTable clubParticipation() { return t("club"); }
        public ReportTable eventParticipation() { return t("event"); }
        public ReportTable incomeByConcept(int y) { return t("concept:" + y); }
        public ReportTable incomeByMonth(int y) { return t("month:" + y); }
        public ReportTable resourceStatus() { return t("resource"); }
        public ReportTable overdueLoans(LocalDate today) { return t("overdue:" + today); }
    };

    @Test
    void usesConfiguredAcademicYear() {
        ReportService s = new ReportService(fake);
        s.studentsByGrade();
        s.topAverages();
        s.incomeByConcept();
        s.incomeByMonth();
        s.overdueLoans();
        assertEquals(List.of("grade:2026", "top:2026:10", "concept:2026", "month:2026",
                "overdue:" + LocalDate.now()), calls);
    }
}
