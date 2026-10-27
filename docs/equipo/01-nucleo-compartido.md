# Núcleo compartido (ya hecho)

Está en `main` y `develop`. Es lo único que usan los dos. No lo modifiquen sin un PR que aprueben ambos.

## Puesta en marcha (los dos, una vez)

1. Instalar JDK 17 o superior, Maven 3.9+, MySQL 8.0.16 o superior y un cliente (DBeaver o Workbench).
2. Tener acceso de escritura al repo en GitHub (colaborador).
3. Clonar y elegir rama:
   ```
   git clone https://github.com/alejandro-xoxo/little-horizons-erp.git
   cd little-horizons-erp
   git checkout dev-a      # Dev A
   git checkout dev-b      # Dev B
   ```
4. Definir la variable de entorno `LH_DB_PASSWORD` con la contraseña de su MySQL local. Si su usuario no es `root`, copie `config.properties` a la carpeta de ejecución (se lee y sobrescribe al del proyecto) y cambie `db.user`. Ese archivo local no se sube.
5. Crear la base. Orden obligatorio (los archivos del otro área también se pueden cargar, ver abajo):
   ```
   mysql -u root -p --default-character-set=utf8mb4 < docs/sql/01-schema.sql
   mysql -u root -p --default-character-set=utf8mb4 < docs/sql/02-data-base.sql
   ```
   Cada dev carga además los suyos: A `10-extras-a.sql` y `11-data-a.sql`; B `20-extras-b.sql` y `21-data-b.sql`.
   **Ojo:** `01-schema.sql` empieza con `DROP DATABASE IF EXISTS`.
6. Compilar y ejecutar: `mvn clean compile` y `mvn exec:java`. Debe aparecer el menú principal con los 7 módulos "(en desarrollo)".

## Qué contiene el núcleo

| Archivo | Para qué |
|---|---|
| `config/AppConfig` | Singleton. `get(key)`, `getInt`, `getDouble`, `getAcademicYear()`. Lee `config.properties`; la contraseña viene de `LH_DB_PASSWORD` |
| `config/DatabaseConnection` | Singleton. `getConnection()` |
| `dao/BaseJdbcDao` | Base de todos los DAO. Ver "Cómo escribir un DAO" |
| `util/CustomExceptions` | `BusinessException(msg)` y `DataAccessException(msg, causa)` |
| `util/LoggerUtility` | `logError(contexto, excepcion)` -> `logs/error_log.txt` |
| `util/InputValidator` | `isNotBlank`, `isValidDocument`, `isValidEmail`, `isValidPhone`, `isValidGrade` |
| `view/BaseView` | Entrada/salida de consola. Ver "Cómo escribir una vista" |
| `controller/BaseController` | `safely(Runnable)`: captura errores y nunca cierra el programa |
| `controller/MenuModule` | Interfaz que implementa cada módulo del menú: `position()` y `run()` |
| `controller/MainController`, `view/MainMenuView`, `Main` | Menú principal. No se tocan |
| `AreaAModules`, `AreaBModules` | Donde cada dev registra sus módulos |
| `docs/sql/01-schema.sql` | Esquema completo (oficial + `horarios_clase` y `becas`) |
| `docs/sql/02-data-base.sql` | 48 estudiantes, 36 acudientes, 12 profesores, 48 matrículas |

### IDs que ambas áreas pueden asumir (de `02-data-base.sql`)

- Estudiantes 1 a 48 (4 por grado), acudientes 1 a 36, profesores 1 a 12.
- Grados 1 a 12 (`id_grado` = nivel + 1; Preescolar es 1, Once es 12). Conceptos de pago 1 a 7. Categorías de recurso 1 a 5.
- Matrículas del año 2026 para los 48 estudiantes. Hermanos: los estudiantes 25 a 36 comparten acudiente con los estudiantes 1 a 12.

### Cómo escribir un DAO

```java
public class ClubDaoImpl extends BaseJdbcDao implements ClubDao {
    @Override
    public Optional<Club> findById(int id) {
        return queryOne("SELECT * FROM clubes WHERE id_club = ?", this::map, id);
    }

    @Override
    public int insert(Club c) {
        return insert("INSERT INTO clubes (nombre_club, capacidad_maxima) VALUES (?, ?)",
                c.name(), c.maxCapacity());
    }

    private Club map(ResultSet rs) throws SQLException { ... }
}
```

- `query`, `queryOne`, `update`, `insert` abren y cierran la conexión solos.
- Para varias operaciones atómicas (por ejemplo un préstamo: insertar préstamo y cambiar el estado del recurso) use `inTransaction(c -> { ... })` con las versiones `query(c, ...)`, `update(c, ...)` e `insert(c, ...)`.

### Cómo escribir una vista, un controlador y registrar el módulo

```java
public class ClubView extends BaseView {
    public ClubView(Scanner scanner) { super(scanner); }
    // showMenu("Gestión de Clubes", "Registrar club", "Listar clubes"), readOption(2), readRequired(...), readDate(...), printTable(...)
}

public class ClubController extends BaseController implements MenuModule {
    public int position() { return 4; }
    public void run() {
        while (true) {
            view.showMenu("Actividades Extracurriculares", "Registrar club", "Listar clubes");
            int option = view.readOption(2);
            if (option == 0) return;
            safely(() -> { ... });
        }
    }
}
```

Registro: agregar la instancia en `create(scanner)` de `AreaAModules` o `AreaBModules`. Cada módulo tiene una posición fija en el menú (1 a 7); si dos módulos usan la misma posición, la aplicación no arranca.

`BaseView` ofrece: `banner`, `showMenu`, `readOption`, `readInt`, `readDouble`, `readMoney`, `readRequired`, `readOptional`, `readDate`, `readYesNo`, `readChoice`, `printTable`, `showSuccess`, `showError`, `showInfo`, `pause`. Si falta algo, pídanlo por issue; no creen utilidades paralelas.
