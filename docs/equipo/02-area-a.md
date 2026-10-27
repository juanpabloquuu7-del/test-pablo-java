# Área A: Dev A (rama `dev-a`)

Módulos del menú: **1 Estudiantes, 2 Académica, 3 Profesores**. Registro en `AreaAModules.java`. Léase primero [01-nucleo-compartido.md](01-nucleo-compartido.md).

## Antes de empezar

- [ ] Tener el entorno del núcleo funcionando (menú principal visible).
- [ ] `git checkout dev-a` y `git pull --rebase origin develop`.
- [ ] Cargar `01-schema.sql` y `02-data-base.sql` en MySQL.

## Qué es tuyo

Clases (modelo, `XxxDao`, `XxxDaoImpl`, `XxxService`, `XxxController`, `XxxView` según corresponda):
`Student`, `Guardian`, `GradeLevel`, `Enrollment`, `Subject`, `SubjectEnrollment`, `AcademicRecord`, `Attendance`, `AverageLine`, `Teacher`, `TeachingAssignment`, `ClassSchedule`, `Names`.
Archivos: `AreaAModules.java`, `docs/sql/10-extras-a.sql`, `docs/sql/11-data-a.sql`, y la sección "Área A" de `config.properties`.
Tablas donde escribes: ver [00-reglas-del-equipo.md](00-reglas-del-equipo.md).

## Qué NO haces

No importas clases del Área B. No creas clases que terminen en `Ref` ni `Lookup*`. No modificas archivos del núcleo ni de B.

## Orden de trabajo (el orden interno evita bloqueos entre tus propios módulos)

### Paso 1: Estudiantes y matrícula (módulo 1)

Clases: `Student`, `Guardian`, `GradeLevel`, `Enrollment`, `Subject`, `SubjectEnrollment` y sus DAO, `StudentService`, `StudentController`, `StudentView`.
- [ ] Registro completo de estudiantes (documento, nombres, fecha de nacimiento, dirección, contactos). Documento único, edad coherente con el grado.
- [ ] Acudientes: al menos uno por estudiante; un acudiente con varios estudiantes (hermanos); marcar principal y contacto de emergencia.
- [ ] Matrícula por grado y año académico; estados Activo, Graduado, Retirado; cambio de grado y de estado.
- [ ] Inscripción en materias por período, solo materias del grado del estudiante, sin duplicar.
- [ ] Consultas: buscar por documento o nombre, listar por grado, ver ficha completa con sus acudientes.

### Paso 2: Profesores y horarios (módulo 3)

Clases: `Teacher`, `TeachingAssignment`, `ClassSchedule` y sus DAO, `TeacherService`, `TeacherController`, `TeacherView`.
- [ ] Directorio de profesores (documento, correo único, especialización, estado).
- [ ] Asignación de materias: un profesor varias materias y una materia varios profesores (por sección y año).
- [ ] Horarios de clase (día, hora de inicio y fin) con detección de conflictos: un profesor no puede estar en dos clases a la misma hora.
- [ ] Carga académica: listar materias, secciones y horas por profesor.

### Paso 3: Académica (módulo 2)

Clases: `AcademicRecord`, `Attendance`, `AverageLine` y sus DAO, `AcademicService`, `AcademicController`, `AcademicView`.
- [ ] Ingreso de notas 0.0 a 5.0 con un decimal, períodos I, II, III, IV y Final. Valida que el estudiante esté inscrito y que el profesor tenga asignada la materia. Una sola nota por estudiante, materia, año y período.
- [ ] Promedios automáticos: por materia de cada estudiante, general del estudiante en el período, y del curso por materia.
- [ ] Historial académico completo de un estudiante desde su ingreso.
- [ ] Asistencia diaria por materia (Presente, Ausente, Tardanza, Excusa Médica), una por estudiante, materia y fecha.
- [ ] Reporte de inasistencias: estudiantes con más de 3 ausencias en una semana (umbral en `business.weeklyAbsenceThreshold`) y total de inasistencias acumuladas.

### Paso 4: Datos de prueba y cierre del área

- [ ] `docs/sql/10-extras-a.sql`: cualquier cambio de esquema que necesites, con justificación (puede quedar vacío).
- [ ] `docs/sql/11-data-a.sql`: 60 materias (5 por grado), asignaciones y horarios para los 12 profesores, inscripciones, 300 o más registros académicos y asistencias de varias semanas. Usa los IDs de `02-data-base.sql`.
- [ ] Registrar los tres módulos en `AreaAModules.java` (posiciones 1, 2 y 3).
- [ ] `docs/readme-a.md`: secciones para el README final: estructura del proyecto, patrones aplicados (Singleton, DAO, MVC, inyección por constructor), manejo de errores y logs, y funcionalidades de los módulos 1 a 3.

## Cómo entregar

Cada módulo terminado: PR `dev-a` -> `develop` (puede ser uno por paso). Criterio de terminado en [00-reglas-del-equipo.md](00-reglas-del-equipo.md).
