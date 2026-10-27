# Área B: módulos 4 a 7 (secciones para el README final)

## Tecnologías

- Java 17 y Maven; arquitectura MVC por capas (modelo, DAO, servicio, controlador, vista de consola).
- MySQL 8 con JDBC (`mysql-connector-j` 8.4.0); todo el SQL usa `PreparedStatement`.
- Apache PDFBox 2.0.31 para la factura en PDF y ZXing 3.5.3 para el código QR.
- JUnit 5 para las pruebas.

## Requisitos

- JDK 17, Maven 3.9 o superior y MySQL 8.
- Usuario de MySQL con permisos sobre la base `erp_little_horizons`.

## Configuración de la base de datos

1. Cargar en este orden: `docs/sql/01-schema.sql`, `docs/sql/02-data-base.sql` y, para tener datos de prueba del Área B, `docs/sql/21-data-b.sql`.
2. Cargar `21-data-b.sql` **una sola vez** (los códigos de recurso y de concepto son únicos). Sus fechas de préstamos y pagos recientes son relativas al día de la carga, por eso siempre hay préstamos vencidos y estudiantes morosos.
3. `docs/sql/20-extras-b.sql` no cambia el esquema: el Área B usa las tablas del esquema base sin modificaciones.
4. La contraseña de MySQL va en la variable de entorno `LH_DB_PASSWORD`; usuario y URL, en `config.properties`.

Claves del Área B en `config.properties`:

| Clave | Uso |
|---|---|
| `business.loanDays` | Días de plazo de un préstamo de recurso. |
| `business.siblingDiscountPercent` | Descuento por hermanos en conceptos recurrentes. |
| `business.delinquencyConcept` y `business.delinquencyDays` | Concepto y días sin pago para considerar moroso a un estudiante. |
| `business.ivaPercent` | IVA incluido en el total de las facturas (0 = exento). |
| `school.*` | Razón social, NIT, dirección, teléfono, correo y resolución que salen en la factura. |
| `paths.invoicesDir` | Carpeta donde se guardan los PDF. |

## Instalación y ejecución

```
mvn package          # compila, corre las pruebas y genera el jar
mvn exec:java        # ejecuta la aplicación desde Maven
```

El menú principal muestra los módulos 4 a 7 junto a los del Área A.

## Funcionalidades

### Módulo 4: Extracurriculares

- Catálogo de clubes con cupo máximo y profesor encargado.
- Inscripción con cupo atómico (la validación y el insert van en la misma transacción) y sin inscripción duplicada en un club; un estudiante puede estar en varios.
- Retiro con fecha y motivo; la membresía queda Retirada.
- Eventos (Académico, Cultural, Deportivo, Social, Científico) con participantes y, en los de competencia, posición (1er, 2do, 3er Lugar o Participante).

### Módulo 5: Financiera

- Catálogo de conceptos de pago: alta, edición, activar y desactivar.
- Valor personalizado por estudiante: en conceptos recurrentes se aplica el **mayor** entre el descuento por hermanos y la beca (no se suman). El monto final puede ajustarse a mano al cobrar.
- Becas parciales por estudiante y año académico.
- Registro de pagos con estudiante, acudiente, concepto, monto, fecha, método y período; recibo (`REC-000123`) y factura (`FE-000123`) consecutivos asignados en la misma transacción que el pago.
- Historial de pagos por estudiante y por acudiente (todos sus hijos).
- Reporte de morosos: estudiantes con matrícula activa sin pago del concepto de pensión en los últimos `delinquencyDays` días (o desde la matrícula si nunca pagaron).

### Factura electrónica

Al confirmar un pago se genera `factura_000123.pdf` en `paths.invoicesDir` y se muestra la ruta. Contiene los datos del colegio (`school.*`), el cliente (acudiente responsable), número, fecha, ítems, subtotal, IVA y total, el CUFE simulado y un código QR. El CUFE es el SHA-256 de número de factura + NIT + fecha de emisión. En la tabla `facturas` se guardan número, CUFE, nombre del archivo y ruta. Si el PDF falla, el pago ya queda guardado y la opción "Regenerar PDF de una factura" lo crea después.

### Módulo 6: Recursos

- Inventario con código único, categoría, estado (Disponible, Prestado, En Mantenimiento, Dado de Baja) y ubicación.
- Préstamo a estudiante o profesor solo si el recurso está Disponible, con fecha de devolución = hoy + `business.loanDays`; el recurso pasa a Prestado en la misma transacción.
- Devolución con fecha real; reporte de préstamos vencidos que además los marca como Vencido.

### Módulo 7: Reportes

Solo lectura: estudiantes por grado, mejores promedios (top 10), participación en clubes y en eventos, ingresos por concepto y por mes, estado de los recursos y préstamos vencidos. Los reportes por año usan `business.academicYear`.

## Limitaciones

- La factura es simulada: el CUFE y la resolución no tienen validez ante la DIAN.
- La aplicación es de consola y de un solo usuario por sesión; el recibo y la factura consecutivos se protegen con un bloqueo en la base de datos y con restricciones UNIQUE.
- La morosidad mide solo el concepto configurado y no distingue el mes que se debe.
- El Área B lee estudiantes, acudientes y profesores con sus propios DAO de consulta y nunca escribe en tablas del Área A.
