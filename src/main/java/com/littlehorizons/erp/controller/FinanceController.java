package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PriceQuote;
import com.littlehorizons.erp.model.Scholarship;
import com.littlehorizons.erp.service.FinanceService;
import com.littlehorizons.erp.service.FinanceService.PaymentOutcome;
import com.littlehorizons.erp.view.FinanceView;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;

/** Módulo 5 del menú: conceptos, becas, pagos, facturación electrónica y morosos. */
public class FinanceController extends BaseController implements MenuModule {

    private final FinanceView financeView;
    private final FinanceService service;

    public FinanceController(FinanceView view, FinanceService service) {
        super(view);
        this.financeView = view;
        this.service = service;
    }

    @Override
    public int position() {
        return 5;
    }

    @Override
    public void run() {
        while (true) {
            int option = financeView.showFinanceMenu();
            if (option == 0) {
                return;
            }
            safely(() -> execute(option));
        }
    }

    private void execute(int option) {
        switch (option) {
            case 1 -> financeView.showConcepts(service.listConcepts(false));
            case 2 -> createConcept();
            case 3 -> editConcept();
            case 4 -> toggleConcept();
            case 5 -> grantScholarship();
            case 6 -> registerPayment();
            case 7 -> financeView.showPayments(service.historyByStudent(financeView.readStudentId()));
            case 8 -> financeView.showPayments(service.historyByGuardian(
                    financeView.readInt("Id del acudiente: ", 1, Integer.MAX_VALUE)));
            case 9 -> delinquentReport();
            case 10 -> regenerate();
            default -> financeView.showError("Opción no válida.");
        }
    }

    private void createConcept() {
        String code = financeView.readConceptCode();
        String name = financeView.readRequired("Nombre del concepto: ");
        String description = financeView.readOptional("Descripción");
        BigDecimal base = financeView.readMoney("Valor base (COP): ");
        boolean recurring = financeView.readYesNo("¿Es un pago recurrente (admite descuentos)?");
        PaymentConcept created = service.createConcept(code, name, description, base, recurring);
        financeView.showSuccess("Concepto " + created.code() + " creado.");
    }

    private void editConcept() {
        PaymentConcept current = service.getConcept(financeView.readConceptCode());
        financeView.showConcepts(java.util.List.of(current));
        financeView.showInfo("Escriba los nuevos datos; Enter conserva el valor actual en los opcionales.");
        String name = financeView.readOptional("Nombre [" + current.name() + "]");
        String description = financeView.readOptional("Descripción");
        BigDecimal base = financeView.readYesNo("¿Cambiar el valor base?")
                ? financeView.readMoney("Nuevo valor base (COP): ") : current.baseAmount();
        boolean recurring = financeView.readYesNo("¿Es recurrente? (actual: " + (current.recurring() ? "sí" : "no") + ")");
        PaymentConcept updated = service.updateConcept(current.code(), name != null ? name : current.name(),
                description != null ? description : current.description(), base, recurring);
        financeView.showSuccess("Concepto " + updated.code() + " actualizado.");
    }

    private void toggleConcept() {
        PaymentConcept current = service.getConcept(financeView.readConceptCode());
        boolean activate = !current.active();
        if (financeView.readYesNo((activate ? "¿Activar " : "¿Desactivar ") + current.code() + "?")) {
            PaymentConcept updated = service.setConceptActive(current.code(), activate);
            financeView.showSuccess("Concepto " + updated.code() + (updated.active() ? " activado." : " desactivado."));
        }
    }

    private void grantScholarship() {
        int studentId = financeView.readStudentId();
        BigDecimal percent = financeView.readPercent("Porcentaje de descuento (0 a 100): ");
        String description = financeView.readOptional("Descripción");
        Scholarship s = service.grantScholarship(studentId, percent, description);
        financeView.showSuccess("Beca del " + s.percent().stripTrailingZeros().toPlainString()
                + "% guardada para el estudiante " + s.studentId() + " en " + s.academicYear() + ".");
    }

    private void registerPayment() {
        int studentId = financeView.readStudentId();
        Integer guardianId = financeView.readOptionalGuardianId();
        String conceptCode = financeView.readConceptCode();
        PriceQuote quote = service.quote(studentId, conceptCode);
        financeView.showQuote(quote);
        BigDecimal amount = quote.suggested();
        if (!financeView.readYesNo("¿Cobrar el valor sugerido?")) {
            amount = financeView.readMoney("Monto a cobrar (COP): ");
        }
        LocalDate date = LocalDate.now();
        if (!financeView.readYesNo("¿El pago es de hoy (" + date + ")?")) {
            date = financeView.readDate("Fecha del pago");
        }
        var method = financeView.chooseMethod();
        String period = financeView.choosePeriod();
        String notes = financeView.readOptional("Notas");
        PaymentOutcome result = service.registerPayment(studentId, guardianId, conceptCode, amount, date, method,
                period, notes);
        financeView.showSuccess("Pago registrado. Recibo " + result.payment().receipt() + ", factura "
                + result.invoice().number() + ".");
        showPdf(result.pdf(), result.invoice().number());
    }

    private void showPdf(Path pdf, String invoiceNumber) {
        if (pdf != null) {
            financeView.showSuccess("Factura PDF guardada en: " + pdf);
        } else {
            financeView.showError("El pago quedó guardado, pero no se pudo crear el PDF. Use la opción 10 con "
                    + invoiceNumber + " para regenerarlo.");
        }
    }

    private void delinquentReport() {
        var late = service.delinquentStudents();
        financeView.showDelinquent(late);
    }

    private void regenerate() {
        String number = financeView.readInvoiceNumber();
        financeView.showSuccess("Factura PDF guardada en: " + service.regenerateInvoice(number));
    }
}
