package test.vista;

import test.controlador.ExportadorMateriasController;
import test.modelo.MateriaAsignada;
import test.modelo.ProfesorInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class ExportadorMateriasProfesorMain {

    private static final String LINEA = "=".repeat(79);
    private static final String PROMPT = "Ingrese el ID del profesor para exportar sus materias asignadas: ";

    public static void main(String[] args) {
        ejecutarExportador();
    }

    private static void ejecutarExportador() {
        Scanner sc = new Scanner(System.in);
        ExportadorMateriasController ctrl = new ExportadorMateriasController();

        System.out.println(LINEA);
        System.out.println("     EXPORTADOR DE MATERIAS ASIGNADAS A PROFESOR - AÑO 2025");
        System.out.println(LINEA);
        System.out.println();

        try {
            ProfesorInfo profesor = null;
            while (profesor == null) {
                System.out.print(PROMPT);
                String linea = sc.nextLine().trim();
                int id;
                try {
                    id = Integer.parseInt(linea);
                } catch (NumberFormatException e) {
                    System.out.println("\nError: Debe ingresar un número válido.");
                    continue;
                }
                profesor = ctrl.buscarProfesor(id);
                if (profesor == null) {
                    System.out.println("\nError: No se encontró ningún profesor con el ID " + id + ".");
                    System.out.println("Verifique el ID e intente nuevamente.\n");
                }
            }

            System.out.println("\nGenerando reporte para profesor con ID " + profesor.getId() + "...");
            System.out.println("Consultando asignaciones del año 2025...");
            List<MateriaAsignada> materias = ctrl.buscarMaterias(profesor.getId());
            if (materias.isEmpty()) {
                System.out.println("\nError: El profesor " + profesor.getNombreCompleto()
                        + " no tiene materias asignadas \npara el año académico 2025.");
                salir(sc);
                return;
            }

            try {
                Path ruta = ctrl.generarArchivo(profesor, materias);
                System.out.println();
                System.out.println(LINEA);
                System.out.println("            ARCHIVO GENERADO EXITOSAMENTE");
                System.out.println(LINEA);
                System.out.println();
                System.out.println("Profesor: " + profesor.getNombreCompleto());
                System.out.println("Archivo generado: " + ruta.getFileName());
                System.out.println("Ubicación: " + ruta);
                System.out.println();
                System.out.println("El archivo está listo para ser utilizado.");
            } catch (IOException e) {
                System.out.println(LINEA);
                System.out.println("ERROR: No se pudo crear el archivo de exportación.");
                System.out.println("Verifique los permisos de escritura en la carpeta del proyecto.");
                System.out.println(LINEA);
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("Error de base de datos: " + e.getMessage());
        }
        salir(sc);
    }

    private static void salir(Scanner sc) {
        System.out.println();
        System.out.println(LINEA);
        System.out.println("Presione Enter para salir...");
        System.out.println(LINEA);
        sc.nextLine();
    }
}
