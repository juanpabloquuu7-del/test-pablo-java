package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.CustomExceptions.DataAccessException;
import com.littlehorizons.erp.util.LoggerUtility;
import com.littlehorizons.erp.view.BaseView;

import java.util.NoSuchElementException;

/** Base de los controladores: ninguna operación puede cerrar el programa. */
public abstract class BaseController {

    protected final BaseView view;

    protected BaseController(BaseView view) {
        this.view = view;
    }

    /**
     * Ejecuta una acción de menú. Regla de negocio violada: se muestra el mensaje al usuario.
     * Error de datos o inesperado: mensaje genérico y detalle en {@code logs/error_log.txt}.
     */
    protected void safely(Runnable action) {
        try {
            action.run();
        } catch (BusinessException e) {
            view.showError(e.getMessage());
        } catch (NoSuchElementException e) {
            throw e;
        } catch (DataAccessException e) {
            LoggerUtility.logError(getClass().getSimpleName(), e);
            view.showError("No se pudo completar la operación con la base de datos. Revise logs/error_log.txt.");
        } catch (RuntimeException e) {
            LoggerUtility.logError(getClass().getSimpleName(), e);
            view.showError("Ocurrió un error inesperado. Revise logs/error_log.txt.");
        }
    }
}
