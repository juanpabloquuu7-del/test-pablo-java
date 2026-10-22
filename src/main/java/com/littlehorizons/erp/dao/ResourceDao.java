package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.ResourceStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de inventario de recursos y de sus préstamos (estudiantes y profesores). */
public interface ResourceDao {

    List<ResourceCategory> findCategories();

    Optional<ResourceCategory> findCategoryById(int id);

    Optional<Resource> findById(int id);

    Optional<Resource> findByCode(String code);

    /** Inventario completo ordenado por código; con {@code status} distinto de null, solo ese estado. */
    List<Resource> findAll(ResourceStatus status);

    /** Inserta un recurso nuevo (Disponible) y devuelve su id. */
    int insert(Resource resource);

    /** Actualiza nombre, categoría, descripción, fecha de adquisición y ubicación. El código no cambia. */
    void updateDetails(Resource resource);

    void updateStatus(int resourceId, ResourceStatus status);

    /**
     * En una sola transacción pasa el recurso de Disponible a Prestado e inserta el préstamo.
     *
     * @return id del préstamo, o -1 si el recurso ya no estaba Disponible (no se escribe nada)
     */
    int lend(int resourceId, BorrowerType type, int borrowerId, LocalDate loanDate,
             LocalDate expectedReturn, String notes);

    /**
     * En una sola transacción cierra el préstamo (Devuelto) y deja el recurso Disponible.
     *
     * @return false si el préstamo ya no estaba abierto
     */
    boolean returnLoan(Loan loan, LocalDate returnDate);

    /** Marca como Vencido todo préstamo Activo cuya fecha esperada ya pasó. Devuelve cuántos cambió. */
    int markOverdue(LocalDate today);

    /** Préstamos Activos o Vencidos (sin devolver). */
    List<Loan> findOpenLoans();

    /** Préstamos sin devolver cuya fecha esperada es anterior a {@code today}. */
    List<Loan> findOverdue(LocalDate today);

    Optional<Loan> findOpenLoanByResource(int resourceId);
}
