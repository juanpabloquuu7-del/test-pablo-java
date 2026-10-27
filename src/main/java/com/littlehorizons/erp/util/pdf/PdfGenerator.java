package com.littlehorizons.erp.util.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.model.GuardianRef;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/** Genera la factura electrónica en PDF (PDFBox) con código QR (ZXing) y calcula el CUFE simulado. */
public final class PdfGenerator {

    private static final float MARGIN = 50;
    private static final PDFont REGULAR = PDType1Font.HELVETICA;
    private static final PDFont BOLD = PDType1Font.HELVETICA_BOLD;

    private PdfGenerator() {
    }

    /** CUFE simulado: SHA-256 en hexadecimal de número de factura + NIT + fecha de emisión. */
    public static String computeCufe(String invoiceNumber, String nit, java.time.LocalDate issueDate) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((invoiceNumber + nit + issueDate).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    /** Nombre del archivo a partir del número de factura: FE-000123 -> factura_000123.pdf. */
    public static String fileNameFor(String invoiceNumber) {
        return "factura_" + invoiceNumber.substring(invoiceNumber.indexOf('-') + 1) + ".pdf";
    }

    /**
     * Escribe el PDF en {@code directory} y devuelve su ruta. El valor pagado es el total; el IVA, si aplica,
     * se desglosa de él.
     */
    public static Path generateInvoice(Invoice invoice, String cufe, Payment payment, GuardianRef client,
                                       BigDecimal ivaPercent, Path directory) throws IOException {
        AppConfig config = AppConfig.getInstance();
        BigDecimal total = payment.amount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = total.divide(BigDecimal.ONE.add(ivaPercent.movePointLeft(2)), 2, RoundingMode.HALF_UP);
        BigDecimal iva = total.subtract(subtotal);
        String description = payment.conceptName() + (payment.period() != null
                ? " - Periodo " + payment.period() + " " + payment.academicYear() : "");

        Files.createDirectories(directory);
        Path target = directory.resolve(fileNameFor(invoice.number()));
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float width = page.getMediaBox().getWidth();
            try (PDPageContentStream out = new PDPageContentStream(doc, page)) {
                float y = page.getMediaBox().getHeight() - MARGIN;
                text(out, BOLD, 15, MARGIN, y, config.get("school.name", ""));
                text(out, REGULAR, 9, MARGIN, y -= 16, "NIT " + config.get("school.nit", ""));
                text(out, REGULAR, 9, MARGIN, y -= 12, config.get("school.address", "")
                        + " - Tel. " + config.get("school.phone", ""));
                text(out, REGULAR, 9, MARGIN, y -= 12, config.get("school.email", ""));
                text(out, REGULAR, 8, MARGIN, y -= 12, config.get("school.resolution", ""));

                text(out, BOLD, 13, width - MARGIN - 190, page.getMediaBox().getHeight() - MARGIN,
                        "FACTURA ELECTRONICA");
                text(out, BOLD, 12, width - MARGIN - 190, page.getMediaBox().getHeight() - MARGIN - 18,
                        "No. " + invoice.number());
                text(out, REGULAR, 9, width - MARGIN - 190, page.getMediaBox().getHeight() - MARGIN - 32,
                        "Fecha de emision: " + invoice.issueDate());
                text(out, REGULAR, 9, width - MARGIN - 190, page.getMediaBox().getHeight() - MARGIN - 44,
                        "Recibo: " + payment.receipt());

                y -= 30;
                line(out, MARGIN, y, width - MARGIN);
                text(out, BOLD, 10, MARGIN, y -= 16, "Cliente (acudiente responsable)");
                text(out, REGULAR, 10, MARGIN, y -= 14, "Nombre: " + client.fullName());
                text(out, REGULAR, 10, MARGIN, y -= 13, "Documento: " + client.document());
                text(out, REGULAR, 10, MARGIN, y -= 13, "Direccion: " + (client.address() == null ? "-" : client.address()));
                text(out, REGULAR, 10, MARGIN, y -= 13, "Estudiante: " + payment.studentName());
                text(out, REGULAR, 10, MARGIN, y -= 13, "Metodo de pago: " + payment.method().label());

                y -= 14;
                line(out, MARGIN, y, width - MARGIN);
                float colQty = MARGIN + 290;
                float colUnit = MARGIN + 340;
                float colSub = MARGIN + 430;
                text(out, BOLD, 9, MARGIN, y -= 14, "Descripcion");
                text(out, BOLD, 9, colQty, y, "Cant.");
                text(out, BOLD, 9, colUnit, y, "Valor unitario");
                text(out, BOLD, 9, colSub, y, "Subtotal");
                line(out, MARGIN, y -= 5, width - MARGIN);
                text(out, REGULAR, 9, MARGIN, y -= 14, truncate(description, 55));
                text(out, REGULAR, 9, colQty, y, "1");
                text(out, REGULAR, 9, colUnit, y, money(subtotal));
                text(out, REGULAR, 9, colSub, y, money(subtotal));
                line(out, MARGIN, y -= 8, width - MARGIN);

                text(out, REGULAR, 10, colUnit, y -= 16, "Subtotal:");
                text(out, REGULAR, 10, colSub, y, money(subtotal));
                text(out, REGULAR, 10, colUnit, y -= 14, "IVA (" + ivaPercent.stripTrailingZeros().toPlainString() + "%):");
                text(out, REGULAR, 10, colSub, y, money(iva));
                text(out, BOLD, 11, colUnit, y -= 16, "TOTAL:");
                text(out, BOLD, 11, colSub, y, money(total));

                float qrSize = 110;
                float qrTop = y - 40;
                drawQr(out, qrContent(invoice, cufe, payment, total), MARGIN, qrTop, qrSize);
                text(out, BOLD, 8, MARGIN + qrSize + 14, qrTop - 12, "CUFE (simulado):");
                text(out, REGULAR, 7, MARGIN + qrSize + 14, qrTop - 24, cufe.substring(0, 64));
                text(out, REGULAR, 7, MARGIN + qrSize + 14, qrTop - 33, cufe.substring(64));
                text(out, REGULAR, 7, MARGIN + qrSize + 14, qrTop - 52,
                        "Documento generado con fines academicos; no tiene validez ante la DIAN.");
            }
            doc.save(target.toFile());
        }
        return target;
    }

    private static String qrContent(Invoice invoice, String cufe, Payment payment, BigDecimal total) {
        AppConfig config = AppConfig.getInstance();
        return "Factura: " + invoice.number() + "\nNIT: " + config.get("school.nit", "")
                + "\nFecha: " + invoice.issueDate() + "\nTotal: " + total.toPlainString()
                + "\nRecibo: " + payment.receipt() + "\nCUFE: " + cufe;
    }

    private static void drawQr(PDPageContentStream out, String content, float x, float top, float size)
            throws IOException {
        BitMatrix matrix;
        try {
            matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.MARGIN, 0, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
        } catch (WriterException e) {
            throw new IOException("No se pudo generar el codigo QR", e);
        }
        float module = size / matrix.getWidth();
        out.setNonStrokingColor(0f, 0f, 0f);
        for (int row = 0; row < matrix.getHeight(); row++) {
            for (int col = 0; col < matrix.getWidth(); col++) {
                if (matrix.get(col, row)) {
                    out.addRect(x + col * module, top - (row + 1) * module, module, module);
                }
            }
        }
        out.fill();
    }

    private static void text(PDPageContentStream out, PDFont font, float size, float x, float y, String value)
            throws IOException {
        out.beginText();
        out.setFont(font, size);
        out.newLineAtOffset(x, y);
        out.showText(safe(value));
        out.endText();
    }

    private static void line(PDPageContentStream out, float x1, float y, float x2) throws IOException {
        out.moveTo(x1, y);
        out.lineTo(x2, y);
        out.stroke();
    }

    /** Helvetica estándar solo codifica Latin-1: cualquier otro carácter se reemplaza. */
    private static String safe(String value) {
        StringBuilder sb = new StringBuilder();
        for (char ch : (value == null ? "" : value).toCharArray()) {
            sb.append(ch >= 32 && ch <= 255 ? ch : '?');
        }
        return sb.toString();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }

    private static String money(BigDecimal value) {
        return "$ " + NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO")).format(value);
    }
}
