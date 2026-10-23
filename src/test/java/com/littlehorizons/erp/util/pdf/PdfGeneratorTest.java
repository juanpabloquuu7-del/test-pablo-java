package com.littlehorizons.erp.util.pdf;

import com.littlehorizons.erp.model.GuardianRef;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfGeneratorTest {

    @Test
    void cufeIsSha256HexOfNumberNitDate() throws Exception {
        LocalDate d = LocalDate.of(2026, 3, 5);
        String cufe = PdfGenerator.computeCufe("FE-000123", "900123456-7", d);
        assertEquals(64, cufe.length());
        assertTrue(cufe.matches("[0-9a-f]{64}"));
        byte[] h = MessageDigest.getInstance("SHA-256")
                .digest("FE-000123900123456-72026-03-05".getBytes(StandardCharsets.UTF_8));
        assertEquals(HexFormat.of().formatHex(h), cufe);
    }

    @Test
    void fileName() {
        assertEquals("factura_000123.pdf", PdfGenerator.fileNameFor("FE-000123"));
    }

    @Test
    void generatesValidPdf(@TempDir Path dir) throws Exception {
        LocalDate d = LocalDate.of(2026, 3, 5);
        Invoice inv = new Invoice(1, "FE-000123", 1, d, null, null, null);
        Payment p = new Payment(1, 1, "Ana Perez", 10, "Mama Perez", 1, "PENSION", "Pensión",
                new BigDecimal("1190000.00"), d, PaymentMethod.TRANSFER, "RC-000001", 2026, "I", null);
        GuardianRef g = new GuardianRef(10, "900", "Mamá Pérez ñ", null);
        String cufe = PdfGenerator.computeCufe(inv.number(), "900123456-7", d);
        Path pdf = PdfGenerator.generateInvoice(inv, cufe, p, g, new BigDecimal("19"), dir);
        assertTrue(Files.exists(pdf));
        assertEquals("factura_000123.pdf", pdf.getFileName().toString());
        byte[] bytes = Files.readAllBytes(pdf);
        assertTrue(bytes.length > 0);
        assertEquals("%PDF", new String(bytes, 0, 4, StandardCharsets.ISO_8859_1));
    }
}
