package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentMethod;
import com.littlehorizons.erp.model.ReportTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ejecuta las consultas reales contra una BD MySQL: solo con -Dlh.it.db=nombre_bd. */
@EnabledIfSystemProperty(named = "lh.it.db", matches = ".+")
class SqlSmokeIT {

    @Test
    void allQueriesRun() throws Exception {
        Field f = AppConfig.class.getDeclaredField("props");
        f.setAccessible(true);
        Properties props = (Properties) f.get(AppConfig.getInstance());
        props.setProperty("db.url", "jdbc:mysql://localhost:3306/" + System.getProperty("lh.it.db")
                + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Bogota");
        props.setProperty("db.user", "root");
        props.setProperty("db.password", "");

        ReportDao r = new ReportDaoImpl();
        for (ReportTable t : new ReportTable[]{r.studentsByGrade(2026), r.topAverages(2026, 10), r.clubParticipation(),
                r.eventParticipation(), r.incomeByConcept(2026), r.incomeByMonth(2026), r.resourceStatus(),
                r.overdueLoans(LocalDate.now().plusYears(1))}) {
            assertNotNull(t);
            for (String[] row : t.rows()) {
                assertEquals(t.headers().length, row.length);
            }
            System.out.println(t.title() + ": " + t.rows().size() + " filas");
        }
        ReportTable top = r.topAverages(2026, 10);
        for (int i = 0; i < top.rows().size(); i++) {
            assertEquals(String.valueOf(i + 1), top.rows().get(i)[0]);
        }

        FinanceDao d = new FinanceDaoImpl();
        assertTrue(d.findConcepts(false).size() > 0);
        d.findConceptByCode("PENSION").orElseThrow();
        d.saveScholarship(1, 2026, new BigDecimal("10"), "x");
        d.saveScholarship(1, 2026, new BigDecimal("20"), "y");
        assertEquals(0, new BigDecimal("20").compareTo(d.findScholarship(1, 2026).orElseThrow().percent()));
        var delinquent = d.findDelinquent("PENSION", 2026, LocalDate.now().minusDays(30), LocalDate.now());
        System.out.println("morosos: " + delinquent.size() + " ids=" + delinquent.stream().map(x -> x.studentId()).toList());

        int concept = d.findConceptByCode("PENSION").orElseThrow().id();
        Payment p = new Payment(0, 1, null, null, null, concept, "PENSION", "x", new BigDecimal("100"),
                LocalDate.now(), PaymentMethod.CASH, null, 2026, "I", null);
        Invoice a = d.insertPaymentWithInvoice(p, LocalDate.now());
        Invoice b = d.insertPaymentWithInvoice(p, LocalDate.now());
        System.out.println(a.number() + " " + b.number());
        assertTrue(!a.number().equals(b.number()));
        d.attachInvoiceFile(a.id(), "c", "f.pdf", "/f.pdf");
        assertEquals("c", d.findInvoiceByNumber(a.number()).orElseThrow().cufe());
        assertTrue(d.findPaymentsByStudent(1).size() >= 2);
        d.findPaymentById(a.paymentId()).orElseThrow();
        d.findInvoiceByPayment(a.paymentId()).orElseThrow();
        d.findPaymentsByGuardian(1);
    }
}
