package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.DelinquentStudent;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentMethod;
import com.littlehorizons.erp.model.Scholarship;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class FinanceDaoImpl extends BaseJdbcDao implements FinanceDao {

    private static final String SELECT_PAYMENT = "SELECT p.*, c.codigo_concepto, c.nombre_concepto, "
            + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) AS estudiante, "
            + "CONCAT_WS(' ', a.primer_nombre, a.segundo_nombre, a.primer_apellido, a.segundo_apellido) AS acudiente "
            + "FROM pagos p JOIN conceptos_pago c ON c.id_concepto = p.id_concepto "
            + "JOIN estudiantes e ON e.id_estudiante = p.id_estudiante "
            + "LEFT JOIN acudientes a ON a.id_acudiente = p.id_acudiente ";

    private static final String SELECT_INVOICE = "SELECT * FROM facturas ";

    // ---------- Conceptos ----------

    @Override
    public List<PaymentConcept> findConcepts(boolean onlyActive) {
        return query("SELECT * FROM conceptos_pago " + (onlyActive ? "WHERE esta_activo = TRUE " : "")
                + "ORDER BY codigo_concepto", this::mapConcept);
    }

    @Override
    public Optional<PaymentConcept> findConceptById(int id) {
        return queryOne("SELECT * FROM conceptos_pago WHERE id_concepto = ?", this::mapConcept, id);
    }

    @Override
    public Optional<PaymentConcept> findConceptByCode(String code) {
        return queryOne("SELECT * FROM conceptos_pago WHERE codigo_concepto = ?", this::mapConcept, code);
    }

    @Override
    public int insertConcept(PaymentConcept c) {
        return insert("INSERT INTO conceptos_pago (codigo_concepto, nombre_concepto, descripcion, monto_base, "
                        + "es_recurrente, esta_activo) VALUES (?, ?, ?, ?, ?, TRUE)",
                c.code(), c.name(), c.description(), c.baseAmount(), c.recurring());
    }

    @Override
    public void updateConcept(PaymentConcept c) {
        update("UPDATE conceptos_pago SET nombre_concepto = ?, descripcion = ?, monto_base = ?, es_recurrente = ? "
                        + "WHERE id_concepto = ?",
                c.name(), c.description(), c.baseAmount(), c.recurring(), c.id());
    }

    @Override
    public void setConceptActive(int conceptId, boolean active) {
        update("UPDATE conceptos_pago SET esta_activo = ? WHERE id_concepto = ?", active, conceptId);
    }

    // ---------- Becas ----------

    @Override
    public Optional<Scholarship> findScholarship(int studentId, int year) {
        return queryOne("SELECT * FROM becas WHERE id_estudiante = ? AND año_academico = ?",
                rs -> new Scholarship(rs.getInt("id_beca"), rs.getInt("id_estudiante"), rs.getInt("año_academico"),
                        rs.getBigDecimal("porcentaje_descuento"), rs.getString("descripcion")),
                studentId, year);
    }

    @Override
    public void saveScholarship(int studentId, int year, BigDecimal percent, String description) {
        update("INSERT INTO becas (id_estudiante, año_academico, porcentaje_descuento, descripcion) "
                        + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                        + "porcentaje_descuento = VALUES(porcentaje_descuento), descripcion = VALUES(descripcion)",
                studentId, year, percent, description);
    }

    // ---------- Pagos y facturas ----------

    @Override
    public Invoice insertPaymentWithInvoice(Payment p, LocalDate issueDate) {
        return inTransaction(c -> {
            String receipt = String.format("REC-%06d", nextSequence(c, "numero_recibo", "pagos", "REC-"));
            String invoiceNumber = String.format("FE-%06d", nextSequence(c, "numero_factura", "facturas", "FE-"));
            int paymentId = insert(c, "INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, monto_pagado, "
                            + "fecha_pago, metodo_pago, numero_recibo, año_academico, periodo_academico, notas) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    p.studentId(), p.guardianId(), p.conceptId(), p.amount(), Date.valueOf(p.date()),
                    p.method().label(), receipt, p.academicYear(), p.period(), p.notes());
            int invoiceId = insert(c, "INSERT INTO facturas (numero_factura, id_pago, fecha_emision) VALUES (?, ?, ?)",
                    invoiceNumber, paymentId, Date.valueOf(issueDate));
            return new Invoice(invoiceId, invoiceNumber, paymentId, issueDate, null, null, null);
        });
    }

    /** Siguiente consecutivo de {@code column} (formato PREFIJO-000123); bloquea las filas leídas hasta el commit. */
    private static int nextSequence(Connection c, String column, String table, String prefix) throws SQLException {
        return query(c, "SELECT COALESCE(MAX(CAST(SUBSTRING(" + column + ", ?) AS UNSIGNED)), 0) + 1 FROM " + table
                        + " WHERE " + column + " LIKE ? FOR UPDATE",
                rs -> rs.getInt(1), prefix.length() + 1, prefix + "%").get(0);
    }

    @Override
    public void attachInvoiceFile(int invoiceId, String cufe, String fileName, String filePath) {
        update("UPDATE facturas SET cufe = ?, nombre_archivo_pdf = ?, ruta_archivo = ? WHERE id_factura = ?",
                cufe, fileName, filePath, invoiceId);
    }

    @Override
    public Optional<Payment> findPaymentById(int id) {
        return queryOne(SELECT_PAYMENT + "WHERE p.id_pago = ?", this::mapPayment, id);
    }

    @Override
    public List<Payment> findPaymentsByStudent(int studentId) {
        return query(SELECT_PAYMENT + "WHERE p.id_estudiante = ? ORDER BY p.fecha_pago DESC, p.id_pago DESC",
                this::mapPayment, studentId);
    }

    @Override
    public List<Payment> findPaymentsByGuardian(int guardianId) {
        return query(SELECT_PAYMENT + "WHERE p.id_estudiante IN "
                        + "(SELECT id_estudiante FROM estudiante_acudiente WHERE id_acudiente = ?) "
                        + "ORDER BY p.fecha_pago DESC, p.id_pago DESC",
                this::mapPayment, guardianId);
    }

    @Override
    public Optional<Invoice> findInvoiceByPayment(int paymentId) {
        return queryOne(SELECT_INVOICE + "WHERE id_pago = ?", this::mapInvoice, paymentId);
    }

    @Override
    public Optional<Invoice> findInvoiceByNumber(String number) {
        return queryOne(SELECT_INVOICE + "WHERE numero_factura = ?", this::mapInvoice, number);
    }

    // ---------- Morosos ----------

    @Override
    public List<DelinquentStudent> findDelinquent(String conceptCode, int year, LocalDate cutoff, LocalDate today) {
        return query("SELECT e.id_estudiante, "
                        + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) AS nombre, "
                        + "(SELECT CONCAT_WS(' ', a.primer_nombre, a.primer_apellido) FROM estudiante_acudiente ea "
                        + " JOIN acudientes a ON a.id_acudiente = ea.id_acudiente WHERE ea.id_estudiante = e.id_estudiante "
                        + " ORDER BY ea.es_principal DESC, a.id_acudiente LIMIT 1) AS acudiente, "
                        + "COALESCE(MAX(p.fecha_pago), m.fecha_matricula) AS ultimo "
                        + "FROM estudiantes e "
                        + "JOIN matriculas m ON m.id_estudiante = e.id_estudiante AND m.año_academico = ? "
                        + "AND m.estado_matricula = 'Activa' "
                        + "LEFT JOIN pagos p ON p.id_estudiante = e.id_estudiante AND p.id_concepto = "
                        + "(SELECT id_concepto FROM conceptos_pago WHERE codigo_concepto = ?) "
                        + "WHERE e.estado = 'Activo' "
                        + "GROUP BY e.id_estudiante, e.primer_nombre, e.segundo_nombre, e.primer_apellido, "
                        + "e.segundo_apellido, m.fecha_matricula "
                        + "HAVING ultimo < ? ORDER BY ultimo, e.id_estudiante",
                rs -> {
                    LocalDate last = rs.getDate("ultimo").toLocalDate();
                    return new DelinquentStudent(rs.getInt("id_estudiante"), rs.getString("nombre"),
                            rs.getString("acudiente"), last, ChronoUnit.DAYS.between(last, today));
                }, year, conceptCode, Date.valueOf(cutoff));
    }

    // ---------- Mapeo ----------

    private PaymentConcept mapConcept(ResultSet rs) throws SQLException {
        return new PaymentConcept(rs.getInt("id_concepto"), rs.getString("codigo_concepto"),
                rs.getString("nombre_concepto"), rs.getString("descripcion"), rs.getBigDecimal("monto_base"),
                rs.getBoolean("es_recurrente"), rs.getBoolean("esta_activo"));
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        int guardian = rs.getInt("id_acudiente");
        Integer guardianId = rs.wasNull() ? null : guardian;
        return new Payment(rs.getInt("id_pago"), rs.getInt("id_estudiante"), rs.getString("estudiante"),
                guardianId, rs.getString("acudiente"), rs.getInt("id_concepto"), rs.getString("codigo_concepto"),
                rs.getString("nombre_concepto"), rs.getBigDecimal("monto_pagado"),
                rs.getDate("fecha_pago").toLocalDate(), PaymentMethod.fromLabel(rs.getString("metodo_pago")),
                rs.getString("numero_recibo"), rs.getInt("año_academico"), rs.getString("periodo_academico"),
                rs.getString("notas"));
    }

    private Invoice mapInvoice(ResultSet rs) throws SQLException {
        return new Invoice(rs.getInt("id_factura"), rs.getString("numero_factura"), rs.getInt("id_pago"),
                rs.getDate("fecha_emision").toLocalDate(), rs.getString("cufe"),
                rs.getString("nombre_archivo_pdf"), rs.getString("ruta_archivo"));
    }
}
