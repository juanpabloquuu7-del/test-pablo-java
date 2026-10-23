package com.littlehorizons.erp.service;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.FinanceDao;
import com.littlehorizons.erp.dao.GuardianLookupDao;
import com.littlehorizons.erp.dao.StudentLookupDao;
import com.littlehorizons.erp.model.DelinquentStudent;
import com.littlehorizons.erp.model.GuardianRef;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentMethod;
import com.littlehorizons.erp.model.PriceQuote;
import com.littlehorizons.erp.model.Scholarship;
import com.littlehorizons.erp.model.StudentRef;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.InputValidator;
import com.littlehorizons.erp.util.LoggerUtility;
import com.littlehorizons.erp.util.pdf.PdfGenerator;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Reglas de negocio de conceptos, descuentos, pagos, facturación electrónica y morosidad. */
public class FinanceService {

    /** Resultado de registrar un pago; {@code pdf} es null si la factura quedó pendiente de generar. */
    public record PaymentOutcome(Payment payment, Invoice invoice, Path pdf) {
    }

    public static final List<String> PERIODS = List.of("I", "II", "III", "IV", "Anual");

    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9_-]{2,20}$");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final FinanceDao finance;
    private final StudentLookupDao students;
    private final GuardianLookupDao guardians;

    public FinanceService(FinanceDao finance, StudentLookupDao students, GuardianLookupDao guardians) {
        this.finance = finance;
        this.students = students;
        this.guardians = guardians;
    }

    // ---------- Catálogo de conceptos ----------

    public List<PaymentConcept> listConcepts(boolean onlyActive) {
        return finance.findConcepts(onlyActive);
    }

    public PaymentConcept getConcept(String code) {
        return finance.findConceptByCode(code == null ? "" : code.trim().toUpperCase())
                .orElseThrow(() -> new BusinessException("No existe un concepto con el código " + code + "."));
    }

    public PaymentConcept createConcept(String code, String name, String description, BigDecimal baseAmount,
                                        boolean recurring) {
        String cleanCode = code == null ? "" : code.trim().toUpperCase();
        if (!CODE.matcher(cleanCode).matches()) {
            throw new BusinessException("El código debe tener entre 2 y 20 letras, números, guiones o guion bajo.");
        }
        if (finance.findConceptByCode(cleanCode).isPresent()) {
            throw new BusinessException("Ya existe un concepto con el código " + cleanCode + ".");
        }
        validateConcept(name, baseAmount);
        int id = finance.insertConcept(new PaymentConcept(0, cleanCode, name.trim(), blankToNull(description),
                baseAmount.setScale(2, RoundingMode.HALF_UP), recurring, true));
        return finance.findConceptById(id).orElseThrow(() -> new BusinessException("No se pudo leer el concepto creado."));
    }

    public PaymentConcept updateConcept(String code, String name, String description, BigDecimal baseAmount,
                                        boolean recurring) {
        PaymentConcept current = getConcept(code);
        validateConcept(name, baseAmount);
        finance.updateConcept(new PaymentConcept(current.id(), current.code(), name.trim(), blankToNull(description),
                baseAmount.setScale(2, RoundingMode.HALF_UP), recurring, current.active()));
        return getConcept(code);
    }

    public PaymentConcept setConceptActive(String code, boolean active) {
        PaymentConcept current = getConcept(code);
        if (current.active() == active) {
            throw new BusinessException("El concepto " + current.code() + " ya está " + (active ? "activo." : "inactivo."));
        }
        finance.setConceptActive(current.id(), active);
        return getConcept(code);
    }

    // ---------- Becas y valor personalizado ----------

    public Scholarship grantScholarship(int studentId, BigDecimal percent, String description) {
        StudentRef student = requireStudent(studentId);
        if (percent == null || percent.signum() < 0 || percent.compareTo(HUNDRED) > 0) {
            throw new BusinessException("El porcentaje de la beca debe estar entre 0 y 100.");
        }
        if (description != null && description.trim().length() > 255) {
            throw new BusinessException("La descripción admite hasta 255 caracteres.");
        }
        int year = AppConfig.getInstance().getAcademicYear();
        finance.saveScholarship(student.id(), year, percent.setScale(2, RoundingMode.HALF_UP), blankToNull(description));
        return finance.findScholarship(student.id(), year)
                .orElseThrow(() -> new BusinessException("No se pudo leer la beca guardada."));
    }

    /**
     * Valor sugerido para el estudiante. Solo los conceptos recurrentes tienen descuento; se aplica el mayor
     * entre el descuento por hermanos y la beca, nunca la suma.
     */
    public PriceQuote quote(int studentId, String conceptCode) {
        return quote(requireStudent(studentId), getConcept(conceptCode));
    }

    private PriceQuote quote(StudentRef student, PaymentConcept concept) {
        BigDecimal sibling = BigDecimal.ZERO;
        BigDecimal scholarship = BigDecimal.ZERO;
        if (concept.recurring()) {
            int year = AppConfig.getInstance().getAcademicYear();
            if (students.countSiblings(student.id(), year) > 0) {
                sibling = BigDecimal.valueOf(AppConfig.getInstance().getDouble("business.siblingDiscountPercent", 0));
            }
            scholarship = finance.findScholarship(student.id(), year).map(Scholarship::percent).orElse(BigDecimal.ZERO);
        }
        BigDecimal applied = sibling.max(scholarship);
        BigDecimal suggested = concept.baseAmount()
                .multiply(HUNDRED.subtract(applied)).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return new PriceQuote(concept.baseAmount(), sibling, scholarship, applied, suggested);
    }

    // ---------- Pagos y factura ----------

    /**
     * Registra el pago (con recibo y factura consecutivos en una transacción) y genera el PDF. Si el pago
     * queda guardado pero el PDF falla, {@code pdf} es null y se puede regenerar con {@link #regenerateInvoice}.
     */
    public PaymentOutcome registerPayment(int studentId, Integer guardianId, String conceptCode, BigDecimal amount,
                                          LocalDate date, PaymentMethod method, String period, String notes) {
        StudentRef student = requireStudent(studentId);
        if (!student.active()) {
            throw new BusinessException("El estudiante " + student.fullName() + " no está activo.");
        }
        PaymentConcept concept = getConcept(conceptCode);
        if (!concept.active()) {
            throw new BusinessException("El concepto " + concept.code() + " está inactivo.");
        }
        if (amount == null || amount.signum() <= 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new BusinessException("El monto debe ser mayor que cero y menor a 100.000.000.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha del pago no puede ser futura.");
        }
        if (!PERIODS.contains(period)) {
            throw new BusinessException("El período debe ser uno de: " + String.join(", ", PERIODS) + ".");
        }
        if (notes != null && notes.trim().length() > 1000) {
            throw new BusinessException("Las notas admiten hasta 1000 caracteres.");
        }
        GuardianRef payer = resolveGuardian(student, guardianId);
        Payment draft = new Payment(0, student.id(), student.fullName(), payer.id(), payer.fullName(), concept.id(),
                concept.code(), concept.name(), amount.setScale(2, RoundingMode.HALF_UP), date, method, null,
                AppConfig.getInstance().getAcademicYear(), period, blankToNull(notes));
        Invoice invoice = finance.insertPaymentWithInvoice(draft, LocalDate.now());
        Payment saved = finance.findPaymentById(invoice.paymentId())
                .orElseThrow(() -> new BusinessException("No se pudo leer el pago registrado."));
        try {
            return new PaymentOutcome(saved, invoice, writeInvoice(invoice, saved));
        } catch (IOException | RuntimeException e) {
            LoggerUtility.logError("FinanceService.registerPayment", e);
            return new PaymentOutcome(saved, invoice, null);
        }
    }

    /** Genera de nuevo el PDF de una factura (por ejemplo, si falló al registrar el pago). */
    public Path regenerateInvoice(String invoiceNumber) {
        Invoice invoice = finance.findInvoiceByNumber(invoiceNumber == null ? "" : invoiceNumber.trim().toUpperCase())
                .orElseThrow(() -> new BusinessException("No existe la factura " + invoiceNumber + "."));
        Payment payment = finance.findPaymentById(invoice.paymentId())
                .orElseThrow(() -> new BusinessException("La factura no tiene un pago asociado."));
        try {
            return writeInvoice(invoice, payment);
        } catch (IOException e) {
            LoggerUtility.logError("FinanceService.regenerateInvoice", e);
            throw new BusinessException("No se pudo escribir el PDF de la factura. Revise logs/error_log.txt.");
        }
    }

    private Path writeInvoice(Invoice invoice, Payment payment) throws IOException {
        AppConfig config = AppConfig.getInstance();
        GuardianRef client = payment.guardianId() != null
                ? guardians.findById(payment.guardianId()).orElse(null)
                : guardians.findPrincipalGuardian(payment.studentId()).orElse(null);
        if (client == null) {
            throw new IOException("El pago no tiene un acudiente para facturar.");
        }
        String cufe = PdfGenerator.computeCufe(invoice.number(), config.get("school.nit", ""), invoice.issueDate());
        BigDecimal iva = BigDecimal.valueOf(config.getDouble("business.ivaPercent", 0));
        Path dir = Path.of(config.get("paths.invoicesDir", "facturas")).toAbsolutePath();
        Path pdf = PdfGenerator.generateInvoice(invoice, cufe, payment, client, iva, dir);
        finance.attachInvoiceFile(invoice.id(), cufe, pdf.getFileName().toString(), pdf.toString());
        return pdf;
    }

    public List<Payment> historyByStudent(int studentId) {
        requireStudent(studentId);
        return finance.findPaymentsByStudent(studentId);
    }

    public List<Payment> historyByGuardian(int guardianId) {
        guardians.findById(guardianId)
                .orElseThrow(() -> new BusinessException("No existe el acudiente con id " + guardianId + "."));
        return finance.findPaymentsByGuardian(guardianId);
    }

    public Invoice findInvoice(int paymentId) {
        return finance.findInvoiceByPayment(paymentId).orElse(null);
    }

    // ---------- Morosidad ----------

    /** Estudiantes sin pago del concepto de pensión en los últimos {@code business.delinquencyDays} días. */
    public List<DelinquentStudent> delinquentStudents() {
        AppConfig config = AppConfig.getInstance();
        String concept = config.get("business.delinquencyConcept", "PENSION");
        int days = config.getInt("business.delinquencyDays", 30);
        LocalDate today = LocalDate.now();
        return finance.findDelinquent(concept, config.getAcademicYear(), today.minusDays(days), today);
    }

    // ---------- Validaciones ----------

    private StudentRef requireStudent(int studentId) {
        return students.findById(studentId)
                .orElseThrow(() -> new BusinessException("No existe el estudiante con id " + studentId + "."));
    }

    private GuardianRef resolveGuardian(StudentRef student, Integer guardianId) {
        if (guardianId == null) {
            return guardians.findPrincipalGuardian(student.id()).orElseThrow(() -> new BusinessException(
                    "El estudiante " + student.fullName() + " no tiene acudientes registrados."));
        }
        GuardianRef guardian = guardians.findById(guardianId)
                .orElseThrow(() -> new BusinessException("No existe el acudiente con id " + guardianId + "."));
        if (!guardians.isGuardianOf(guardian.id(), student.id())) {
            throw new BusinessException(guardian.fullName() + " no es acudiente de " + student.fullName() + ".");
        }
        return guardian;
    }

    private void validateConcept(String name, BigDecimal baseAmount) {
        if (!InputValidator.isNotBlank(name) || name.trim().length() > 100) {
            throw new BusinessException("El nombre es obligatorio y admite hasta 100 caracteres.");
        }
        if (baseAmount == null || baseAmount.signum() <= 0 || baseAmount.compareTo(MAX_AMOUNT) > 0) {
            throw new BusinessException("El valor base debe ser mayor que cero y menor a 100.000.000.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
