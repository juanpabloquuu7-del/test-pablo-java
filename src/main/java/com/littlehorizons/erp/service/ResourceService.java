package com.littlehorizons.erp.service;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentLookupDao;
import com.littlehorizons.erp.dao.TeacherLookupDao;
import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.ResourceStatus;
import com.littlehorizons.erp.model.StudentRef;
import com.littlehorizons.erp.model.TeacherRef;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.InputValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

/** Reglas de negocio de inventario, préstamos y devoluciones de recursos. */
public class ResourceService {

    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9-]{3,50}$");

    private final ResourceDao resources;
    private final StudentLookupDao students;
    private final TeacherLookupDao teachers;

    public ResourceService(ResourceDao resources, StudentLookupDao students, TeacherLookupDao teachers) {
        this.resources = resources;
        this.students = students;
        this.teachers = teachers;
    }

    // ---------- Inventario ----------

    public List<ResourceCategory> listCategories() {
        return resources.findCategories();
    }

    public List<Resource> listResources(ResourceStatus statusOrNull) {
        return resources.findAll(statusOrNull);
    }

    public Resource getByCode(String code) {
        return resources.findByCode(code.trim())
                .orElseThrow(() -> new BusinessException("No existe un recurso con el código " + code + "."));
    }

    public Resource register(String code, String name, int categoryId, String description,
                             LocalDate acquisitionDate, String location) {
        String cleanCode = code == null ? "" : code.trim().toUpperCase();
        if (!CODE.matcher(cleanCode).matches()) {
            throw new BusinessException("El código debe tener entre 3 y 50 letras, números o guiones.");
        }
        if (resources.findByCode(cleanCode).isPresent()) {
            throw new BusinessException("Ya existe un recurso con el código " + cleanCode + ".");
        }
        validateDetails(name, categoryId, acquisitionDate, location);
        Resource draft = new Resource(0, cleanCode, name.trim(), categoryId, null, blankToNull(description),
                acquisitionDate, blankToNull(location), ResourceStatus.AVAILABLE);
        int id = resources.insert(draft);
        return resources.findById(id).orElseThrow(() -> new BusinessException("No se pudo leer el recurso creado."));
    }

    public Resource updateDetails(String code, String name, int categoryId, String description,
                                  LocalDate acquisitionDate, String location) {
        Resource current = getByCode(code);
        validateDetails(name, categoryId, acquisitionDate, location);
        resources.updateDetails(new Resource(current.id(), current.code(), name.trim(), categoryId, null,
                blankToNull(description), acquisitionDate, blankToNull(location), current.status()));
        return getByCode(code);
    }

    /**
     * Cambio manual de estado. "Prestado" solo lo fijan los préstamos y devoluciones, y un recurso con
     * un préstamo sin devolver no puede cambiar de estado a mano.
     */
    public Resource changeStatus(String code, ResourceStatus newStatus) {
        Resource resource = getByCode(code);
        if (newStatus == ResourceStatus.ON_LOAN) {
            throw new BusinessException("El estado Prestado solo se asigna al registrar un préstamo.");
        }
        if (resource.status() == ResourceStatus.ON_LOAN || resources.findOpenLoanByResource(resource.id()).isPresent()) {
            throw new BusinessException("El recurso tiene un préstamo sin devolver. Registre primero la devolución.");
        }
        if (resource.status() == newStatus) {
            throw new BusinessException("El recurso ya está en estado " + newStatus.label() + ".");
        }
        resources.updateStatus(resource.id(), newStatus);
        return getByCode(code);
    }

    // ---------- Préstamos ----------

    public Loan lendToStudent(String resourceCode, int studentId, String notes) {
        StudentRef student = students.findById(studentId)
                .orElseThrow(() -> new BusinessException("No existe el estudiante con id " + studentId + "."));
        if (!student.active()) {
            throw new BusinessException("El estudiante " + student.fullName() + " no está activo.");
        }
        return lend(resourceCode, BorrowerType.STUDENT, student.id(), notes);
    }

    public Loan lendToTeacher(String resourceCode, int teacherId, String notes) {
        TeacherRef teacher = teachers.findActiveById(teacherId)
                .orElseThrow(() -> new BusinessException("No existe un profesor activo con id " + teacherId + "."));
        return lend(resourceCode, BorrowerType.TEACHER, teacher.id(), notes);
    }

    private Loan lend(String resourceCode, BorrowerType type, int borrowerId, String notes) {
        Resource resource = getByCode(resourceCode);
        if (resource.status() != ResourceStatus.AVAILABLE) {
            throw new BusinessException("El recurso " + resource.code() + " no está disponible (estado: "
                    + resource.status().label() + ").");
        }
        LocalDate today = LocalDate.now();
        LocalDate due = today.plusDays(AppConfig.getInstance().getInt("business.loanDays", 7));
        int loanId = resources.lend(resource.id(), type, borrowerId, today, due, blankToNull(notes));
        if (loanId < 0) {
            throw new BusinessException("El recurso " + resource.code() + " acaba de ser prestado a otra persona.");
        }
        return resources.findOpenLoanByResource(resource.id())
                .orElseThrow(() -> new BusinessException("No se pudo leer el préstamo creado."));
    }

    /** Registra la devolución del préstamo abierto (Activo o Vencido) del recurso. */
    public Loan returnResource(String resourceCode, LocalDate returnDate) {
        Resource resource = getByCode(resourceCode);
        Loan loan = resources.findOpenLoanByResource(resource.id())
                .orElseThrow(() -> new BusinessException("El recurso " + resource.code() + " no tiene un préstamo sin devolver."));
        if (returnDate.isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha de devolución no puede ser futura.");
        }
        if (returnDate.isBefore(loan.loanDate())) {
            throw new BusinessException("La fecha de devolución no puede ser anterior al préstamo (" + loan.loanDate() + ").");
        }
        if (!resources.returnLoan(loan, returnDate)) {
            throw new BusinessException("El préstamo ya había sido devuelto.");
        }
        return new Loan(loan.id(), loan.resourceId(), loan.resourceCode(), loan.resourceName(), loan.borrowerType(),
                loan.borrowerId(), loan.borrowerName(), loan.loanDate(), loan.expectedReturn(), returnDate, "Devuelto");
    }

    public List<Loan> listOpenLoans() {
        return resources.findOpenLoans();
    }

    /** Marca como Vencidos los préstamos atrasados y devuelve todos los atrasados con quién los tiene. */
    public List<Loan> refreshAndListOverdue() {
        LocalDate today = LocalDate.now();
        resources.markOverdue(today);
        return resources.findOverdue(today);
    }

    // ---------- Validaciones ----------

    private void validateDetails(String name, int categoryId, LocalDate acquisitionDate, String location) {
        if (!InputValidator.isNotBlank(name) || name.trim().length() > 150) {
            throw new BusinessException("El nombre es obligatorio y admite hasta 150 caracteres.");
        }
        if (resources.findCategoryById(categoryId).isEmpty()) {
            throw new BusinessException("La categoría indicada no existe.");
        }
        if (acquisitionDate != null && acquisitionDate.isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha de adquisición no puede ser futura.");
        }
        if (location != null && location.trim().length() > 100) {
            throw new BusinessException("La ubicación admite hasta 100 caracteres.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
