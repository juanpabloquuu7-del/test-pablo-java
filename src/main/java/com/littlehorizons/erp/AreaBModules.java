package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.MenuModule;
import com.littlehorizons.erp.controller.ExtracurricularController;
import com.littlehorizons.erp.controller.FinanceController;
import com.littlehorizons.erp.controller.ReportController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.dao.ExtracurricularDaoImpl;
import com.littlehorizons.erp.dao.FinanceDaoImpl;
import com.littlehorizons.erp.dao.GuardianLookupDaoImpl;
import com.littlehorizons.erp.dao.ReportDaoImpl;
import com.littlehorizons.erp.dao.ResourceDaoImpl;
import com.littlehorizons.erp.dao.StudentLookupDaoImpl;
import com.littlehorizons.erp.dao.TeacherLookupDaoImpl;
import com.littlehorizons.erp.service.ExtracurricularService;
import com.littlehorizons.erp.service.FinanceService;
import com.littlehorizons.erp.service.ReportService;
import com.littlehorizons.erp.service.ResourceService;
import com.littlehorizons.erp.view.ExtracurricularView;
import com.littlehorizons.erp.view.FinanceView;
import com.littlehorizons.erp.view.ReportView;
import com.littlehorizons.erp.view.ResourceView;

import java.util.List;
import java.util.Scanner;

/**
 * Registro de módulos del Área B (Dev B): posiciones 4, 5, 6 y 7 del menú.
 * Dueño: Dev B. Para registrar un módulo, agregue su instancia a la lista, por ejemplo:
 * {@code new ClubController(new ClubView(scanner), new ExtracurricularService(new ClubDaoImpl()))}.
 */
public final class AreaBModules {

    private AreaBModules() {
    }

    public static List<MenuModule> create(Scanner scanner) {
        return List.of(
                // posición 4: Extracurriculares
                new ExtracurricularController(new ExtracurricularView(scanner),
                        new ExtracurricularService(new ExtracurricularDaoImpl(), new StudentLookupDaoImpl(),
                                new TeacherLookupDaoImpl())),
                // posición 5: Financiera
                new FinanceController(new FinanceView(scanner),
                        new FinanceService(new FinanceDaoImpl(), new StudentLookupDaoImpl(),
                                new GuardianLookupDaoImpl())),
                // posición 6: Recursos
                new ResourceController(new ResourceView(scanner),
                        new ResourceService(new ResourceDaoImpl(), new StudentLookupDaoImpl(),
                                new TeacherLookupDaoImpl())),
                // posición 7: Reportes
                new ReportController(new ReportView(scanner), new ReportService(new ReportDaoImpl()))
        );
    }
}
