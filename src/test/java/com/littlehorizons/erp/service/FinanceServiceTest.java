package com.littlehorizons.erp.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinanceServiceTest {

    private FakeFinance finance;
    private FakeStudents students;
    private FakeGuardians guardians;
    private FinanceService service;

    @BeforeEach
    void setUp() {
        finance = new FakeFinance();
        students = new FakeStudents();
        guardians = new FakeGuardians();
        service = new FinanceService(finance, students, guardians);
        students.map.put(1, new StudentRef(1, "111", "Ana Perez", true));
        guardians.map.put(10, new GuardianRef(10, "900", "Mama Perez", "Calle 1"));
        guardians.map.put(11, new GuardianRef(11, "901", "Otro Acudiente", "Calle 2"));
        guardians.links.add("10-1");
        guardians.principal.put(1, 10);
        finance.addConcept(new PaymentConcept(1, "PENSION", "Pension", null, new BigDecimal("1000.00"), true, true));
        finance.addConcept(new PaymentConcept(2, "UNIFORME", "Uniforme", null, new BigDecimal("500.00"), false, true));
        finance.addConcept(new PaymentConcept(3, "VIEJO", "Viejo", null, new BigDecimal("100.00"), true, false));
    }

    private void pay(String concept, String amount, LocalDate date, String period, int guardian) {
        service.registerPayment(1, guardian, concept, new BigDecimal(amount), date, PaymentMethod.CASH, period, null);
    }

    // ---- descuentos ----

    @Test
    void siblingDiscountApplied() {
        students.siblings = 1;
        PriceQuote q = service.quote(1, "PENSION");
        assertEquals(0, new BigDecimal("900.00").compareTo(q.suggested()));
    }

    @Test
    void appliesMaxOfSiblingAndScholarshipNotSum() {
        students.siblings = 1;
        finance.scholarship = new Scholarship(1, 1, 2026, new BigDecimal("25"), null);
        PriceQuote q = service.quote(1, "PENSION");
        assertEquals(0, new BigDecimal("750.00").compareTo(q.suggested())); // 25 %, no 35 %
    }

    @Test
    void siblingWinsWhenGreaterThanScholarship() {
        students.siblings = 2;
        finance.scholarship = new Scholarship(1, 1, 2026, new BigDecimal("5"), null);
        assertEquals(0, new BigDecimal("900.00").compareTo(service.quote(1, "PENSION").suggested()));
    }

    @Test
    void nonRecurringConceptHasNoDiscount() {
        students.siblings = 1;
        finance.scholarship = new Scholarship(1, 1, 2026, new BigDecimal("50"), null);
        assertEquals(0, new BigDecimal("500.00").compareTo(service.quote(1, "UNIFORME").suggested()));
    }

    // ---- validaciones ----

    @Test
    void rejectsZeroOrNegativeAmount() {
        assertThrows(BusinessException.class, () -> pay("PENSION", "0", LocalDate.now(), "I", 10));
        assertThrows(BusinessException.class, () -> pay("PENSION", "-5", LocalDate.now(), "I", 10));
    }

    @Test
    void rejectsFutureDate() {
        assertThrows(BusinessException.class, () -> pay("PENSION", "100", LocalDate.now().plusDays(1), "I", 10));
    }

    @Test
    void rejectsInactiveConcept() {
        assertThrows(BusinessException.class, () -> pay("VIEJO", "100", LocalDate.now(), "I", 10));
    }

    @Test
    void rejectsGuardianNotOfStudent() {
        assertThrows(BusinessException.class, () -> pay("PENSION", "100", LocalDate.now(), "I", 11));
    }

    @Test
    void rejectsInvalidPeriod() {
        assertThrows(BusinessException.class, () -> pay("PENSION", "100", LocalDate.now(), "V", 10));
    }

    @Test
    void rejectsUnknownStudent() {
        assertThrows(BusinessException.class, () -> service.quote(99, "PENSION"));
    }

    @Test
    void rejectsInvalidScholarshipPercent() {
        assertThrows(BusinessException.class, () -> service.grantScholarship(1, new BigDecimal("101"), null));
        assertThrows(BusinessException.class, () -> service.grantScholarship(1, new BigDecimal("-1"), null));
    }

    @Test
    void rejectsDuplicateConceptCode() {
        assertThrows(BusinessException.class,
                () -> service.createConcept("pension", "X", null, new BigDecimal("10"), true));
    }

    // ---- fakes en memoria ----

    static class FakeStudents implements StudentLookupDao {
        final Map<Integer, StudentRef> map = new HashMap<>();
        int siblings = 0;

        public Optional<StudentRef> findById(int id) { return Optional.ofNullable(map.get(id)); }
        public Optional<StudentRef> findByDocument(String d) { return Optional.empty(); }
        public int countSiblings(int id, int year) { return siblings; }
        public Optional<String> findGradeName(int id, int year) { return Optional.empty(); }
    }

    static class FakeGuardians implements GuardianLookupDao {
        final Map<Integer, GuardianRef> map = new HashMap<>();
        final Set<String> links = new HashSet<>();
        final Map<Integer, Integer> principal = new HashMap<>();

        public Optional<GuardianRef> findById(int id) { return Optional.ofNullable(map.get(id)); }
        public Optional<GuardianRef> findByDocument(String d) { return Optional.empty(); }
        public Optional<GuardianRef> findPrincipalGuardian(int s) {
            return Optional.ofNullable(principal.get(s)).map(map::get);
        }
        public List<GuardianRef> findByStudent(int s) { return List.of(); }
        public boolean isGuardianOf(int g, int s) { return links.contains(g + "-" + s); }
    }

    static class FakeFinance implements FinanceDao {
        final Map<Integer, PaymentConcept> concepts = new LinkedHashMap<>();
        Scholarship scholarship;

        void addConcept(PaymentConcept c) { concepts.put(c.id(), c); }

        public List<PaymentConcept> findConcepts(boolean onlyActive) {
            return concepts.values().stream().filter(c -> !onlyActive || c.active()).toList();
        }
        public Optional<PaymentConcept> findConceptById(int id) { return Optional.ofNullable(concepts.get(id)); }
        public Optional<PaymentConcept> findConceptByCode(String code) {
            return concepts.values().stream().filter(c -> c.code().equals(code)).findFirst();
        }
        public int insertConcept(PaymentConcept c) {
            int id = concepts.size() + 1;
            concepts.put(id, new PaymentConcept(id, c.code(), c.name(), c.description(), c.baseAmount(),
                    c.recurring(), true));
            return id;
        }
        public void updateConcept(PaymentConcept c) { concepts.put(c.id(), c); }
        public void setConceptActive(int id, boolean a) { }
        public Optional<Scholarship> findScholarship(int s, int y) { return Optional.ofNullable(scholarship); }
        public void saveScholarship(int s, int y, BigDecimal p, String d) {
            scholarship = new Scholarship(1, s, y, p, d);
        }
        public Invoice insertPaymentWithInvoice(Payment d, LocalDate i) {
            throw new IllegalStateException("la validacion debia rechazar antes de insertar");
        }
        public void attachInvoiceFile(int i, String c, String f, String p) { }
        public Optional<Payment> findPaymentById(int id) { return Optional.empty(); }
        public List<Payment> findPaymentsByStudent(int s) { return List.of(); }
        public List<Payment> findPaymentsByGuardian(int g) { return List.of(); }
        public Optional<Invoice> findInvoiceByPayment(int p) { return Optional.empty(); }
        public Optional<Invoice> findInvoiceByNumber(String n) { return Optional.empty(); }
        public List<DelinquentStudent> findDelinquent(String c, int y, LocalDate cut, LocalDate t) {
            return List.of();
        }
    }
}
