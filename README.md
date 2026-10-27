# ERP Colegio Internacional "Little Horizons"

Sistema de gestión educativa de consola para un colegio: estudiantes y acudientes, notas y asistencia, profesores, clubes y eventos, pagos con factura electrónica en PDF, recursos prestables y reportes. Está hecho en Java 17 con Maven y MySQL 8, con arquitectura MVC por capas.

## Autores

- Alejandro Acevedo: Área A (módulos 1 a 3) y núcleo compartido.
- Juan Pablo: Área B (módulos 4 a 7).

## Tecnologías

- Java 17 y Maven.
- MySQL 8 con JDBC (`mysql-connector-j` 8.4.0).
- Apache PDFBox 2.0.31 (factura en PDF) y ZXing 3.5.3 (código QR).
- JUnit 5 para las pruebas.

## Requisitos

- JDK 17 y Maven 3.9 o superior.
- MySQL 8 con un usuario que pueda crear y escribir la base `campuslands`. Se recomienda un usuario dedicado en lugar de `root`.

## Configuración de la base de datos

1. Cargar los scripts de `docs/sql` en este orden: `01-schema.sql`, `02-data-base.sql`, `10-extras-a.sql`, `11-data-a.sql`, `20-extras-b.sql` y `21-data-b.sql`.
2. Cargarlos **una sola vez** sobre una base limpia: los datos de prueba tienen códigos únicos y fechas relativas al día de la carga.
3. Ajustar `src/main/resources/config.properties` (URL y usuario). La contraseña **no** va en el archivo: se define con la variable de entorno `LH_DB_PASSWORD`. Un `config.properties` en la carpeta de ejecución sobrescribe al del jar.

Claves principales de configuración:

| Clave | Uso |
|---|---|
| `business.academicYear` | Año académico vigente (0 = año actual). Los datos de prueba son de 2026. |
| `business.weeklyAbsenceThreshold` | Ausencias en una semana que disparan la alerta. |
| `business.loanDays` | Días de plazo de un préstamo de recurso. |
| `business.siblingDiscountPercent` | Descuento por hermanos en conceptos recurrentes. |
| `business.delinquencyConcept` y `business.delinquencyDays` | Concepto y días sin pago para considerar moroso a un estudiante. |
| `business.ivaPercent` | IVA incluido en el total de la factura (0 = exento). |
| `school.*` | Datos del colegio que salen en la factura. |
| `paths.invoicesDir` y `paths.logFile` | Carpeta de facturas y archivo de log. |

## Instalación y ejecución

```
mvn clean package    # compila, corre las pruebas y genera el jar
mvn exec:java        # ejecuta la aplicación desde Maven
```

El jar generado en `target/` incluye todas las dependencias y se ejecuta con `java -jar`.

## Estructura

Paquete base `com.littlehorizons.erp`, organizado por capas:

| Paquete | Contenido |
|---|---|
| `config` | `AppConfig` y `DatabaseConnection` (núcleo) |
| `model` | Registros e enumeraciones del dominio |
| `dao` | Interfaz y `...DaoImpl` por entidad; todo el SQL vive aquí |
| `service` | Reglas de negocio |
| `controller` | Controladores de menú; cada uno es un `MenuModule` con su posición |
| `view` | Entrada y salida de consola |
| `util` | Validaciones, excepciones, log y `pdf/PdfGenerator` |

Cada área registra sus módulos en `AreaAModules.java` y `AreaBModules.java`; `Main` los junta en el menú principal.

## Patrones

- **Singleton:** `AppConfig` y `DatabaseConnection`.
- **DAO:** todo el acceso a datos con `PreparedStatement` a través de `BaseJdbcDao`.
- **MVC por capas:** controlador, servicio y DAO conectados por interfaz; la vista solo hace entrada y salida.
- **Inyección por constructor:** el ensamblado se hace en los archivos de registro de módulos.
- **Transacciones:** las operaciones compuestas (matrícula, inscripción con cupo, préstamo, pago con factura) son atómicas.

## Funcionalidades

| # | Módulo | Qué hace |
|---|---|---|
| 1 | Estudiantes | Registro con acudientes y matrícula, búsqueda, listado por grado, cambio de estado e inscripción en materias |
| 2 | Académica | Notas de 0.0 a 5.0, historial, promedios y asistencia con alerta de inasistencias |
| 3 | Profesores | Registro, asignaciones de materia, horarios sin traslapes y carga académica |
| 4 | Extracurriculares | Clubes con cupo atómico, retiros y eventos con participantes y posiciones |
| 5 | Financiera | Conceptos, becas, descuentos, pagos, historiales, morosos y factura electrónica PDF |
| 6 | Recursos | Inventario, préstamos a estudiantes y profesores, devoluciones y vencidos |
| 7 | Reportes | Estudiantes por grado, mejores promedios, clubes, eventos, ingresos, recursos y préstamos vencidos |

El detalle de cada área está en [`docs/readme-a.md`](docs/readme-a.md) y [`docs/readme-b.md`](docs/readme-b.md).

### Factura electrónica

Al registrar un pago se asignan recibo y factura consecutivos y se genera `factura_000123.pdf` en `paths.invoicesDir`, con los datos del colegio, el acudiente responsable, los ítems, el IVA, el total, un CUFE simulado (SHA-256 de número + NIT + fecha) y un código QR. Si el PDF falla, el pago queda guardado y se puede regenerar desde el menú Financiera.

## Errores y logs

Las reglas de negocio violadas se muestran al usuario. Los errores de base de datos o inesperados muestran un mensaje genérico y el detalle se escribe en `logs/error_log.txt` (ignorado por git). Ninguna operación cierra el programa por un error.

## Pruebas

`mvn test` ejecuta las pruebas unitarias. `SqlSmokeIT` ejecuta las consultas contra MySQL real y solo corre si se indica una base temporal con `-Dtest=SqlSmokeIT -Dlh.it.db=<base>`; **escribe datos**, así que nunca debe apuntarse a la base de producción.

## Limitaciones

- No hay usuarios ni roles: quien use la consola ve todas las fichas, incluidos los datos médicos.
- La factura es simulada: el CUFE y la resolución no tienen validez ante la DIAN.
- La morosidad mide solo el concepto configurado y no distingue el mes adeudado.
- Aplicación de consola, pensada para un colegio y un operador por sesión.

## Licencia

MIT. Ver [`LICENSE`](LICENSE).

## Ejecutar en IntelliJ IDEA (JetBrains)

Para abrir, compilar y ejecutar el proyecto en IntelliJ IDEA (Community o Ultimate) en Windows sin pasos ocultos:

1. **Abrir el proyecto:** Seleccionar **File > Open**, navegar a la carpeta del repositorio y elegir el archivo `pom.xml` (abrir como proyecto Maven).
2. **Configurar el SDK (JDK 17+):** En **File > Project Structure > Project**, verificar que el SDK configurado sea Java 17 o superior (el archivo `pom.xml` define `<maven.compiler.release>17</maven.compiler.release>`).
3. **Importar dependencias Maven:** Esperar a que IntelliJ finalice la sincronización e importación del proyecto Maven (ventana de herramientas Maven o barra de progreso).
4. **Definir contraseña de base de datos (`LH_DB_PASSWORD`):**
   - Como variable de entorno del sistema (`LH_DB_PASSWORD`), o
   - En la configuración de ejecución del IDE: ir a **Run > Edit Configurations**, seleccionar **Little Horizons ERP**, y en **Environment variables** definir `LH_DB_PASSWORD=tu_contraseña` (la configuración compartida en `.run/Little Horizons ERP.run.xml` se distribuye con `envs` vacío por seguridad para no almacenar contraseñas en el repositorio).
5. **Configuración local de MySQL (`config.properties`):** Si el usuario o la URL de MySQL difieren de los valores por defecto (`root` en `localhost:3306`), copiar `src/main/resources/config.properties` a la raíz del proyecto y ajustar `db.url` o `db.user`. El archivo en la raíz está ignorado por Git y tiene prioridad sobre el classpath.
6. **Cargar la base de datos:** Cargar el script SQL consolidado `docs/database.sql` en MySQL 8 (por ejemplo: `mysql -u root -p < docs/database.sql`).
7. **Ejecutar la aplicación:** Seleccionar la configuración de ejecución compartida **«Little Horizons ERP»** en la barra superior y hacer clic en **Run** (o `Shift+F10`). Usar la ventana interactiva **Run** de IntelliJ para operar el sistema (la consola de IntelliJ gestiona la entrada interactiva por teclado directamente). También se incluyen las configuraciones Maven compartidas **«Compilar y probar»** (`clean test`) y **«Empaquetar jar»** (`clean package`).
8. **Codificación UTF-8:** En **Settings > Editor > File Encodings**, verificar que *Global Encoding*, *Project Encoding* y *Default encoding for properties files* estén configurados en `UTF-8`. La configuración compartida ya incorpora las VM options (`-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8`), lo que asegura que los marcos de tabla y acentos se rendericen correctamente en la consola del IDE.
9. **Problemas típicos:**
   - *Access denied for user...*: Falta configurar la variable de entorno `LH_DB_PASSWORD` en el sistema o en Edit Configurations.
   - *Communications link failure*: El servidor MySQL se encuentra apagado o no es accesible en el puerto configurado.
   - *Caracteres raros o marcos rotos*: Verificar que las VM options de codificación UTF-8 estén presentes en la configuración de ejecución.
10. **Consola interactiva vs IDE:** La clase `Main` únicamente cambia la página de códigos de Windows (`chcp 65001`) si detecta una consola de terminal interactiva real (`System.console() != null`). Al correr dentro del IDE IntelliJ IDEA, `System.console()` retorna `null`, por lo que dicha invocación se omite automáticamente y no interfiere con el entorno de ejecución del IDE.
