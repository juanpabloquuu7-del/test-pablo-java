package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ReportTable;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

public class ReportDaoImpl extends BaseJdbcDao implements ReportDao {

    private static final String STUDENT_NAME =
            "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido)";

    @Override
    public ReportTable studentsByGrade(int year) {
        return table("Estudiantes por grado (" + year + ")", new String[]{"Grado", "Estudiantes activos"},
                "SELECT g.nombre_grado, COUNT(m.id_matricula) FROM grados g "
                        + "LEFT JOIN matriculas m ON m.id_grado = g.id_grado AND m.año_academico = ? "
                        + "AND m.estado_matricula = 'Activa' GROUP BY g.id_grado, g.nombre_grado, g.nivel_numerico "
                        + "ORDER BY g.nivel_numerico", year);
    }

    @Override
    public ReportTable topAverages(int year, int limit) {
        // El puesto se numera en Java (ver table): evita variables de sesión de MySQL.
        ReportTable raw = table("Mejores promedios (" + year + ")",
                new String[]{"Estudiante", "Promedio", "Notas"},
                "SELECT " + STUDENT_NAME + " AS nombre, ROUND(AVG(r.valor_nota), 2) AS promedio, COUNT(*) AS notas "
                        + "FROM registros_academicos r JOIN estudiantes e ON e.id_estudiante = r.id_estudiante "
                        + "WHERE r.año_academico = ? GROUP BY e.id_estudiante, e.primer_nombre, e.segundo_nombre, "
                        + "e.primer_apellido, e.segundo_apellido ORDER BY promedio DESC, nombre LIMIT ?",
                year, limit);
        List<String[]> ranked = new java.util.ArrayList<>();
        for (int i = 0; i < raw.rows().size(); i++) {
            String[] row = raw.rows().get(i);
            ranked.add(new String[]{String.valueOf(i + 1), row[0], row[1], row[2]});
        }
        return new ReportTable(raw.title(), new String[]{"Puesto", "Estudiante", "Promedio", "Notas"}, ranked);
    }

    @Override
    public ReportTable clubParticipation() {
        return table("Participación en clubes",
                new String[]{"Club", "Cupo", "Activos", "Retirados", "Ocupación %"},
                "SELECT c.nombre_club, c.capacidad_maxima, "
                        + "COALESCE(SUM(m.estado_membresia = 'Activa'), 0), "
                        + "COALESCE(SUM(m.estado_membresia = 'Retirada'), 0), "
                        + "ROUND(100 * COALESCE(SUM(m.estado_membresia = 'Activa'), 0) / NULLIF(c.capacidad_maxima, 0), 1) "
                        + "FROM clubes c LEFT JOIN membresias_club m ON m.id_club = c.id_club "
                        + "GROUP BY c.id_club, c.nombre_club, c.capacidad_maxima ORDER BY c.nombre_club");
    }

    @Override
    public ReportTable eventParticipation() {
        return table("Participación en eventos",
                new String[]{"Evento", "Fecha", "Tipo", "Participantes", "Podio"},
                "SELECT ev.nombre_evento, ev.fecha_evento, ev.tipo_evento, COUNT(p.id_participacion), "
                        + "COALESCE(SUM(p.posicion_obtenida IN ('1er Lugar', '2do Lugar', '3er Lugar')), 0) "
                        + "FROM eventos ev LEFT JOIN participantes_evento p ON p.id_evento = ev.id_evento "
                        + "GROUP BY ev.id_evento, ev.nombre_evento, ev.fecha_evento, ev.tipo_evento "
                        + "ORDER BY ev.fecha_evento");
    }

    @Override
    public ReportTable incomeByConcept(int year) {
        return table("Ingresos por concepto (" + year + ")", new String[]{"Concepto", "Pagos", "Total (COP)"},
                "SELECT c.nombre_concepto, COUNT(p.id_pago), SUM(p.monto_pagado) FROM pagos p "
                        + "JOIN conceptos_pago c ON c.id_concepto = p.id_concepto WHERE YEAR(p.fecha_pago) = ? "
                        + "GROUP BY c.id_concepto, c.nombre_concepto ORDER BY SUM(p.monto_pagado) DESC", year);
    }

    @Override
    public ReportTable incomeByMonth(int year) {
        return table("Ingresos por mes (" + year + ")", new String[]{"Mes", "Pagos", "Total (COP)"},
                "SELECT DATE_FORMAT(p.fecha_pago, '%Y-%m'), COUNT(*), SUM(p.monto_pagado) FROM pagos p "
                        + "WHERE YEAR(p.fecha_pago) = ? GROUP BY DATE_FORMAT(p.fecha_pago, '%Y-%m') "
                        + "ORDER BY 1", year);
    }

    @Override
    public ReportTable resourceStatus() {
        return table("Estado de los recursos", new String[]{"Estado", "Cantidad"},
                "SELECT estado_recurso, COUNT(*) FROM recursos GROUP BY estado_recurso "
                        + "ORDER BY FIELD(estado_recurso, 'Disponible', 'Prestado', 'En Mantenimiento', 'Dado de Baja')");
    }

    @Override
    public ReportTable overdueLoans(LocalDate today) {
        String student = "SELECT r.codigo_recurso AS codigo, r.nombre_recurso AS recurso, 'Estudiante' AS tipo, "
                + "CONCAT_WS(' ', b.primer_nombre, b.segundo_nombre, b.primer_apellido, b.segundo_apellido) AS nombre, "
                + "p.fecha_devolucion_esperada AS esperada, DATEDIFF(?, p.fecha_devolucion_esperada) AS dias "
                + "FROM prestamos_estudiante p JOIN recursos r ON r.id_recurso = p.id_recurso "
                + "JOIN estudiantes b ON b.id_estudiante = p.id_estudiante "
                + "WHERE p.estado_prestamo IN ('Activo', 'Vencido') AND p.fecha_devolucion_esperada < ?";
        String teacher = "SELECT r.codigo_recurso, r.nombre_recurso, 'Profesor', "
                + "CONCAT_WS(' ', b.primer_nombre, b.segundo_nombre, b.primer_apellido, b.segundo_apellido), "
                + "p.fecha_devolucion_esperada, DATEDIFF(?, p.fecha_devolucion_esperada) "
                + "FROM prestamos_profesor p JOIN recursos r ON r.id_recurso = p.id_recurso "
                + "JOIN profesores b ON b.id_profesor = p.id_profesor "
                + "WHERE p.estado_prestamo IN ('Activo', 'Vencido') AND p.fecha_devolucion_esperada < ?";
        Date now = Date.valueOf(today);
        return table("Préstamos vencidos",
                new String[]{"Código", "Recurso", "Tipo", "Prestatario", "Debía devolver", "Días"},
                student + " UNION ALL " + teacher + " ORDER BY esperada, codigo", now, now, now, now);
    }

    /** Ejecuta el SELECT y convierte cada columna a texto (null queda vacío). */
    private ReportTable table(String title, String[] headers, String sql, Object... params) {
        List<String[]> rows = query(sql, rs -> {
            String[] row = new String[headers.length];
            for (int i = 0; i < headers.length; i++) {
                String value = rs.getString(i + 1);
                row[i] = value == null ? "" : value;
            }
            return row;
        }, params);
        return new ReportTable(title, headers, rows);
    }
}
