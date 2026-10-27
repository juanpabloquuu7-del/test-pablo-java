# Área B: Dev B (rama `dev-b`)

Módulos del menú: **4 Extracurriculares, 5 Financiera, 6 Recursos, 7 Reportes**. Registro en `AreaBModules.java`. Léase primero [01-nucleo-compartido.md](01-nucleo-compartido.md).

## Antes de empezar

- [ ] Tener el entorno del núcleo funcionando (menú principal visible).
- [ ] `git checkout dev-b` y `git pull --rebase origin develop`.
- [ ] Cargar `01-schema.sql` y `02-data-base.sql` en MySQL.

## Qué es tuyo

Clases (modelo, `XxxDao`, `XxxDaoImpl`, `XxxService`, `XxxController`, `XxxView` según corresponda):
`Club`, `ClubMembership`, `SchoolEvent`, `EventParticipant`, `PaymentConcept`, `Payment`, `Invoice`, `Scholarship`, `ResourceCategory`, `Resource`, `Loan`, reportes (`ReportDao`, `ReportService`, `ReportController`, `ReportView`), y `util/pdf/PdfGenerator`.
Archivos: `AreaBModules.java`, `docs/sql/20-extras-b.sql`, `docs/sql/21-data-b.sql`, y la sección "Área B" de `config.properties`.
Tablas donde escribes: ver [00-reglas-del-equipo.md](00-reglas-del-equipo.md).

## Datos del Área A: solo lectura y con tus propias clases

Necesitas leer estudiantes, acudientes y profesores. No uses clases del Área A: crea las tuyas.
- Modelos: `StudentRef(id, document, fullName, active)`, `GuardianRef(id, document, fullName, address)`, `TeacherRef(id, fullName)`.
- DAOs: `StudentLookupDao`, `GuardianLookupDao`, `TeacherLookupDao` con sus `Impl` (solo `SELECT`). Métodos típicos: `findById`, `findByDocument`, `findPrincipalGuardian(idStudent)`, `countSiblings(idStudent)` (otros estudiantes que comparten acudiente), `findGradeName(idStudent, year)`.
- IDs de prueba que puedes asumir: estudiantes 1 a 48, acudientes 1 a 36, profesores 1 a 12.

## Qué NO haces

No importas clases del Área A. No escribes en tablas de A. No modificas archivos del núcleo ni de A.

## Orden de trabajo

### Paso 1: Recursos y préstamos (módulo 6)

Clases: `ResourceCategory`, `Resource`, `Loan`, DAO, `ResourceService`, `ResourceController`, `ResourceView`.
- [ ] Inventario: código único, nombre, categoría, estado (Disponible, Prestado, En Mantenimiento, Dado de Baja) y ubicación. Alta, edición y cambio de estado.
- [ ] Préstamo a estudiante o profesor (tablas `prestamos_estudiante` y `prestamos_profesor`): solo si el recurso está Disponible. Fecha de devolución = hoy + `business.loanDays`. El recurso pasa a Prestado en la misma transacción.
- [ ] Devolución: fecha real, préstamo Devuelto y recurso Disponible, en una transacción.
- [ ] Reporte de préstamos vencidos (no devueltos después de la fecha esperada): marcarlos Vencido y listarlos con quién los tiene.

### Paso 2: Extracurriculares (módulo 4)

Clases: `Club`, `ClubMembership`, `SchoolEvent`, `EventParticipant`, DAO, `ExtracurricularService`, `ExtracurricularController`, `ExtracurricularView`.
- [ ] Catálogo de clubes: nombre, descripción, cupo máximo, profesor encargado (validado con `TeacherLookupDao`).
- [ ] Inscripción de estudiantes: no se excede el cupo (la validación y el insert deben ser atómicos), un estudiante no se inscribe dos veces en el mismo club, puede estar en varios.
- [ ] Retiro con fecha y motivo; estado Activa o Retirada.
- [ ] Eventos: nombre, fecha, descripción, tipo (Académico, Cultural, Deportivo, Social, Científico), lugar, competencia sí o no.
- [ ] Participantes por evento; en eventos con competencia, posición (1er Lugar, 2do Lugar, 3er Lugar, Participante).

### Paso 3: Financiera y factura (módulo 5)

Clases: `PaymentConcept`, `Payment`, `Invoice`, `Scholarship`, DAO, `FinanceService`, `FinanceController`, `FinanceView`, `util/pdf/PdfGenerator`.
- [ ] Catálogo de conceptos con valor base (alta, edición, activar y desactivar).
- [ ] Valor personalizado por estudiante: descuento por hermanos (`business.siblingDiscountPercent` en conceptos recurrentes) y becas parciales (`becas`). Se aplica el mayor de los dos descuentos, no se suman; el monto final puede ajustarse manualmente.
- [ ] Registro de pagos: estudiante, acudiente, concepto, monto, fecha, método (Efectivo, Transferencia, Tarjeta Débito, Tarjeta Crédito, Cheque) y número de recibo consecutivo.
- [ ] Historial de pagos por estudiante y por acudiente (todos sus hijos).
- [ ] Reporte de morosos: estudiantes con la pensión pendiente por más de un mes (`business.delinquencyConcept` y `delinquencyDays`).
- [ ] **Factura electrónica PDF** al confirmar el pago (PDFBox + ZXing, ya en el `pom.xml`):
  - Datos del colegio desde `school.*` (razón social "Colegio Internacional Little Horizons S.A.S.", NIT, dirección, teléfono, correo, resolución simulada).
  - Cliente: acudiente responsable (nombre, documento, dirección).
  - Número consecutivo, fecha de emisión, ítems (descripción, cantidad, valor unitario, subtotal), subtotal, IVA si aplica, total.
  - CUFE simulado = SHA-256(número de factura + NIT + fecha) y código QR con los datos de la factura.
  - Guardar en `paths.invoicesDir` como `factura_000123.pdf`, registrar `numero_factura`, `cufe`, `nombre_archivo_pdf` y `ruta_archivo` en `facturas`, y mostrar la ruta al usuario.

### Paso 4: Reportes y estadísticas (módulo 7)

Clases: `ReportDao`, `ReportService`, `ReportController`, `ReportView`. Solo `SELECT` sobre cualquier tabla.
- [ ] Estudiantes por grado.
- [ ] Mejores promedios (lee `registros_academicos`).
- [ ] Participación en clubes y en eventos.
- [ ] Ingresos por concepto y por mes.
- [ ] Estado de los recursos y préstamos vencidos.

### Paso 5: Datos de prueba y cierre del área

- [ ] `docs/sql/20-extras-b.sql`: cambios de esquema que necesites, con justificación (puede quedar vacío).
- [ ] `docs/sql/21-data-b.sql`: 8 clubes, membresías, 6 eventos con participantes y posiciones, 150 o más pagos con facturas, becas, 40 o más recursos y préstamos (algunos vencidos). Usa los IDs de `02-data-base.sql`.
- [ ] Registrar los cuatro módulos en `AreaBModules.java` (posiciones 4, 5, 6 y 7).
- [ ] `docs/readme-b.md`: secciones para el README final: tecnologías, requisitos, configuración de la base de datos, instalación y ejecución, funcionalidades de los módulos 4 a 7, factura electrónica y limitaciones.

## Cómo entregar

Cada módulo terminado: PR `dev-b` -> `develop` (puede ser uno por paso). Criterio de terminado en [00-reglas-del-equipo.md](00-reglas-del-equipo.md).
