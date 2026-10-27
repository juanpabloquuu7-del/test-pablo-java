package com.littlehorizons.erp.controller;

/**
 * Un módulo del menú principal. Cada área registra los suyos en {@code AreaAModules} o {@code AreaBModules}.
 * La posición (1 a 7) fija el lugar en el menú:
 * 1 Estudiantes, 2 Académica, 3 Profesores, 4 Extracurriculares, 5 Financiera, 6 Recursos, 7 Reportes.
 */
public interface MenuModule {

    /** Posición en el menú principal, de 1 a 7. */
    int position();

    /** Abre el submenú del módulo y vuelve cuando el usuario elige "0. Volver". */
    void run();
}
