package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeachingAssignment;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TeacherServiceTest {

    @Nested
    @DisplayName("Reglas puras de traslape y horarios")
    class OverlapRuleTests {

        @Test
        @DisplayName("Traslape: clases que se cruzan parcialmente deben dar traslape")
        void testOverlapPartial() {
            LocalTime existingStart = LocalTime.of(8, 0);
            LocalTime existingEnd = LocalTime.of(10, 0);

            // Cruce por la derecha: 09:00 a 11:00
            assertTrue(TeacherService.overlaps(LocalTime.of(9, 0), LocalTime.of(11, 0), existingStart, existingEnd));
            // Cruce por la izquierda: 07:00 a 09:00
            assertTrue(TeacherService.overlaps(LocalTime.of(7, 0), LocalTime.of(9, 0), existingStart, existingEnd));
        }

        @Test
        @DisplayName("Traslape: clase contenida o envolvente")
        void testOverlapContainedAndEnclosing() {
            LocalTime existingStart = LocalTime.of(8, 0);
            LocalTime existingEnd = LocalTime.of(10, 0);

            // Contenida: 08:30 a 09:30
            assertTrue(TeacherService.overlaps(LocalTime.of(8, 30), LocalTime.of(9, 30), existingStart, existingEnd));
            // Envolvente: 07:00 a 11:00
            assertTrue(TeacherService.overlaps(LocalTime.of(7, 0), LocalTime.of(11, 0), existingStart, existingEnd));
            // Idéntica: 08:00 a 10:00
            assertTrue(TeacherService.overlaps(LocalTime.of(8, 0), LocalTime.of(10, 0), existingStart, existingEnd));
        }

        @Test
        @DisplayName("Traslape: clases consecutivas que tocan en el límite no se traslapan")
        void testNoOverlapTouchingBoundaries() {
            LocalTime existingStart = LocalTime.of(8, 0);
            LocalTime existingEnd = LocalTime.of(10, 0);

            // Termina exactamente cuando empieza la otra
            assertFalse(TeacherService.overlaps(LocalTime.of(7, 0), LocalTime.of(8, 0), existingStart, existingEnd));
            // Empieza exactamente cuando termina la otra
            assertFalse(TeacherService.overlaps(LocalTime.of(10, 0), LocalTime.of(11, 0), existingStart, existingEnd));
        }

        @Test
        @DisplayName("Traslape: clases completamente disyuntas")
        void testNoOverlapDisjoint() {
            LocalTime existingStart = LocalTime.of(8, 0);
            LocalTime existingEnd = LocalTime.of(10, 0);

            assertFalse(TeacherService.overlaps(LocalTime.of(6, 0), LocalTime.of(7, 0), existingStart, existingEnd));
            assertFalse(TeacherService.overlaps(LocalTime.of(11, 0), LocalTime.of(12, 0), existingStart, existingEnd));
        }

        @Test
        @DisplayName("Traslape: horarios invertidos o nulos deben retornar false")
        void testOverlapInvertedOrNull() {
            LocalTime s = LocalTime.of(8, 0);
            LocalTime e = LocalTime.of(10, 0);

            assertFalse(TeacherService.overlaps(null, e, s, e));
            assertFalse(TeacherService.overlaps(s, null, s, e));
            assertFalse(TeacherService.overlaps(s, e, null, e));
            assertFalse(TeacherService.overlaps(s, e, s, null));

            // Horario invertido (inicio posterior o igual a fin)
            assertFalse(TeacherService.overlaps(LocalTime.of(10, 0), LocalTime.of(8, 0), s, e));
            assertFalse(TeacherService.overlaps(LocalTime.of(8, 0), LocalTime.of(8, 0), s, e));
            assertFalse(TeacherService.overlaps(s, e, LocalTime.of(10, 0), LocalTime.of(8, 0)));
        }

        @Test
        @DisplayName("Conflicto con lista de horarios existentes")
        void testHasScheduleConflict() {
            List<ClassSchedule> schedules = List.of(
                    new ClassSchedule(1, 10, "Lunes", LocalTime.of(8, 0), LocalTime.of(10, 0)),
                    new ClassSchedule(2, 10, "Miércoles", LocalTime.of(10, 0), LocalTime.of(12, 0))
            );

            // Mismo día y se traslapa -> conflicto
            assertTrue(TeacherService.hasScheduleConflict(schedules, "Lunes", LocalTime.of(9, 0), LocalTime.of(11, 0)));
            // Mismo día sin traslape -> sin conflicto
            assertFalse(TeacherService.hasScheduleConflict(schedules, "Lunes", LocalTime.of(10, 0), LocalTime.of(12, 0)));
            // Mismo horario pero otro día -> sin conflicto
            assertFalse(TeacherService.hasScheduleConflict(schedules, "Martes", LocalTime.of(8, 0), LocalTime.of(10, 0)));
        }

        @Test
        @DisplayName("Conflicto con acentos: 'miercoles' vs 'Miércoles' y 'sabado' vs 'Sábado'")
        void testHasScheduleConflictAccentInsensitive() {
            List<ClassSchedule> schedules = List.of(
                    new ClassSchedule(1, 10, "Miércoles", LocalTime.of(8, 0), LocalTime.of(10, 0)),
                    new ClassSchedule(2, 10, "Sábado", LocalTime.of(14, 0), LocalTime.of(16, 0))
            );

            // Sin acento debe detectar conflicto con el horario que tiene acento
            assertTrue(TeacherService.hasScheduleConflict(schedules, "miercoles", LocalTime.of(9, 0), LocalTime.of(11, 0)));
            assertTrue(TeacherService.hasScheduleConflict(schedules, "sabado", LocalTime.of(15, 0), LocalTime.of(17, 0)));
        }

        @Test
        @DisplayName("Conflicto con nulos o lista vacía")
        void testHasScheduleConflictNulls() {
            assertFalse(TeacherService.hasScheduleConflict(null, "Lunes", LocalTime.of(8, 0), LocalTime.of(10, 0)));
            assertFalse(TeacherService.hasScheduleConflict(List.of(), "Lunes", LocalTime.of(8, 0), LocalTime.of(10, 0)));
            assertFalse(TeacherService.hasScheduleConflict(List.of(), null, LocalTime.of(8, 0), LocalTime.of(10, 0)));

            List<ClassSchedule> withNullDay = new ArrayList<>();
            withNullDay.add(new ClassSchedule(1, 10, null, LocalTime.of(8, 0), LocalTime.of(10, 0)));
            assertFalse(TeacherService.hasScheduleConflict(withNullDay, "Lunes", LocalTime.of(8, 0), LocalTime.of(10, 0)));
        }

        @Test
        @DisplayName("Validación de días y franjas horarias")
        void testValidDaysAndTimeWindows() {
            assertTrue(TeacherService.isValidDay("Lunes"));
            assertTrue(TeacherService.isValidDay("lunes"));
            assertTrue(TeacherService.isValidDay("Miércoles"));
            assertTrue(TeacherService.isValidDay("miercoles"));
            assertTrue(TeacherService.isValidDay("Sábado"));
            assertTrue(TeacherService.isValidDay("sabado"));
            assertFalse(TeacherService.isValidDay("Domingo"));
            assertFalse(TeacherService.isValidDay("Invalido"));
            assertFalse(TeacherService.isValidDay(null));
            assertFalse(TeacherService.isValidDay("   "));

            // Normalización
            assertEquals("Miércoles", TeacherService.normalizeDay("miercoles"));
            assertEquals("Miércoles", TeacherService.normalizeDay("Miércoles"));
            assertEquals("Sábado", TeacherService.normalizeDay("sabado"));
            assertEquals("Lunes", TeacherService.normalizeDay("lunes"));
            assertNull(TeacherService.normalizeDay(null));

            // Franjas horarias válidas: 06:00 a 18:00
            assertTrue(TeacherService.isValidTimeWindow(LocalTime.of(6, 0), LocalTime.of(18, 0)));
            assertTrue(TeacherService.isValidTimeWindow(LocalTime.of(8, 0), LocalTime.of(10, 0)));
            // Fuera de rango o fin antes que inicio
            assertFalse(TeacherService.isValidTimeWindow(LocalTime.of(5, 59), LocalTime.of(8, 0)));
            assertFalse(TeacherService.isValidTimeWindow(LocalTime.of(8, 0), LocalTime.of(18, 1)));
            assertFalse(TeacherService.isValidTimeWindow(LocalTime.of(10, 0), LocalTime.of(10, 0)));
            assertFalse(TeacherService.isValidTimeWindow(LocalTime.of(12, 0), LocalTime.of(10, 0)));
            assertFalse(TeacherService.isValidTimeWindow(null, LocalTime.of(10, 0)));
            assertFalse(TeacherService.isValidTimeWindow(LocalTime.of(10, 0), null));
        }
    }

    @Nested
    @DisplayName("Reglas de negocio con DAOs simulados")
    class ServiceBusinessRuleTests {

        private FakeTeacherDao teacherDao;
        private FakeAssignmentDao assignmentDao;
        private FakeSubjectDao subjectDao;
        private TeacherService teacherService;

        @BeforeEach
        void setUp() {
            teacherDao = new FakeTeacherDao();
            assignmentDao = new FakeAssignmentDao();
            subjectDao = new FakeSubjectDao();
            teacherService = new TeacherService(teacherDao, assignmentDao, subjectDao, 2026);
        }

        @Test
        @DisplayName("Registrar profesor: exitoso con datos válidos")
        void testRegisterTeacherSuccess() {
            Teacher t = new Teacher(0, "CC", "12345678", "Carlos", "Alberto", "Gomez", "Perez",
                    "carlos.gomez@colegio.edu.co", "3001234567", "Matemáticas", LocalDate.of(2025, 1, 15), "Activo");

            Teacher created = teacherService.registerTeacher(t);
            assertNotNull(created);
            assertEquals(1, created.id());
            assertEquals("Activo", created.status());
            assertEquals("Carlos", created.firstName());
        }

        @Test
        @DisplayName("Registrar profesor: campos opcionales con espacios se guardan como null")
        void testRegisterTeacherOptionalFieldsEmptyAsNull() {
            Teacher t = new Teacher(0, "CC", "12345678", "Carlos", "   ", "Gomez", "",
                    "carlos.gomez@colegio.edu.co", "   ", "  ", LocalDate.of(2025, 1, 15), null);

            Teacher created = teacherService.registerTeacher(t);
            assertNotNull(created);
            assertNull(created.middleName());
            assertNull(created.secondLastName());
            assertNull(created.phone());
            assertNull(created.specialization());
            assertEquals("Activo", created.status());
        }

        @Test
        @DisplayName("Registrar profesor: validaciones de entrada obligatorias y formatos")
        void testRegisterTeacherValidations() {
            // Null teacher
            assertThrows(BusinessException.class, () -> teacherService.registerTeacher(null));

            // Nombre vacío
            assertThrows(BusinessException.class, () -> teacherService.registerTeacher(
                    new Teacher(0, "CC", "12345678", "  ", null, "Gomez", null, "carlos@colegio.edu.co", null, null, LocalDate.now(), "Activo")));

            // Apellido vacío
            assertThrows(BusinessException.class, () -> teacherService.registerTeacher(
                    new Teacher(0, "CC", "12345678", "Carlos", null, "", null, "carlos@colegio.edu.co", null, null, LocalDate.now(), "Activo")));

            // Documento inválido (letras/números < 5 o > 20 o caracteres raros)
            assertThrows(BusinessException.class, () -> teacherService.registerTeacher(
                    new Teacher(0, "CC", "123", "Carlos", null, "Gomez", null, "carlos@colegio.edu.co", null, null, LocalDate.now(), "Activo")));
            assertThrows(BusinessException.class, () -> teacherService.registerTeacher(
                    new Teacher(0, "CC", "12345-678", "Carlos", null, "Gomez", null, "carlos@colegio.edu.co", null, null, LocalDate.now(), "Activo")));

            // Correo inválido
            assertThrows(BusinessException.class, () -> teacherService.registerTeacher(
                    new Teacher(0, "CC", "12345678", "Carlos", null, "Gomez", null, "sin-arroba", null, null, LocalDate.now(), "Activo")));

            // Teléfono inválido
            assertThrows(BusinessException.class, () -> teacherService.registerTeacher(
                    new Teacher(0, "CC", "12345678", "Carlos", null, "Gomez", null, "carlos@colegio.edu.co", "abc", null, LocalDate.now(), "Activo")));

            // Tipo de documento inválido
            assertThrows(BusinessException.class, () -> teacherService.registerTeacher(
                    new Teacher(0, "TI", "12345678", "Carlos", null, "Gomez", null, "carlos@colegio.edu.co", null, null, LocalDate.now(), "Activo")));

            // Fecha de contratación nula
            assertThrows(BusinessException.class, () -> teacherService.registerTeacher(
                    new Teacher(0, "CC", "12345678", "Carlos", null, "Gomez", null, "carlos@colegio.edu.co", null, null, null, "Activo")));
        }

        @Test
        @DisplayName("Registrar profesor: documento o correo duplicado es rechazado sin exponer datos sensibles")
        void testRegisterTeacherDuplicateDocOrEmail() {
            Teacher t1 = new Teacher(0, "CC", "12345678", "Carlos", null, "Gomez", null,
                    "carlos.gomez@colegio.edu.co", null, "Matemáticas", LocalDate.of(2025, 1, 15), "Activo");
            teacherService.registerTeacher(t1);

            // Mismo documento, distinto correo
            Teacher t2 = new Teacher(0, "CC", "12345678", "Otro", null, "Nombre", null,
                    "otro.correo@colegio.edu.co", null, "Física", LocalDate.of(2025, 1, 15), "Activo");
            BusinessException exDoc = assertThrows(BusinessException.class, () -> teacherService.registerTeacher(t2));
            assertFalse(exDoc.getMessage().contains("12345678"), "El mensaje no debe contener el documento");

            // Mismo correo, distinto documento
            Teacher t3 = new Teacher(0, "CC", "87654321", "Otro", null, "Nombre", null,
                    "carlos.gomez@colegio.edu.co", null, "Física", LocalDate.of(2025, 1, 15), "Activo");
            BusinessException exEmail = assertThrows(BusinessException.class, () -> teacherService.registerTeacher(t3));
            assertTrue(exEmail.getMessage().contains("correo"));
        }

        @Test
        @DisplayName("Asignación: solo profesores Activos reciben asignaciones")
        void testAssignSubjectOnlyActiveTeachers() {
            Teacher active = teacherService.registerTeacher(new Teacher(0, "CC", "11111111", "Juan", null, "Perez", null,
                    "juan@colegio.edu.co", null, "Ciencias", LocalDate.of(2025, 1, 1), "Activo"));
            Teacher inactive = teacherService.registerTeacher(new Teacher(0, "CC", "22222222", "Pedro", null, "Lopez", null,
                    "pedro@colegio.edu.co", null, "Artes", LocalDate.of(2025, 1, 1), "Inactivo"));
            subjectDao.insert(new Subject(1, "CIEN-6", "Ciencias Naturales", "Desc", 7, 4, true));

            // Profesor activo -> asigna OK
            TeachingAssignment a = teacherService.assignSubject(active.id(), 1, "A");
            assertNotNull(a);
            assertEquals("A", a.section());

            // Profesor inactivo -> BusinessException
            BusinessException ex = assertThrows(BusinessException.class, () -> teacherService.assignSubject(inactive.id(), 1, "B"));
            assertTrue(ex.getMessage().contains("Activo"));
        }

        @Test
        @DisplayName("Asignación: sección obligatoria (default 'A') y no duplicar por profesor, materia, año y sección")
        void testAssignSubjectDefaultSectionAndNoDuplicates() {
            Teacher teacher = teacherService.registerTeacher(new Teacher(0, "CC", "11111111", "Juan", null, "Perez", null,
                    "juan@colegio.edu.co", null, "Ciencias", LocalDate.of(2025, 1, 1), "Activo"));
            subjectDao.insert(new Subject(1, "CIEN-6", "Ciencias Naturales", "Desc", 7, 4, true));

            // Sin sección especificada toma "A"
            TeachingAssignment a1 = teacherService.assignSubject(teacher.id(), 1, null);
            assertEquals("A", a1.section());

            // Duplicar sección "A" para el mismo año falla
            BusinessException ex = assertThrows(BusinessException.class, () -> teacherService.assignSubject(teacher.id(), 1, "A"));
            assertTrue(ex.getMessage().contains("ya tiene asignada"));

            // Otra sección sí es permitida
            TeachingAssignment a2 = teacherService.assignSubject(teacher.id(), 1, "B");
            assertEquals("B", a2.section());
        }

        @Test
        @DisplayName("Asignación: valida IDs inexistentes, materia inactiva, sección > 10 caracteres y año <= 0")
        void testAssignSubjectEdgeCases() {
            Teacher teacher = teacherService.registerTeacher(new Teacher(0, "CC", "11111111", "Juan", null, "Perez", null,
                    "juan@colegio.edu.co", null, "Ciencias", LocalDate.of(2025, 1, 1), "Activo"));
            subjectDao.insert(new Subject(1, "CIEN-6", "Ciencias", "Desc", 7, 4, true));
            subjectDao.insert(new Subject(2, "INAC-6", "Inactiva", "Desc", 7, 2, false));

            // Profesor inexistente o negativo
            assertThrows(BusinessException.class, () -> teacherService.assignSubject(999, 1, "A"));
            assertThrows(BusinessException.class, () -> teacherService.assignSubject(-1, 1, "A"));

            // Materia inexistente o negativa
            assertThrows(BusinessException.class, () -> teacherService.assignSubject(teacher.id(), 999, "A"));
            assertThrows(BusinessException.class, () -> teacherService.assignSubject(teacher.id(), -5, "A"));

            // Materia inactiva
            BusinessException exInactive = assertThrows(BusinessException.class, () -> teacherService.assignSubject(teacher.id(), 2, "A"));
            assertTrue(exInactive.getMessage().contains("activa"));

            // Sección > 10 caracteres
            assertThrows(BusinessException.class, () -> teacherService.assignSubject(teacher.id(), 1, "SECCION_DEMASIADO_LARGA"));

            // Año <= 0
            assertThrows(BusinessException.class, () -> teacherService.assignSubject(teacher.id(), 1, "A", -2026));
            assertThrows(BusinessException.class, () -> teacherService.assignSubject(teacher.id(), 1, "A", 0));
        }

        @Test
        @DisplayName("Asignación con notas registradas no se puede eliminar")
        void testCannotDeleteAssignmentWithGrades() {
            Teacher teacher = teacherService.registerTeacher(new Teacher(0, "CC", "11111111", "Juan", null, "Perez", null,
                    "juan@colegio.edu.co", null, "Ciencias", LocalDate.of(2025, 1, 1), "Activo"));
            subjectDao.insert(new Subject(1, "CIEN-6", "Ciencias Naturales", "Desc", 7, 4, true));
            TeachingAssignment a = teacherService.assignSubject(teacher.id(), 1, "A");

            // Sin notas se elimina sin problemas
            teacherService.deleteAssignment(a.id());

            // Nueva asignación simulando que tiene notas
            TeachingAssignment a2 = teacherService.assignSubject(teacher.id(), 1, "A");
            assignmentDao.setHasGrades(a2.id(), true);

            BusinessException ex = assertThrows(BusinessException.class, () -> teacherService.deleteAssignment(a2.id()));
            assertTrue(ex.getMessage().contains("notas registradas"));

            // Asignación inexistente o negativa
            assertThrows(BusinessException.class, () -> teacherService.deleteAssignment(999));
            assertThrows(BusinessException.class, () -> teacherService.deleteAssignment(-1));
        }

        @Test
        @DisplayName("Horarios: rechaza traslape en misma asignación y entre materias distintas del mismo profesor")
        void testScheduleOverlapRejectionAcrossDifferentSubjects() {
            Teacher teacher = teacherService.registerTeacher(new Teacher(0, "CC", "11111111", "Juan", null, "Perez", null,
                    "juan@colegio.edu.co", null, "Ciencias", LocalDate.of(2025, 1, 1), "Activo"));
            subjectDao.insert(new Subject(1, "CIEN-6", "Ciencias", "Desc", 7, 4, true));
            subjectDao.insert(new Subject(2, "BIOL-6", "Biología", "Desc", 7, 3, true));

            TeachingAssignment asigCiencias = teacherService.assignSubject(teacher.id(), 1, "A");
            TeachingAssignment asigBiologia = teacherService.assignSubject(teacher.id(), 2, "A");

            // Horario para Ciencias: Lunes 08:00 a 10:00
            ClassSchedule h1 = teacherService.registerSchedule(asigCiencias.id(), "Lunes", LocalTime.of(8, 0), LocalTime.of(10, 0));
            assertNotNull(h1);

            // Horario para Ciencias el mismo Lunes 09:00 a 11:00 -> traslape en misma asignación
            assertThrows(BusinessException.class, () ->
                    teacherService.registerSchedule(asigCiencias.id(), "Lunes", LocalTime.of(9, 0), LocalTime.of(11, 0))
            );

            // Horario para Biología (OTRA MATERIA) el mismo Lunes 09:30 a 11:30 -> traslape detectado!
            BusinessException exAcrossSubjects = assertThrows(BusinessException.class, () ->
                    teacherService.registerSchedule(asigBiologia.id(), "Lunes", LocalTime.of(9, 30), LocalTime.of(11, 30))
            );
            assertTrue(exAcrossSubjects.getMessage().contains("Conflicto de horario"));

            // Horario para Biología inmediatamente después (10:00 a 12:00) -> sin conflicto
            ClassSchedule h2 = teacherService.registerSchedule(asigBiologia.id(), "Lunes", LocalTime.of(10, 0), LocalTime.of(12, 0));
            assertNotNull(h2);
        }

        @Test
        @DisplayName("Horarios: rechaza días u horas fuera del rango válido y asignaciones inexistentes")
        void testScheduleInvalidDayOrTime() {
            Teacher teacher = teacherService.registerTeacher(new Teacher(0, "CC", "11111111", "Juan", null, "Perez", null,
                    "juan@colegio.edu.co", null, "Ciencias", LocalDate.of(2025, 1, 1), "Activo"));
            subjectDao.insert(new Subject(1, "CIEN-6", "Ciencias", "Desc", 7, 4, true));
            TeachingAssignment asig = teacherService.assignSubject(teacher.id(), 1, "A");

            // Asignación inexistente o negativa
            assertThrows(BusinessException.class, () ->
                    teacherService.registerSchedule(999, "Lunes", LocalTime.of(8, 0), LocalTime.of(10, 0)));
            assertThrows(BusinessException.class, () ->
                    teacherService.registerSchedule(-5, "Lunes", LocalTime.of(8, 0), LocalTime.of(10, 0)));

            // Domingo
            assertThrows(BusinessException.class, () ->
                    teacherService.registerSchedule(asig.id(), "Domingo", LocalTime.of(8, 0), LocalTime.of(10, 0)));

            // Antes de las 06:00
            assertThrows(BusinessException.class, () ->
                    teacherService.registerSchedule(asig.id(), "Lunes", LocalTime.of(5, 30), LocalTime.of(7, 30)));

            // Después de las 18:00
            assertThrows(BusinessException.class, () ->
                    teacherService.registerSchedule(asig.id(), "Lunes", LocalTime.of(17, 0), LocalTime.of(18, 30)));

            // Fin anterior a inicio
            assertThrows(BusinessException.class, () ->
                    teacherService.registerSchedule(asig.id(), "Lunes", LocalTime.of(10, 0), LocalTime.of(9, 0)));
        }

        @Test
        @DisplayName("Cambiar estado: valida estados permitidos y actualiza")
        void testChangeTeacherStatus() {
            Teacher teacher = teacherService.registerTeacher(new Teacher(0, "CC", "11111111", "Juan", null, "Perez", null,
                    "juan@colegio.edu.co", null, "Ciencias", LocalDate.of(2025, 1, 1), "Activo"));

            teacherService.changeTeacherStatus(teacher.id(), "Licencia");
            assertEquals("Licencia", teacherDao.findById(teacher.id()).orElseThrow().status());

            teacherService.changeTeacherStatus(teacher.id(), "Inactivo");
            assertEquals("Inactivo", teacherDao.findById(teacher.id()).orElseThrow().status());

            assertThrows(BusinessException.class, () -> teacherService.changeTeacherStatus(teacher.id(), "Retirado"));
            assertThrows(BusinessException.class, () -> teacherService.changeTeacherStatus(teacher.id(), null));
            assertThrows(BusinessException.class, () -> teacherService.changeTeacherStatus(999, "Activo"));
            assertThrows(BusinessException.class, () -> teacherService.changeTeacherStatus(-1, "Activo"));
        }

        @Test
        @DisplayName("Carga académica: lista materias asignadas y horarios")
        void testGetTeacherWorkload() {
            Teacher teacher = teacherService.registerTeacher(new Teacher(0, "CC", "11111111", "Juan", null, "Perez", null,
                    "juan@colegio.edu.co", null, "Ciencias", LocalDate.of(2025, 1, 1), "Activo"));
            subjectDao.insert(new Subject(1, "CIEN-6", "Ciencias", "Desc", 7, 4, true));

            // Sin materias asignadas
            List<TeacherService.TeacherAssignmentDetail> emptyWorkload = teacherService.getTeacherWorkload(teacher.id());
            assertTrue(emptyWorkload.isEmpty());

            // Asignar materia y horario
            TeachingAssignment asig = teacherService.assignSubject(teacher.id(), 1, "A");
            teacherService.registerSchedule(asig.id(), "Lunes", LocalTime.of(8, 0), LocalTime.of(10, 0));

            List<TeacherService.TeacherAssignmentDetail> workload = teacherService.getTeacherWorkload(teacher.id());
            assertEquals(1, workload.size());
            assertEquals("Ciencias", workload.get(0).subject().name());
            assertEquals(1, workload.get(0).schedules().size());
            assertEquals("Lunes", workload.get(0).schedules().get(0).day());

            // Profesor inexistente o ID negativo
            assertThrows(BusinessException.class, () -> teacherService.getTeacherWorkload(999));
            assertThrows(BusinessException.class, () -> teacherService.getTeacherWorkload(-1));
            assertThrows(BusinessException.class, () -> teacherService.getTeacherWorkload(teacher.id(), 0));
        }

        @Test
        @DisplayName("Consultas por ID: Optional vacío ante ID negativo o inexistente")
        void testFindByIdNegativeOrNonexistent() {
            assertTrue(teacherService.findTeacherById(-1).isEmpty());
            assertTrue(teacherService.findTeacherById(0).isEmpty());
            assertTrue(teacherService.findTeacherById(999).isEmpty());

            assertTrue(teacherService.findAssignmentById(-1).isEmpty());
            assertTrue(teacherService.findAssignmentById(0).isEmpty());
            assertTrue(teacherService.findAssignmentById(999).isEmpty());
        }

        @Test
        @DisplayName("Listar activos y todos")
        void testListActiveAndAll() {
            teacherService.registerTeacher(new Teacher(0, "CC", "11111111", "Juan", null, "Perez", null,
                    "juan@colegio.edu.co", null, "Ciencias", LocalDate.of(2025, 1, 1), "Activo"));
            Teacher inact = teacherService.registerTeacher(new Teacher(0, "CC", "22222222", "Pedro", null, "Lopez", null,
                    "pedro@colegio.edu.co", null, "Artes", LocalDate.of(2025, 1, 1), "Inactivo"));

            assertEquals(1, teacherService.listActiveTeachers().size());
            assertEquals(2, teacherService.listAllTeachers().size());
        }
    }

    // -------------------------------------------------------------
    // Fakes en memoria para pruebas sin base de datos
    // -------------------------------------------------------------

    private static class FakeTeacherDao implements TeacherDao {
        private final Map<Integer, Teacher> teachers = new HashMap<>();
        private int sequence = 1;

        @Override
        public int insert(Teacher t) {
            int id = sequence++;
            teachers.put(id, new Teacher(id, t.documentType(), t.documentNumber(), t.firstName(), t.middleName(),
                    t.lastName(), t.secondLastName(), t.email(), t.phone(), t.specialization(), t.hireDate(), t.status()));
            return id;
        }

        @Override
        public int update(Teacher teacher) {
            teachers.put(teacher.id(), teacher);
            return 1;
        }

        @Override
        public Optional<Teacher> findById(int id) {
            return Optional.ofNullable(teachers.get(id));
        }

        @Override
        public Optional<Teacher> findByDocument(String documentNumber) {
            return teachers.values().stream()
                    .filter(t -> t.documentNumber().equals(documentNumber))
                    .findFirst();
        }

        @Override
        public Optional<Teacher> findByEmail(String email) {
            return teachers.values().stream()
                    .filter(t -> t.email().equalsIgnoreCase(email))
                    .findFirst();
        }

        @Override
        public List<Teacher> findAllActive() {
            return teachers.values().stream()
                    .filter(t -> "Activo".equals(t.status()))
                    .toList();
        }

        @Override
        public List<Teacher> findAll() {
            return new ArrayList<>(teachers.values());
        }

        @Override
        public int updateStatus(int teacherId, String status) {
            Teacher t = teachers.get(teacherId);
            if (t != null) {
                teachers.put(teacherId, new Teacher(t.id(), t.documentType(), t.documentNumber(), t.firstName(),
                        t.middleName(), t.lastName(), t.secondLastName(), t.email(), t.phone(), t.specialization(),
                        t.hireDate(), status));
                return 1;
            }
            return 0;
        }
    }

    private static class FakeSubjectDao implements SubjectDao {
        private final Map<Integer, Subject> subjects = new HashMap<>();
        private int sequence = 1;

        @Override
        public int insert(Subject subject) {
            int id = subject.id() > 0 ? subject.id() : sequence++;
            subjects.put(id, new Subject(id, subject.code(), subject.name(), subject.description(),
                    subject.gradeId(), subject.weeklyHours(), subject.active()));
            return id;
        }

        @Override
        public Optional<Subject> findById(int id) {
            return Optional.ofNullable(subjects.get(id));
        }

        @Override
        public List<Subject> findByGrade(int gradeId) {
            return subjects.values().stream().filter(s -> s.gradeId() == gradeId).toList();
        }

        @Override
        public List<Subject> findActive() {
            return subjects.values().stream().filter(Subject::active).toList();
        }
    }

    private static class FakeAssignmentDao implements AssignmentDao {
        private final Map<Integer, TeachingAssignment> assignments = new HashMap<>();
        private final Map<Integer, ClassSchedule> schedules = new HashMap<>();
        private final Map<Integer, Boolean> gradesMap = new HashMap<>();
        private int assignmentSeq = 1;
        private int scheduleSeq = 1;

        public void setHasGrades(int assignmentId, boolean has) {
            gradesMap.put(assignmentId, has);
        }

        @Override
        public int insert(TeachingAssignment a) {
            int id = assignmentSeq++;
            assignments.put(id, new TeachingAssignment(id, a.teacherId(), a.subjectId(), a.year(), a.section()));
            return id;
        }

        @Override
        public Optional<TeachingAssignment> findById(int id) {
            return Optional.ofNullable(assignments.get(id));
        }

        @Override
        public List<TeachingAssignment> findByTeacher(int teacherId, int year) {
            return assignments.values().stream()
                    .filter(a -> a.teacherId() == teacherId && a.year() == year)
                    .toList();
        }

        @Override
        public List<TeachingAssignment> findBySubject(int subjectId, int year) {
            return assignments.values().stream()
                    .filter(a -> a.subjectId() == subjectId && a.year() == year)
                    .toList();
        }

        @Override
        public boolean exists(int teacherId, int subjectId, int year, String section) {
            return assignments.values().stream().anyMatch(a ->
                    a.teacherId() == teacherId && a.subjectId() == subjectId && a.year() == year && a.section().equalsIgnoreCase(section));
        }

        @Override
        public boolean teacherHasSubject(int teacherId, int subjectId, int year) {
            return assignments.values().stream().anyMatch(a ->
                    a.teacherId() == teacherId && a.subjectId() == subjectId && a.year() == year);
        }

        @Override
        public boolean hasGrades(int assignmentId) {
            return gradesMap.getOrDefault(assignmentId, false);
        }

        @Override
        public int delete(int assignmentId) {
            assignments.remove(assignmentId);
            schedules.entrySet().removeIf(e -> e.getValue().assignmentId() == assignmentId);
            return 1;
        }

        @Override
        public int insertSchedule(ClassSchedule s) {
            int id = scheduleSeq++;
            schedules.put(id, new ClassSchedule(id, s.assignmentId(), s.day(), s.startTime(), s.endTime()));
            return id;
        }

        @Override
        public List<ClassSchedule> findSchedulesByAssignment(int assignmentId) {
            return schedules.values().stream()
                    .filter(s -> s.assignmentId() == assignmentId)
                    .toList();
        }

        @Override
        public List<ClassSchedule> findSchedulesByTeacher(int teacherId, int year) {
            List<Integer> teacherAssignmentIds = assignments.values().stream()
                    .filter(a -> a.teacherId() == teacherId && a.year() == year)
                    .map(TeachingAssignment::id)
                    .toList();
            return schedules.values().stream()
                    .filter(s -> teacherAssignmentIds.contains(s.assignmentId()))
                    .toList();
        }

        @Override
        public int deleteSchedule(int scheduleId) {
            schedules.remove(scheduleId);
            return 1;
        }
    }
}
