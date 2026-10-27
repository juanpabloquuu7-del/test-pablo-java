package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.DelinquentStudent;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentMethod;
import com.littlehorizons.erp.model.PriceQuote;
import com.littlehorizons.erp.service.FinanceService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/** Entrada y salida de consola del módulo financiero. */
public class FinanceView extends BaseView {

    public FinanceView(Scanner scanner) {
        super(scanner);
    }

    public int showFinanceMenu() {
        showMenu("Gestión Financiera",
                "Listar conceptos de pago",
                "Crear concepto",
                "Editar concepto",
                "Activar o desactivar concepto",
                "Asignar beca a un estudiante",
                "Registrar pago (genera factura PDF)",
                "Historial de pagos por estudiante",
                "Historial de pagos por acudiente",
                "Reporte de morosos",
                "Regenerar PDF de una factura");
        return readOption(10);
    }

    public String readConceptCode() {
        return readRequired("Código del concepto: ");
    }

    public String readInvoiceNumber() {
        return readRequired("Número de factura (ej. FE-000123): ");
    }

    public int readStudentId() {
        return readInt("Id del estudiante: ", 1, Integer.MAX_VALUE);
    }

    public BigDecimal readPercent(String prompt) {
        return BigDecimal.valueOf(readDouble(prompt, 0, 100));
    }

    public PaymentMethod chooseMethod() {
        PaymentMethod[] methods = PaymentMethod.values();
        List<String> labels = new ArrayList<>();
        for (PaymentMethod method : methods) {
            labels.add(method.label());
        }
        return methods[readChoice("Método de pago: ", labels)];
    }

    public String choosePeriod() {
        return FinanceService.PERIODS.get(readChoice("Período: ", FinanceService.PERIODS));
    }

    /** Id del acudiente que paga, o null si se omite (se usa el acudiente principal). */
    public Integer readOptionalGuardianId() {
        while (true) {
            String text = readOptional("Id del acudiente que paga (vacío = principal)");
            if (text == null) {
                return null;
            }
            try {
                int id = Integer.parseInt(text);
                if (id > 0) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
                // se repite la pregunta
            }
            showError("Ingrese un id de acudiente válido.");
        }
    }

    public void showConcepts(List<PaymentConcept> concepts) {
        List<String[]> rows = new ArrayList<>();
        for (PaymentConcept c : concepts) {
            rows.add(new String[]{c.code(), c.name(), money(c.baseAmount()), c.recurring() ? "Sí" : "No",
                    c.active() ? "Activo" : "Inactivo"});
        }
        printTable(new String[]{"Código", "Concepto", "Valor base", "Recurrente", "Estado"}, rows);
    }

    public void showQuote(PriceQuote q) {
        showInfo("Valor base: " + money(q.base()));
        if (q.appliedPercent().signum() > 0) {
            showInfo("Descuento por hermanos: " + q.siblingPercent().stripTrailingZeros().toPlainString()
                    + "% | Beca: " + q.scholarshipPercent().stripTrailingZeros().toPlainString()
                    + "% | Se aplica el mayor: " + q.appliedPercent().stripTrailingZeros().toPlainString() + "%");
        }
        showInfo("Valor sugerido: " + money(q.suggested()));
    }

    public void showPayments(List<Payment> payments) {
        List<String[]> rows = new ArrayList<>();
        for (Payment p : payments) {
            rows.add(new String[]{p.receipt(), p.date().toString(), p.studentName(), p.conceptName(),
                    p.period() == null ? "" : p.period(), p.method().label(), money(p.amount())});
        }
        printTable(new String[]{"Recibo", "Fecha", "Estudiante", "Concepto", "Per.", "Método", "Monto"}, rows);
        BigDecimal total = BigDecimal.ZERO;
        for (Payment p : payments) {
            total = total.add(p.amount());
        }
        if (!payments.isEmpty()) {
            showInfo("Total pagado: " + money(total));
        }
    }

    public void showDelinquent(List<DelinquentStudent> students) {
        List<String[]> rows = new ArrayList<>();
        for (DelinquentStudent s : students) {
            rows.add(new String[]{String.valueOf(s.studentId()), s.studentName(),
                    s.guardianName() == null ? "" : s.guardianName(), s.lastPaymentOrEnrollment().toString(),
                    String.valueOf(s.daysPending())});
        }
        printTable(new String[]{"Id", "Estudiante", "Acudiente", "Último pago/matrícula", "Días"}, rows);
    }

    public static String money(BigDecimal value) {
        return "$ " + NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO")).format(value);
    }
}
