# Reglas del equipo

Proyecto: ERP Colegio Internacional "Little Horizons" (Java 17, Maven, MySQL 8, consola, MVC).

## Cómo se trabaja

El proyecto se divide en **dos áreas independientes**. Cada dev construye sus módulos completos (modelo, DAO, servicio, controlador y vista) en su rama, sin esperar al otro. Lo único compartido es el **núcleo**, que ya está hecho. Al final solo se integra en `develop`.

| Documento | Para qué |
|---|---|
| [01-nucleo-compartido.md](01-nucleo-compartido.md) | Lo que ya existe y cómo usarlo. Léanlo primero |
| [02-area-a.md](02-area-a.md) | Todo lo que debe hacer **Dev A** |
| [03-area-b.md](03-area-b.md) | Todo lo que debe hacer **Dev B** |
| [04-integracion-y-cierre.md](04-integracion-y-cierre.md) | Cómo se juntan las dos áreas y la entrega |

## Las dos áreas

| | **Área A (Dev A, rama `dev-a`)** | **Área B (Dev B, rama `dev-b`)** |
|---|---|---|
| Módulos del menú | 1 Estudiantes, 2 Académica, 3 Profesores | 4 Extracurriculares, 5 Financiera, 6 Recursos, 7 Reportes |
| Registro en el menú | `AreaAModules.java` | `AreaBModules.java` |
| SQL propio | `docs/sql/10-extras-a.sql`, `11-data-a.sql` | `docs/sql/20-extras-b.sql`, `21-data-b.sql` |
| Config propia | Sección "Área A" de `config.properties` | Sección "Área B" de `config.properties` |
| Extra | - | Factura PDF (`util/pdf/PdfGenerator`) |

## Reglas para que no se choquen

1. **Cada archivo tiene un solo dueño.** Los archivos del núcleo están congelados: se cambian solo con un PR que aprueben los dos.
2. **Cero dependencias de código entre áreas.** Un área nunca importa clases de la otra. Si necesita datos que son del otro área (por ejemplo B necesita el nombre de un estudiante), los lee con **su propia consulta SQL de solo lectura** dentro de su área.
3. **Nombres que no se pisan.** Los modelos de B que representan datos de A se llaman `StudentRef`, `GuardianRef`, `TeacherRef` y sus DAOs `StudentLookupDao`, `GuardianLookupDao`, `TeacherLookupDao`. A no crea ninguna clase que termine en `Ref` o `Lookup*`.
4. **Escritura por área.** A solo escribe en las tablas de A y B solo en las de B (lista abajo). Ambos pueden leer cualquier tabla.
5. **Integración mínima.** Cada dev registra sus módulos solo en su `AreaXModules.java`. `Main.java` no se toca.
6. **Cada dev prueba solo con el núcleo.** No se necesita nada del otro para ejecutar y probar su área.

### Tablas por área

| Escribe A | Escribe B |
|---|---|
| `estudiantes`, `acudientes`, `estudiante_acudiente`, `matriculas`, `grados`, `materias`, `profesores`, `asignaciones_profesor`, `horarios_clase`, `inscripciones_materia`, `registros_academicos`, `asistencias` | `clubes`, `membresias_club`, `eventos`, `participantes_evento`, `conceptos_pago`, `pagos`, `becas`, `facturas`, `categorias_recurso`, `recursos`, `prestamos_estudiante`, `prestamos_profesor` |

## Ramas y flujo de git

| Rama | Para qué | Quién escribe |
|---|---|---|
| `main` | Versiones estables y entregables. Siempre compila | Nadie directo. Recibe PR desde `develop` |
| `develop` | Integración: aquí se juntan A y B | Nadie directo. Recibe PR desde `dev-a` y `dev-b` |
| `dev-a` | Trabajo de **Dev A** | Solo Dev A |
| `dev-b` | Trabajo de **Dev B** | Solo Dev B |

- Cada dev trabaja solo en su rama. Para aislar una tarea grande puede crear `feat/<modulo>` desde su rama y volver a mezclarla en ella.
- Entregar un módulo terminado: `git push origin dev-x` y PR `dev-x` -> `develop`.
- Antes del PR: `git pull --rebase origin develop` y `mvn clean compile` sin errores.
- El otro dev revisa y aprueba (idealmente en menos de 24 horas). Merge con "squash".
- Al cerrar el proyecto, PR `develop` -> `main` (lo abre Dev A, lo aprueba Dev B).
- Nadie hace `push --force` sobre `main` ni `develop`.
- Commits en español, formato `tipo(modulo): descripcion` (ej. `feat(club): valida cupo maximo`). Sin líneas de atribución de herramientas.
- Nunca se sube una contraseña. La clave de MySQL va en la variable de entorno `LH_DB_PASSWORD`.

## Convenciones de código

- Paquete base: `com.littlehorizons.erp`. Capas: `model`, `dao`, `service`, `controller`, `view`.
- Clases e identificadores en inglés, mensajes al usuario en español.
- Modelos: `record` inmutables, sin lógica de persistencia.
- SQL solo en `dao/`, siempre vía `BaseJdbcDao` (PreparedStatement).
- Reglas de negocio solo en `service/` (lanzan `BusinessException`). La vista solo hace entrada y salida.
- Errores de SQL: `DataAccessException` (la lanza `BaseJdbcDao`). El `BaseController.safely` muestra el mensaje al usuario y registra el detalle en `logs/error_log.txt`.
- Capas: controller -> service -> dao, por interfaz e inyección por constructor.
- Cada controlador extiende `BaseController` e implementa `MenuModule`.

## Definición de "terminado" para un módulo

1. Compila (`mvn clean compile`).
2. Todas las entradas validadas con mensaje claro y reintento.
3. Mensaje de éxito o error en cada operación.
4. Se puede volver al menú anterior desde cualquier punto (`0. Volver`).
5. Probado a mano contra la base `erp_little_horizons` con los datos de prueba.
6. PR revisado por el otro dev.
