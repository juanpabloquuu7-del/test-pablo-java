# Área A: Estudiantes, Académica y Profesores

Módulos 1, 2 y 3 del menú principal.

## Estructura

Paquete base `com.littlehorizons.erp`, por capas:

| Capa | Clases del Área A |
|---|---|
| `model` | `Student`, `Guardian`, `GuardianLink`, `GradeLevel`, `Enrollment`, `Subject`, `SubjectEnrollment`, `Teacher`, `TeachingAssignment`, `ClassSchedule`, `AcademicRecord`, `AverageLine`, `Attendance`, `Names` |
| `dao` | Interfaz + `...DaoImpl` por entidad, y `StudentRegistrationDao` para el registro atómico |
| `service` | `StudentService`, `TeacherService`, `AcademicService` (+ reglas puras como `StudentRules`) |
| `view` | `StudentView`, `TeacherView`, `AcademicView` |
| `controller` | `StudentController` (posición 1), `AcademicController` (2), `TeacherController` (3) |

Registro en `AreaAModules.java`. Datos de prueba en `docs/sql/11-data-a.sql`, generado por `docs/sql/tools/gen_data_a.py`.

## Patrones

- **Singleton:** `AppConfig` y `DatabaseConnection` (núcleo).
- **DAO:** todo el SQL vive en `dao/`, siempre con `PreparedStatement` vía `BaseJdbcDao`.
- **MVC por capas:** controlador -> servicio -> DAO, por interfaz. La vista solo hace entrada y salida.
- **Inyección por constructor** en servicios y controladores; el ensamblado se hace en `AreaAModules`.

## Reglas de negocio destacadas

- Estudiante: documento único; edad coherente con el grado (referencia `nivel + 5` años al 31 de marzo, ±3); al menos un acudiente y un solo principal; una matrícula por año.
- El registro (estudiante, acudientes, vínculos y matrícula) es **una sola transacción**.
- Estados: Retirado cierra la matrícula con fecha; Graduado solo desde Once; Suspendido no permite inscribir ni calificar.
- Inscripción en materias: solo del grado del estudiante, períodos I a IV, sin duplicar.
- Profesor: documento y correo únicos; sin asignaciones duplicadas; **sin traslape de horarios** (lunes a sábado, 06:00 a 18:00).
- Notas de 0.0 a 5.0 con un decimal; estudiante inscrito y profesor con la materia asignada; una nota por estudiante, materia, año y período. La nota `Final` no entra en los promedios.
- Asistencia: una por estudiante, materia y fecha; alerta con más de `business.weeklyAbsenceThreshold` **días distintos** con ausencia en una semana (lunes a domingo).

## Errores y logs

`BaseController.safely` muestra al usuario las reglas de negocio violadas, y ante errores de base de datos o inesperados muestra un mensaje genérico y registra el detalle en `logs/error_log.txt` (archivo ignorado por git). Ningún mensaje incluye documentos, teléfonos ni datos médicos.

## Funcionalidades

1. **Estudiantes:** registrar, buscar, listar por grado, gestionar acudientes, matricular o cambiar de grado, cambiar estado, ficha completa, inscribir en materias.
2. **Académica:** registrar nota, historial, promedios (por materia, por período y del curso), asistencia del curso, reporte de inasistencias.
3. **Profesores:** registrar, listar, asignar materia, registrar horario, carga académica, cambiar estado.

## Limitaciones

No hay usuarios ni roles: quien use la consola ve las fichas completas, incluidos los datos médicos. Se recomienda un usuario MySQL dedicado en lugar de `root`.
