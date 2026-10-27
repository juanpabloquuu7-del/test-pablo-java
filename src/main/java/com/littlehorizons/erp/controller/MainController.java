package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.view.MainMenuView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Controla el menú principal y delega en el {@link MenuModule} elegido. */
public class MainController extends BaseController {

    private final MainMenuView menuView;
    private final Map<Integer, MenuModule> modules = new HashMap<>();

    public MainController(MainMenuView menuView, List<MenuModule> registered) {
        super(menuView);
        this.menuView = menuView;
        for (MenuModule module : registered) {
            if (module.position() < 1 || module.position() > MainMenuView.TITLES.length) {
                throw new IllegalArgumentException("Posición de menú inválida: " + module.position());
            }
            if (modules.put(module.position(), module) != null) {
                throw new IllegalArgumentException("Posición de menú repetida: " + module.position());
            }
        }
    }

    public void run() {
        while (true) {
            int option = menuView.showMainMenu(modules.keySet());
            if (option == MainMenuView.EXIT) {
                menuView.showGoodbye();
                return;
            }
            MenuModule module = modules.get(option);
            if (module == null) {
                menuView.showUnavailable();
            } else {
                safely(module::run);
            }
        }
    }
}
