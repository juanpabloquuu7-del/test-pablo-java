package test.controlador;

import com.littlehorizons.erp.config.DatabaseConnection;
import test.modelo.MateriaAsignada;
import test.modelo.ProfesorInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExportadorMateriasController {

    public static final int ANIO = 2025;
    private static final String SEP_PRINCIPAL = "=".repeat(79);
    private static final String SEP_SUB = "-".repeat(79);

    public ProfesorInfo buscarProfesor(int id) throws SQLException {
        String sql = "SELECT id_profesor, tipo_documento, numero_documento, primer_nombre, segundo_nombre, "
                + "primer_apellido, segundo_apellido FROM profesores WHERE id_profesor = ?";
        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String nombre = unir(rs.getString("primer_nombre"), rs.getString("segundo_nombre"),
                        rs.getString("primer_apellido"), rs.getString("segundo_apellido"));
                return new ProfesorInfo(rs.getInt("id_profesor"), rs.getString("tipo_documento"),
                        rs.getString("numero_documento"), nombre);
            }
        }
    }

    public List<MateriaAsignada> buscarMaterias(int idProfesor) throws SQLException {
        String sql = "SELECT DISTINCT m.codigo_materia, m.nombre_materia, g.nombre_grado, g.nivel_numerico "
                + "FROM asignaciones_profesor a "
                + "JOIN materias m ON m.id_materia = a.id_materia "
                + "JOIN grados g ON g.id_grado = m.id_grado "
                + "WHERE a.id_profesor = ? AND a.año_academico = ? "
                + "ORDER BY g.nivel_numerico, m.codigo_materia";
        List<MateriaAsignada> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProfesor);
            ps.setInt(2, ANIO);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new MateriaAsignada(rs.getString("codigo_materia"),
                            rs.getString("nombre_materia"), rs.getString("nombre_grado")));
                }
            }
        }
        return lista;
    }

    /** Escribe el archivo en la raíz del proyecto y devuelve su ruta. */
    public Path generarArchivo(ProfesorInfo p, List<MateriaAsignada> materias) throws IOException {
        Path destino = Paths.get("materias_profesor_" + p.getId() + "_" + ANIO + ".txt").toAbsolutePath();
        try (BufferedWriter w = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            w.write(construirContenido(p, materias));
        }
        return destino;
    }

    String construirContenido(ProfesorInfo p, List<MateriaAsignada> materias) {
        StringBuilder sb = new StringBuilder();
        sb.append(SEP_PRINCIPAL).append('\n');
        sb.append("           COLEGIO INTERNACIONAL \"LITTLE HORIZONS\"\n");
        sb.append("              REPORTE DE MATERIAS ASIGNADAS - AÑO ").append(ANIO).append('\n');
        sb.append(SEP_PRINCIPAL).append("\n\n");
        sb.append("DATOS DEL PROFESOR\n").append(SEP_SUB).append('\n');
        sb.append("Nombre completo    : ").append(p.getNombreCompleto()).append('\n');
        sb.append("Documento          : ").append(p.getTipoDocumento()).append(' ')
                .append(p.getNumeroDocumento()).append("\n\n");
        sb.append(SEP_PRINCIPAL).append('\n');
        sb.append("                        MATERIAS ASIGNADAS\n");
        sb.append(SEP_PRINCIPAL).append("\n\n");
        sb.append(String.format("%-10s%-35s%-20s", "Código", "Materia", "Grado")).append('\n');
        sb.append(SEP_SUB).append('\n');
        for (MateriaAsignada m : materias) {
            sb.append(String.format("%-10s%-35s%-20s", m.getCodigo(), m.getMateria(), m.getGrado())).append('\n');
        }
        sb.append('\n').append(SEP_PRINCIPAL).append("\n\n");
        sb.append("Documento generado el: ")
                .append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n\n");
        sb.append("_____________________________\n");
        sb.append("Recursos Humanos\n");
        sb.append("Colegio Internacional \"Little Horizons\"\n");
        return sb.toString();
    }

    private String unir(String... partes) {
        StringBuilder sb = new StringBuilder();
        for (String s : partes) {
            if (s != null && !s.trim().isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(s.trim());
            }
        }
        return sb.toString();
    }
}
