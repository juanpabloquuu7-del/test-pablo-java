package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.ResourceStatus;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Entrada y salida de consola del módulo de recursos y préstamos. */
public class ResourceView extends BaseView {

    public ResourceView(Scanner scanner) {
        super(scanner);
    }

    public int showResourceMenu() {
        showMenu("Gestión de Recursos",
                "Registrar recurso",
                "Editar recurso",
                "Cambiar estado de un recurso",
                "Listar inventario",
                "Buscar recurso por código",
                "Prestar recurso a un estudiante",
                "Prestar recurso a un profesor",
                "Registrar devolución",
                "Préstamos sin devolver",
                "Reporte de préstamos vencidos");
        return readOption(10);
    }

    public String readCode() {
        return readRequired("Código del recurso: ");
    }

    public String readName() {
        return readRequired("Nombre del recurso: ");
    }

    public ResourceCategory chooseCategory(List<ResourceCategory> categories) {
        List<String> names = new ArrayList<>();
        for (ResourceCategory category : categories) {
            names.add(category.name());
        }
        return categories.get(readChoice("Categoría: ", names));
    }

    /** Fecha opcional: Enter la omite (devuelve null); si se escribe, debe ser AAAA-MM-DD. */
    public LocalDate readOptionalDate(String prompt) {
        while (true) {
            String text = readOptional(prompt + " (AAAA-MM-DD)");
            if (text == null) {
                return null;
            }
            try {
                return LocalDate.parse(text);
            } catch (DateTimeParseException e) {
                showError("Fecha inválida. Use el formato AAAA-MM-DD, por ejemplo 2026-03-15.");
            }
        }
    }

    /** Elige uno de los estados que se pueden fijar a mano (todos menos Prestado). */
    public ResourceStatus chooseManualStatus() {
        List<ResourceStatus> options = List.of(ResourceStatus.AVAILABLE, ResourceStatus.MAINTENANCE,
                ResourceStatus.RETIRED);
        List<String> labels = new ArrayList<>();
        for (ResourceStatus status : options) {
            labels.add(status.label());
        }
        return options.get(readChoice("Nuevo estado: ", labels));
    }

    /** Filtro de inventario: devuelve null para "todos". */
    public ResourceStatus chooseStatusFilter() {
        List<String> labels = new ArrayList<>(List.of("Todos"));
        for (ResourceStatus status : ResourceStatus.values()) {
            labels.add(status.label());
        }
        int index = readChoice("Mostrar: ", labels);
        return index == 0 ? null : ResourceStatus.values()[index - 1];
    }

    public void showResources(List<Resource> resources) {
        List<String[]> rows = new ArrayList<>();
        for (Resource r : resources) {
            rows.add(new String[]{r.code(), r.name(), r.categoryName(), r.status().label(),
                    r.location() == null ? "" : r.location()});
        }
        printTable(new String[]{"Código", "Nombre", "Categoría", "Estado", "Ubicación"}, rows);
    }

    public void showResourceDetail(Resource r) {
        System.out.println("Código:        " + r.code());
        System.out.println("Nombre:        " + r.name());
        System.out.println("Categoría:     " + r.categoryName());
        System.out.println("Estado:        " + r.status().label());
        System.out.println("Ubicación:     " + (r.location() == null ? "-" : r.location()));
        System.out.println("Adquisición:   " + (r.acquisitionDate() == null ? "-" : r.acquisitionDate()));
        System.out.println("Descripción:   " + (r.description() == null ? "-" : r.description()));
    }

    public void showLoans(List<Loan> loans) {
        List<String[]> rows = new ArrayList<>();
        for (Loan l : loans) {
            rows.add(new String[]{l.resourceCode(), l.resourceName(), l.borrowerType().label(), l.borrowerName(),
                    l.loanDate().toString(), l.expectedReturn().toString(), l.status()});
        }
        printTable(new String[]{"Recurso", "Nombre", "Tipo", "Prestado a", "Préstamo", "Devolver el", "Estado"}, rows);
    }
}
