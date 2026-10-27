package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.DelinquentStudent;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Scholarship;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Acceso a conceptos de pago, becas, pagos y facturas. */
public interface FinanceDao {

    // ---------- Conceptos ----------

    List<PaymentConcept> findConcepts(boolean onlyActive);

    Optional<PaymentConcept> findConceptById(int id);

    Optional<PaymentConcept> findConceptByCode(String code);

    int insertConcept(PaymentConcept concept);

    void updateConcept(PaymentConcept concept);

    void setConceptActive(int conceptId, boolean active);

    // ---------- Becas ----------

    Optional<Scholarship> findScholarship(int studentId, int year);

    /** Crea la beca del estudiante en el año o reemplaza la existente. */
    void saveScholarship(int studentId, int year, BigDecimal percent, String description);

    // ---------- Pagos y facturas ----------

    /**
     * En una sola transacción asigna el siguiente recibo y la siguiente factura consecutivos, e inserta el
     * pago y la factura (sin CUFE ni archivo). Devuelve la factura creada.
     */
    Invoice insertPaymentWithInvoice(Payment draft, LocalDate issueDate);

    void attachInvoiceFile(int invoiceId, String cufe, String fileName, String filePath);

    Optional<Payment> findPaymentById(int id);

    List<Payment> findPaymentsByStudent(int studentId);

    /** Pagos de todos los estudiantes que tienen a este acudiente. */
    List<Payment> findPaymentsByGuardian(int guardianId);

    Optional<Invoice> findInvoiceByPayment(int paymentId);

    Optional<Invoice> findInvoiceByNumber(String number);

    // ---------- Morosos ----------

    /** Estudiantes con matrícula activa cuyo último pago del concepto (o su matrícula) es anterior a {@code cutoff}. */
    List<DelinquentStudent> findDelinquent(String conceptCode, int year, LocalDate cutoff, LocalDate today);
}
