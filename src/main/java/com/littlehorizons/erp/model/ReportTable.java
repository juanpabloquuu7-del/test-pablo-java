package com.littlehorizons.erp.model;

import java.util.List;

/** Resultado tabular de un reporte: título, encabezados y filas ya formateadas como texto. */
public record ReportTable(String title, String[] headers, List<String[]> rows) {
}
