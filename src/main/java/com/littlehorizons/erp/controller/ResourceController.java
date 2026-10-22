package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.service.ResourceService;
import com.littlehorizons.erp.view.ResourceView;

import java.time.LocalDate;
import java.util.List;

/** Módulo 6 del menú: inventario de recursos, préstamos y devoluciones. */
public class ResourceController extends BaseController implements MenuModule {

    private final ResourceView resourceView;
    private final ResourceService service;

    public ResourceController(ResourceView view, ResourceService service) {
        super(view);
        this.resourceView = view;
        this.service = service;
    }

    @Override
    public int position() {
        return 6;
    }

    @Override
    public void run() {
        while (true) {
            int option = resourceView.showResourceMenu();
            if (option == 0) {
                return;
            }
            safely(() -> execute(option));
        }
    }

    private void execute(int option) {
        switch (option) {
            case 1 -> register();
            case 2 -> edit();
            case 3 -> changeStatus();
            case 4 -> list();
            case 5 -> search();
            case 6 -> lendToStudent();
            case 7 -> lendToTeacher();
            case 8 -> returnResource();
            case 9 -> resourceView.showLoans(service.listOpenLoans());
            case 10 -> overdueReport();
            default -> resourceView.showError("Opción no válida.");
        }
    }

    private void register() {
        String code = resourceView.readCode();
        String name = resourceView.readName();
        ResourceCategory category = resourceView.chooseCategory(service.listCategories());
        String description = resourceView.readOptional("Descripción");
        LocalDate acquired = resourceView.readOptionalDate("Fecha de adquisición");
        String location = resourceView.readOptional("Ubicación física");
        Resource created = service.register(code, name, category.id(), description, acquired, location);
        resourceView.showSuccess("Recurso " + created.code() + " registrado como " + created.status().label() + ".");
    }

    private void edit() {
        Resource current = service.getByCode(resourceView.readCode());
        resourceView.showResourceDetail(current);
        resourceView.showInfo("Escriba los nuevos datos; en los opcionales, Enter conserva el valor actual.");
        String name = resourceView.readOptional("Nombre [" + current.name() + "]");
        int categoryId = current.categoryId();
        if (resourceView.readYesNo("¿Cambiar la categoría? Actual: " + current.categoryName())) {
            categoryId = resourceView.chooseCategory(service.listCategories()).id();
        }
        String description = resourceView.readOptional("Descripción");
        LocalDate acquired = resourceView.readOptionalDate("Fecha de adquisición");
        String location = resourceView.readOptional("Ubicación física");
        Resource updated = service.updateDetails(current.code(),
                name != null ? name : current.name(), categoryId,
                description != null ? description : current.description(),
                acquired != null ? acquired : current.acquisitionDate(),
                location != null ? location : current.location());
        resourceView.showSuccess("Recurso " + updated.code() + " actualizado.");
    }

    private void changeStatus() {
        Resource current = service.getByCode(resourceView.readCode());
        resourceView.showInfo("Estado actual de " + current.code() + ": " + current.status().label());
        Resource updated = service.changeStatus(current.code(), resourceView.chooseManualStatus());
        resourceView.showSuccess("Recurso " + updated.code() + " ahora está " + updated.status().label() + ".");
    }

    private void list() {
        resourceView.showResources(service.listResources(resourceView.chooseStatusFilter()));
    }

    private void search() {
        resourceView.showResourceDetail(service.getByCode(resourceView.readCode()));
    }

    private void lendToStudent() {
        String code = resourceView.readCode();
        int studentId = resourceView.readInt("Id del estudiante: ", 1, Integer.MAX_VALUE);
        String notes = resourceView.readOptional("Observaciones");
        showLent(service.lendToStudent(code, studentId, notes));
    }

    private void lendToTeacher() {
        String code = resourceView.readCode();
        int teacherId = resourceView.readInt("Id del profesor: ", 1, Integer.MAX_VALUE);
        String notes = resourceView.readOptional("Observaciones");
        showLent(service.lendToTeacher(code, teacherId, notes));
    }

    private void showLent(Loan loan) {
        resourceView.showSuccess("Préstamo registrado: " + loan.resourceCode() + " a " + loan.borrowerName()
                + ". Debe devolverlo el " + loan.expectedReturn() + ".");
    }

    private void returnResource() {
        String code = resourceView.readCode();
        LocalDate date = LocalDate.now();
        if (!resourceView.readYesNo("¿Se devuelve hoy (" + date + ")?")) {
            date = resourceView.readDate("Fecha de devolución");
        }
        Loan loan = service.returnResource(code, date);
        resourceView.showSuccess("Devolución registrada: " + loan.resourceCode() + " de " + loan.borrowerName()
                + ". El recurso queda Disponible.");
    }

    private void overdueReport() {
        List<Loan> overdue = service.refreshAndListOverdue();
        resourceView.showLoans(overdue);
        if (!overdue.isEmpty()) {
            resourceView.showInfo("Los préstamos listados quedaron marcados como Vencido.");
        }
    }
}
