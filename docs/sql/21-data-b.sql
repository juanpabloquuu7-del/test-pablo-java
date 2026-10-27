-- Datos de prueba del Área B (Dev B): clubes, membresías, eventos, pagos, facturas, becas, recursos, préstamos. Puede asumir los IDs de 02-data-base.sql (estudiantes 1..48, acudientes 1..36, profesores 1..12).
-- Las fechas de préstamos y pagos son relativas al día de la carga (CURDATE()), así siempre hay vencidos y morosos.
-- Cargar UNA sola vez, después de 01-schema.sql y 02-data-base.sql (el código de recurso es único).
USE campuslands;

-- ============================================
-- PASO 1: RECURSOS Y PRÉSTAMOS (módulo 6)
-- Categorías: 1 Libro, 2 Equipo de Laboratorio, 3 Equipo Deportivo, 4 Dispositivo Tecnológico, 5 Material Didáctico
-- ============================================

INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, descripcion, fecha_adquisicion, ubicacion_fisica, estado_recurso) VALUES
('LIB-001', 'Cien años de soledad', 1, 'Novela de Gabriel García Márquez', '2022-02-10', 'Biblioteca', 'Prestado'),
('LIB-002', 'Don Quijote de la Mancha', 1, 'Edición escolar ilustrada', '2021-08-15', 'Biblioteca', 'Prestado'),
('LIB-003', 'Álgebra de Baldor', 1, 'Texto de álgebra para bachillerato', '2020-01-20', 'Biblioteca', 'Prestado'),
('LIB-004', 'Atlas Universal', 1, 'Atlas geográfico actualizado', '2023-03-05', 'Biblioteca', 'Disponible'),
('LIB-005', 'Diccionario de la Lengua Española', 1, 'Real Academia Española', '2022-06-18', 'Biblioteca', 'Disponible'),
('LIB-006', 'El Principito', 1, 'Antoine de Saint-Exupéry', '2023-09-01', 'Biblioteca', 'Disponible'),
('LIB-007', 'Física Conceptual', 1, 'Texto de física para décimo y once', '2021-04-12', 'Biblioteca', 'Disponible'),
('LIB-008', 'Química General', 1, 'Texto de química para bachillerato', '2021-04-12', 'Biblioteca', 'Disponible'),
('LIB-009', 'Historia de Colombia', 1, 'Compendio de historia nacional', '2022-11-30', 'Biblioteca', 'Disponible'),
('LIB-010', 'Cuentos de la Selva', 1, 'Horacio Quiroga, lectura de primaria', '2023-01-25', 'Biblioteca', 'Disponible'),
('LIB-011', 'Matemáticas 5', 1, 'Texto guía de quinto grado', '2024-02-02', 'Biblioteca', 'Disponible'),
('LIB-012', 'Ciencias Naturales 3', 1, 'Texto guía de tercer grado', '2024-02-02', 'Biblioteca', 'Disponible'),
('LIB-013', 'Inglés Beginner', 1, 'Libro de inglés nivel inicial', '2024-02-02', 'Biblioteca', 'Disponible'),
('LIB-014', 'Geografía de América', 1, 'Edición antigua, páginas deterioradas', '2015-05-09', 'Bodega', 'Dado de Baja'),
('LAB-001', 'Microscopio óptico 1', 2, 'Microscopio de 1000 aumentos', '2022-05-20', 'Laboratorio 1', 'Prestado'),
('LAB-002', 'Microscopio óptico 2', 2, 'Microscopio de 1000 aumentos', '2022-05-20', 'Laboratorio 1', 'Prestado'),
('LAB-003', 'Balanza analítica', 2, 'Precisión de 0.01 g', '2021-09-14', 'Laboratorio 1', 'Disponible'),
('LAB-004', 'Kit de química básica', 2, 'Tubos de ensayo, pipetas y soportes', '2023-02-28', 'Laboratorio 2', 'Disponible'),
('LAB-005', 'Kit de circuitos eléctricos', 2, 'Protoboard, resistencias y LEDs', '2023-02-28', 'Laboratorio 2', 'Disponible'),
('LAB-006', 'Telescopio escolar', 2, 'Telescopio refractor 70 mm', '2020-10-03', 'Laboratorio 2', 'Disponible'),
('LAB-007', 'Multímetro digital', 2, 'En revisión por falla en la pantalla', '2021-07-07', 'Laboratorio 2', 'En Mantenimiento'),
('LAB-008', 'Modelo de célula', 2, 'Modelo desarmable de célula animal', '2019-03-11', 'Laboratorio 1', 'Disponible'),
('DEP-001', 'Balón de fútbol 1', 3, 'Balón número 5', '2024-01-15', 'Coliseo', 'Prestado'),
('DEP-002', 'Balón de fútbol 2', 3, 'Balón número 5', '2024-01-15', 'Coliseo', 'Disponible'),
('DEP-003', 'Balón de baloncesto', 3, 'Balón oficial número 7', '2023-08-22', 'Coliseo', 'Disponible'),
('DEP-004', 'Balón de voleibol', 3, 'Balón de competencia', '2023-08-22', 'Coliseo', 'Disponible'),
('DEP-005', 'Juego de conos', 3, 'Doce conos de entrenamiento', '2022-03-01', 'Coliseo', 'Disponible'),
('DEP-006', 'Cuerdas para saltar', 3, 'Diez cuerdas', '2022-03-01', 'Coliseo', 'Disponible'),
('DEP-007', 'Red de voleibol', 3, 'Red reglamentaria', '2021-11-19', 'Coliseo', 'Disponible'),
('DEP-008', 'Colchoneta de gimnasia', 3, 'Rota, no se puede reparar', '2016-06-30', 'Bodega', 'Dado de Baja'),
('TEC-001', 'Tablet educativa 1', 4, 'Tablet de 10 pulgadas con licencias', '2024-03-12', 'Sala de sistemas', 'Prestado'),
('TEC-002', 'Tablet educativa 2', 4, 'Tablet de 10 pulgadas con licencias', '2024-03-12', 'Sala de sistemas', 'Prestado'),
('TEC-003', 'Calculadora científica 1', 4, 'Casio fx-991', '2023-05-17', 'Sala de sistemas', 'Disponible'),
('TEC-004', 'Calculadora científica 2', 4, 'Casio fx-991', '2023-05-17', 'Sala de sistemas', 'Disponible'),
('TEC-005', 'Portátil de préstamo 1', 4, 'Portátil de 14 pulgadas', '2024-06-10', 'Sala de sistemas', 'Disponible'),
('TEC-006', 'Portátil de préstamo 2', 4, 'Portátil de 14 pulgadas', '2024-06-10', 'Sala de sistemas', 'Disponible'),
('TEC-007', 'Proyector portátil', 4, 'Proyector HDMI de 3000 lúmenes', '2022-12-05', 'Sala de sistemas', 'Disponible'),
('TEC-008', 'Cámara de documentos', 4, 'No enciende, enviada a revisión', '2021-02-14', 'Sala de sistemas', 'En Mantenimiento'),
('DID-001', 'Mapa de Colombia en relieve', 5, 'Mapa físico de pared', '2020-02-18', 'Sala de profesores', 'Prestado'),
('DID-002', 'Modelo del sistema solar', 5, 'Maqueta con planetas desmontables', '2021-10-27', 'Sala de profesores', 'Disponible'),
('DID-003', 'Tangram de madera', 5, 'Veinte juegos', '2022-04-04', 'Sala de profesores', 'Disponible'),
('DID-004', 'Bloques lógicos', 5, 'Caja de 60 piezas', '2022-04-04', 'Sala de profesores', 'Disponible'),
('DID-005', 'Esqueleto humano a escala', 5, 'Modelo de 85 cm', '2020-09-09', 'Sala de profesores', 'Disponible'),
('DID-006', 'Globo terráqueo', 5, 'Globo político de 30 cm', '2021-01-21', 'Sala de profesores', 'Disponible');

-- Préstamos de estudiantes sin devolver (los recursos correspondientes quedaron en estado Prestado arriba).
-- LIB-002, LIB-003 y DEP-001 llevan fecha esperada pasada; LIB-003 sigue en 'Activo' para probar el marcado a Vencido.
INSERT INTO prestamos_estudiante (id_recurso, id_estudiante, fecha_prestamo, fecha_devolucion_esperada, fecha_devolucion_real, estado_prestamo, observaciones) VALUES
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'LIB-001'), 1,  DATE_SUB(CURDATE(), INTERVAL 3 DAY),  DATE_ADD(CURDATE(), INTERVAL 4 DAY),  NULL, 'Activo', NULL),
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'LIB-002'), 5,  DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 13 DAY), NULL, 'Vencido', 'Lectura del plan lector'),
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'LIB-003'), 12, DATE_SUB(CURDATE(), INTERVAL 15 DAY), DATE_SUB(CURDATE(), INTERVAL 8 DAY),  NULL, 'Activo', NULL),
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'TEC-001'), 20, DATE_SUB(CURDATE(), INTERVAL 2 DAY),  DATE_ADD(CURDATE(), INTERVAL 5 DAY),  NULL, 'Activo', 'Trabajo de investigación'),
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'DEP-001'), 33, DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY),  NULL, 'Vencido', NULL),
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'LAB-001'), 40, DATE_SUB(CURDATE(), INTERVAL 1 DAY),  DATE_ADD(CURDATE(), INTERVAL 6 DAY),  NULL, 'Activo', 'Feria de ciencias');

-- Préstamos de estudiantes ya devueltos (historial).
INSERT INTO prestamos_estudiante (id_recurso, id_estudiante, fecha_prestamo, fecha_devolucion_esperada, fecha_devolucion_real, estado_prestamo, observaciones) VALUES
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'LIB-004'), 8,  DATE_SUB(CURDATE(), INTERVAL 40 DAY), DATE_SUB(CURDATE(), INTERVAL 33 DAY), DATE_SUB(CURDATE(), INTERVAL 34 DAY), 'Devuelto', NULL),
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'LIB-005'), 15, DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_SUB(CURDATE(), INTERVAL 23 DAY), DATE_SUB(CURDATE(), INTERVAL 20 DAY), 'Devuelto', 'Devuelto con tres días de retraso'),
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'LIB-006'), 22, DATE_SUB(CURDATE(), INTERVAL 25 DAY), DATE_SUB(CURDATE(), INTERVAL 18 DAY), DATE_SUB(CURDATE(), INTERVAL 19 DAY), 'Devuelto', NULL),
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'DEP-002'), 3,  DATE_SUB(CURDATE(), INTERVAL 12 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY),  DATE_SUB(CURDATE(), INTERVAL 6 DAY),  'Devuelto', NULL),
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'TEC-003'), 44, DATE_SUB(CURDATE(), INTERVAL 18 DAY), DATE_SUB(CURDATE(), INTERVAL 11 DAY), DATE_SUB(CURDATE(), INTERVAL 12 DAY), 'Devuelto', 'Examen de matemáticas');

-- Préstamos de profesores. LAB-002 vencido; DID-001 con fecha pasada pero aún 'Activo'.
INSERT INTO prestamos_profesor (id_recurso, id_profesor, fecha_prestamo, fecha_devolucion_esperada, fecha_devolucion_real, estado_prestamo, observaciones) VALUES
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'TEC-002'), 3, DATE_SUB(CURDATE(), INTERVAL 4 DAY),  DATE_ADD(CURDATE(), INTERVAL 3 DAY),  NULL, 'Activo', NULL),
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'LAB-002'), 7, DATE_SUB(CURDATE(), INTERVAL 12 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY),  NULL, 'Vencido', 'Práctica de biología'),
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'DID-001'), 2, DATE_SUB(CURDATE(), INTERVAL 9 DAY),  DATE_SUB(CURDATE(), INTERVAL 2 DAY),  NULL, 'Activo', NULL),
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'LIB-007'), 1, DATE_SUB(CURDATE(), INTERVAL 35 DAY), DATE_SUB(CURDATE(), INTERVAL 28 DAY), DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'Devuelto', NULL),
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'LAB-003'), 4, DATE_SUB(CURDATE(), INTERVAL 22 DAY), DATE_SUB(CURDATE(), INTERVAL 15 DAY), DATE_SUB(CURDATE(), INTERVAL 15 DAY), 'Devuelto', NULL),
((SELECT id_recurso FROM recursos WHERE codigo_recurso = 'DID-002'), 9, DATE_SUB(CURDATE(), INTERVAL 16 DAY), DATE_SUB(CURDATE(), INTERVAL 9 DAY),  DATE_SUB(CURDATE(), INTERVAL 10 DAY), 'Devuelto', NULL);

-- ============================================
-- PASO 2: EXTRACURRICULARES (módulo 4)
-- Los ids de clubes y eventos se calculan desde LAST_INSERT_ID() (insertados en una sola sentencia).
-- El club Ajedrez (cupo 5) queda lleno a propósito para probar el rechazo de inscripciones.
-- ============================================

INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, capacidad_maxima, horario_reunion, esta_activo) VALUES
('Ajedrez',   'Estrategia y torneos internos de ajedrez',         1, 5,  'Lunes 3-4 PM',     TRUE),
('Robótica',  'Diseño y programación de robots',                  2, 15, 'Martes 3-5 PM',    TRUE),
('Teatro',    'Montaje de obras y expresión corporal',            3, 25, 'Miércoles 3-5 PM', TRUE),
('Fútbol',    'Entrenamiento y torneos interclases',              4, 30, 'Jueves 3-5 PM',    TRUE),
('Música',    'Coro e instrumentos',                              5, 20, 'Viernes 2-4 PM',   TRUE),
('Debate',    'Oratoria y debate argumentado',                    6, 12, 'Lunes 4-5 PM',     TRUE),
('Ciencias',  'Experimentos y proyectos científicos',             7, 20, 'Martes 2-4 PM',    TRUE),
('Arte',      'Pintura, dibujo y manualidades',                   8, 20, 'Jueves 2-4 PM',    TRUE);
SET @club0 = LAST_INSERT_ID();

-- Offsets: 0 Ajedrez, 1 Robótica, 2 Teatro, 3 Fútbol, 4 Música, 5 Debate, 6 Ciencias, 7 Arte
INSERT INTO membresias_club (id_estudiante, id_club, fecha_ingreso, estado_membresia, fecha_retiro, motivo_retiro) VALUES
(1,  @club0 + 0, DATE_SUB(CURDATE(), INTERVAL 200 DAY), 'Activa', NULL, NULL),
(2,  @club0 + 0, DATE_SUB(CURDATE(), INTERVAL 190 DAY), 'Activa', NULL, NULL),
(3,  @club0 + 0, DATE_SUB(CURDATE(), INTERVAL 180 DAY), 'Activa', NULL, NULL),
(4,  @club0 + 0, DATE_SUB(CURDATE(), INTERVAL 170 DAY), 'Activa', NULL, NULL),
(5,  @club0 + 0, DATE_SUB(CURDATE(), INTERVAL 160 DAY), 'Activa', NULL, NULL),
(6,  @club0 + 0, DATE_SUB(CURDATE(), INTERVAL 150 DAY), 'Retirada', DATE_SUB(CURDATE(), INTERVAL 60 DAY), 'Cambio de horario con otra actividad'),
(6,  @club0 + 1, DATE_SUB(CURDATE(), INTERVAL 140 DAY), 'Activa', NULL, NULL),
(7,  @club0 + 1, DATE_SUB(CURDATE(), INTERVAL 140 DAY), 'Activa', NULL, NULL),
(8,  @club0 + 1, DATE_SUB(CURDATE(), INTERVAL 135 DAY), 'Activa', NULL, NULL),
(9,  @club0 + 1, DATE_SUB(CURDATE(), INTERVAL 130 DAY), 'Activa', NULL, NULL),
(10, @club0 + 1, DATE_SUB(CURDATE(), INTERVAL 125 DAY), 'Activa', NULL, NULL),
(11, @club0 + 1, DATE_SUB(CURDATE(), INTERVAL 120 DAY), 'Activa', NULL, NULL),
(12, @club0 + 1, DATE_SUB(CURDATE(), INTERVAL 110 DAY), 'Activa', NULL, NULL),
(13, @club0 + 2, DATE_SUB(CURDATE(), INTERVAL 150 DAY), 'Activa', NULL, NULL),
(14, @club0 + 2, DATE_SUB(CURDATE(), INTERVAL 150 DAY), 'Activa', NULL, NULL),
(15, @club0 + 2, DATE_SUB(CURDATE(), INTERVAL 145 DAY), 'Activa', NULL, NULL),
(16, @club0 + 2, DATE_SUB(CURDATE(), INTERVAL 140 DAY), 'Activa', NULL, NULL),
(17, @club0 + 2, DATE_SUB(CURDATE(), INTERVAL 135 DAY), 'Activa', NULL, NULL),
(18, @club0 + 2, DATE_SUB(CURDATE(), INTERVAL 130 DAY), 'Activa', NULL, NULL),
(19, @club0 + 2, DATE_SUB(CURDATE(), INTERVAL 120 DAY), 'Retirada', DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'Prefiere dedicarse a otro club'),
(20, @club0 + 3, DATE_SUB(CURDATE(), INTERVAL 180 DAY), 'Activa', NULL, NULL),
(21, @club0 + 3, DATE_SUB(CURDATE(), INTERVAL 180 DAY), 'Activa', NULL, NULL),
(22, @club0 + 3, DATE_SUB(CURDATE(), INTERVAL 175 DAY), 'Activa', NULL, NULL),
(23, @club0 + 3, DATE_SUB(CURDATE(), INTERVAL 170 DAY), 'Activa', NULL, NULL),
(24, @club0 + 3, DATE_SUB(CURDATE(), INTERVAL 165 DAY), 'Activa', NULL, NULL),
(25, @club0 + 3, DATE_SUB(CURDATE(), INTERVAL 160 DAY), 'Activa', NULL, NULL),
(26, @club0 + 3, DATE_SUB(CURDATE(), INTERVAL 155 DAY), 'Activa', NULL, NULL),
(27, @club0 + 3, DATE_SUB(CURDATE(), INTERVAL 150 DAY), 'Activa', NULL, NULL),
(28, @club0 + 3, DATE_SUB(CURDATE(), INTERVAL 145 DAY), 'Activa', NULL, NULL),
(29, @club0 + 3, DATE_SUB(CURDATE(), INTERVAL 140 DAY), 'Activa', NULL, NULL),
(30, @club0 + 4, DATE_SUB(CURDATE(), INTERVAL 160 DAY), 'Activa', NULL, NULL),
(31, @club0 + 4, DATE_SUB(CURDATE(), INTERVAL 160 DAY), 'Activa', NULL, NULL),
(32, @club0 + 4, DATE_SUB(CURDATE(), INTERVAL 155 DAY), 'Activa', NULL, NULL),
(33, @club0 + 4, DATE_SUB(CURDATE(), INTERVAL 150 DAY), 'Activa', NULL, NULL),
(34, @club0 + 4, DATE_SUB(CURDATE(), INTERVAL 145 DAY), 'Activa', NULL, NULL),
(35, @club0 + 4, DATE_SUB(CURDATE(), INTERVAL 140 DAY), 'Activa', NULL, NULL),
(1,  @club0 + 4, DATE_SUB(CURDATE(), INTERVAL 100 DAY), 'Activa', NULL, NULL),
(36, @club0 + 5, DATE_SUB(CURDATE(), INTERVAL 130 DAY), 'Activa', NULL, NULL),
(37, @club0 + 5, DATE_SUB(CURDATE(), INTERVAL 130 DAY), 'Activa', NULL, NULL),
(38, @club0 + 5, DATE_SUB(CURDATE(), INTERVAL 125 DAY), 'Activa', NULL, NULL),
(39, @club0 + 5, DATE_SUB(CURDATE(), INTERVAL 120 DAY), 'Activa', NULL, NULL),
(2,  @club0 + 5, DATE_SUB(CURDATE(), INTERVAL 90 DAY),  'Activa', NULL, NULL),
(40, @club0 + 6, DATE_SUB(CURDATE(), INTERVAL 120 DAY), 'Activa', NULL, NULL),
(41, @club0 + 6, DATE_SUB(CURDATE(), INTERVAL 120 DAY), 'Activa', NULL, NULL),
(42, @club0 + 6, DATE_SUB(CURDATE(), INTERVAL 115 DAY), 'Activa', NULL, NULL),
(43, @club0 + 6, DATE_SUB(CURDATE(), INTERVAL 110 DAY), 'Activa', NULL, NULL),
(6,  @club0 + 6, DATE_SUB(CURDATE(), INTERVAL 55 DAY),  'Activa', NULL, NULL),
(44, @club0 + 7, DATE_SUB(CURDATE(), INTERVAL 110 DAY), 'Activa', NULL, NULL),
(45, @club0 + 7, DATE_SUB(CURDATE(), INTERVAL 105 DAY), 'Activa', NULL, NULL),
(46, @club0 + 7, DATE_SUB(CURDATE(), INTERVAL 100 DAY), 'Activa', NULL, NULL),
(47, @club0 + 7, DATE_SUB(CURDATE(), INTERVAL 95 DAY),  'Activa', NULL, NULL),
(48, @club0 + 7, DATE_SUB(CURDATE(), INTERVAL 90 DAY),  'Activa', NULL, NULL),
(20, @club0 + 7, DATE_SUB(CURDATE(), INTERVAL 80 DAY),  'Retirada', DATE_SUB(CURDATE(), INTERVAL 20 DAY), 'Falta de tiempo');

INSERT INTO eventos (nombre_evento, descripcion, tipo_evento, fecha_evento, lugar, tiene_competencia, id_organizador, esta_activo) VALUES
('Olimpiadas de Matemáticas', 'Competencia interna de razonamiento matemático', 'Académico', DATE_SUB(CURDATE(), INTERVAL 120 DAY), 'Auditorio',        TRUE,  1, TRUE),
('Feria de Ciencias',         'Exposición de proyectos científicos',            'Científico', DATE_SUB(CURDATE(), INTERVAL 90 DAY),  'Laboratorio 1',    TRUE,  7, TRUE),
('Festival de Talentos',      'Presentaciones de música, danza y teatro',        'Cultural',   DATE_SUB(CURDATE(), INTERVAL 60 DAY),  'Auditorio',        TRUE,  3, TRUE),
('Torneo Interclases de Fútbol', 'Torneo entre cursos de secundaria',            'Deportivo',  DATE_SUB(CURDATE(), INTERVAL 30 DAY),  'Cancha principal', TRUE,  4, TRUE),
('Día de la Familia',         'Jornada de integración con las familias',         'Social',     DATE_SUB(CURDATE(), INTERVAL 10 DAY),  'Patio central',    FALSE, 5, TRUE),
('Muestra de Arte',           'Exposición de trabajos de arte del colegio',      'Cultural',   DATE_ADD(CURDATE(), INTERVAL 20 DAY),  'Biblioteca',       FALSE, 8, TRUE);
SET @event0 = LAST_INSERT_ID();

-- Eventos con competencia: rol Competidor y posición. Sin competencia: rol Participante o Asistente, sin posición.
INSERT INTO participantes_evento (id_evento, id_estudiante, rol_participacion, posicion_obtenida) VALUES
(@event0 + 0, 40, 'Competidor', '1er Lugar'),
(@event0 + 0, 41, 'Competidor', '2do Lugar'),
(@event0 + 0, 36, 'Competidor', '3er Lugar'),
(@event0 + 0, 10, 'Competidor', 'Participante'),
(@event0 + 0, 25, 'Competidor', 'Participante'),
(@event0 + 1, 42, 'Competidor', '1er Lugar'),
(@event0 + 1, 43, 'Competidor', '2do Lugar'),
(@event0 + 1, 7,  'Competidor', '3er Lugar'),
(@event0 + 1, 8,  'Competidor', 'Participante'),
(@event0 + 2, 13, 'Competidor', '1er Lugar'),
(@event0 + 2, 30, 'Competidor', '2do Lugar'),
(@event0 + 2, 14, 'Competidor', '3er Lugar'),
(@event0 + 2, 31, 'Competidor', 'Participante'),
(@event0 + 2, 15, 'Competidor', 'Participante'),
(@event0 + 3, 20, 'Competidor', '1er Lugar'),
(@event0 + 3, 21, 'Competidor', '2do Lugar'),
(@event0 + 3, 22, 'Competidor', '3er Lugar'),
(@event0 + 3, 23, 'Competidor', 'Participante'),
(@event0 + 3, 24, 'Competidor', 'Participante'),
(@event0 + 4, 1,  'Participante', NULL),
(@event0 + 4, 2,  'Participante', NULL),
(@event0 + 4, 3,  'Asistente', NULL),
(@event0 + 4, 26, 'Asistente', NULL),
(@event0 + 4, 44, 'Participante', NULL),
(@event0 + 5, 44, 'Participante', NULL),
(@event0 + 5, 45, 'Participante', NULL),
(@event0 + 5, 46, 'Participante', NULL);

-- ============================================
-- PASO 3: FINANCIERA (módulo 5)
-- Becas, pagos del año 2026 (matrícula y pensiones) y una factura por pago.
-- Los estudiantes con id múltiplo de 6 dejan de pagar la pensión después de junio: son los morosos del reporte.
-- Los demás tienen un pago de pensión reciente (relativo a CURDATE()) para no figurar como morosos.
-- Las facturas se crean sin archivo PDF; la opción "Regenerar PDF" del módulo Financiera lo genera.
-- ============================================

-- Los conceptos de pago (MAT, PENSION, UNIFORME, TRANSPORTE, ...) ya los carga 01-schema.sql; aquí solo se usan.

INSERT INTO becas (id_estudiante, año_academico, porcentaje_descuento, descripcion) VALUES
(3, 2026, 50.00, 'Beca por excelencia académica'),
(9, 2026, 25.00, 'Beca deportiva'),
(14, 2026, 30.00, 'Beca por convenio empresarial'),
(22, 2026, 50.00, 'Beca socioeconómica'),
(30, 2026, 25.00, 'Beca cultural'),
(41, 2026, 75.00, 'Beca socioeconómica');

-- Matrícula de todos los estudiantes (enero de 2026).
INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, monto_pagado, fecha_pago, metodo_pago, año_academico, periodo_academico, notas)
SELECT e.id_estudiante,
       (SELECT ea.id_acudiente FROM estudiante_acudiente ea WHERE ea.id_estudiante = e.id_estudiante ORDER BY ea.es_principal DESC, ea.id_acudiente LIMIT 1),
       (SELECT id_concepto FROM conceptos_pago WHERE codigo_concepto = 'MAT'),
       (SELECT monto_base FROM conceptos_pago WHERE codigo_concepto = 'MAT'),
       DATE_ADD('2026-01-05', INTERVAL (e.id_estudiante % 20) DAY),
       ELT(1 + (e.id_estudiante % 5), 'Efectivo', 'Transferencia', 'Tarjeta Débito', 'Tarjeta Crédito', 'Cheque'),
       2026, 'Anual', 'Pago de matrícula'
FROM estudiantes e;

-- Pensiones mensuales de enero a septiembre (los morosos solo hasta junio), con la beca aplicada.
INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, monto_pagado, fecha_pago, metodo_pago, año_academico, periodo_academico, notas)
WITH RECURSIVE meses (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM meses WHERE n < 9)
SELECT e.id_estudiante,
       (SELECT ea.id_acudiente FROM estudiante_acudiente ea WHERE ea.id_estudiante = e.id_estudiante ORDER BY ea.es_principal DESC, ea.id_acudiente LIMIT 1),
       (SELECT id_concepto FROM conceptos_pago WHERE codigo_concepto = 'PENSION'),
       ROUND((SELECT monto_base FROM conceptos_pago WHERE codigo_concepto = 'PENSION') * (1 - COALESCE(b.porcentaje_descuento, 0) / 100), 2),
       LEAST(DATE_ADD(MAKEDATE(2026, 1), INTERVAL (m.n - 1) MONTH) + INTERVAL (5 + e.id_estudiante % 20) DAY, CURDATE()),
       ELT(1 + ((e.id_estudiante + m.n) % 5), 'Efectivo', 'Transferencia', 'Tarjeta Débito', 'Tarjeta Crédito', 'Cheque'),
       2026,
       CASE WHEN m.n <= 3 THEN 'I' WHEN m.n <= 6 THEN 'II' ELSE 'III' END,
       CONCAT('Pensión mes ', m.n)
FROM estudiantes e
JOIN meses m ON (e.id_estudiante % 6 <> 0 OR m.n <= 6)
LEFT JOIN becas b ON b.id_estudiante = e.id_estudiante AND b.año_academico = 2026;

-- Un pago de pensión reciente para los estudiantes que no son morosos (no figuran en el reporte de morosos).
INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, monto_pagado, fecha_pago, metodo_pago, año_academico, periodo_academico, notas)
SELECT e.id_estudiante,
       (SELECT ea.id_acudiente FROM estudiante_acudiente ea WHERE ea.id_estudiante = e.id_estudiante ORDER BY ea.es_principal DESC, ea.id_acudiente LIMIT 1),
       (SELECT id_concepto FROM conceptos_pago WHERE codigo_concepto = 'PENSION'),
       ROUND((SELECT monto_base FROM conceptos_pago WHERE codigo_concepto = 'PENSION') * (1 - COALESCE(b.porcentaje_descuento, 0) / 100), 2),
       DATE_SUB(CURDATE(), INTERVAL (e.id_estudiante % 20) DAY),
       'Transferencia', 2026, 'III', 'Pensión reciente'
FROM estudiantes e
LEFT JOIN becas b ON b.id_estudiante = e.id_estudiante AND b.año_academico = 2026
WHERE e.id_estudiante % 6 <> 0;

-- Algunos pagos de transporte y uniformes para variar los ingresos por concepto.
INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, monto_pagado, fecha_pago, metodo_pago, año_academico, periodo_academico, notas)
SELECT e.id_estudiante,
       (SELECT ea.id_acudiente FROM estudiante_acudiente ea WHERE ea.id_estudiante = e.id_estudiante ORDER BY ea.es_principal DESC, ea.id_acudiente LIMIT 1),
       (SELECT id_concepto FROM conceptos_pago WHERE codigo_concepto = IF(e.id_estudiante % 2 = 0, 'TRANSPORTE', 'UNIFORME')),
       (SELECT monto_base FROM conceptos_pago WHERE codigo_concepto = IF(e.id_estudiante % 2 = 0, 'TRANSPORTE', 'UNIFORME')),
       DATE_ADD('2026-02-10', INTERVAL (e.id_estudiante % 15) DAY),
       'Efectivo', 2026, IF(e.id_estudiante % 2 = 0, 'I', 'Anual'), NULL
FROM estudiantes e
WHERE e.id_estudiante % 3 = 0;

-- Recibo y factura consecutivos por pago (el CUFE sigue la fórmula de la aplicación: SHA-256 de número + NIT + fecha).
UPDATE pagos SET numero_recibo = CONCAT('REC-', LPAD(id_pago, 6, '0')) WHERE numero_recibo IS NULL;

INSERT INTO facturas (numero_factura, id_pago, fecha_emision, cufe)
SELECT CONCAT('FE-', LPAD(p.id_pago, 6, '0')), p.id_pago, p.fecha_pago,
       SHA2(CONCAT('FE-', LPAD(p.id_pago, 6, '0'), '900123456-7', p.fecha_pago), 256)
FROM pagos p
ORDER BY p.id_pago;
