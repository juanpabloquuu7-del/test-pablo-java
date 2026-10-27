#!/usr/bin/env python3
"""
Generador determinista de datos de prueba para el Área A de Little Horizons ERP.
Genera docs/sql/11-data-a.sql a partir de las decisiones fijadas del proyecto y los IDs base de 01-schema.sql y 02-data-base.sql.

Reglas aplicadas:
1. 60 materias (5 por grado, 12 grados), códigos únicos tipo MAT-01, horas semanales entre 2 y 5.
2. 60 asignaciones de profesor para 2026 (sección 'A') cubriendo todas las materias y los 12 profesores.
3. 156 bloques de horarios_clase sin traslapes por profesor ni por grado, entre 06:00 y 18:00 (Lunes a Viernes).
4. 960 inscripciones en materias (48 estudiantes x 5 materias de su grado x 4 períodos I-IV), sección 'A', estado 'Activa'.
5. 480 registros académicos (períodos I y II para todos los estudiantes en sus 5 materias), notas 0.0-5.0 con 1 decimal.
6. 2496 registros de asistencia a lo largo de 4 semanas de febrero 2026 (días Lunes a Viernes):
   - Estudiantes con >3 días distintos de ausencia en una semana (disparan alerta semanal).
   - Un estudiante con múltiples ausencias el mismo día (1 solo día distinto de ausencia, NO dispara alerta).
"""

import os
import random
from datetime import date, timedelta

SEED = 20261022

# Catálogo de grados base (id_grado, nombre_grado, nivel_numerico)
GRADES = [
    (1, "Preescolar", 0),
    (2, "Primero", 1),
    (3, "Segundo", 2),
    (4, "Tercero", 3),
    (5, "Cuarto", 4),
    (6, "Quinto", 5),
    (7, "Sexto", 6),
    (8, "Séptimo", 7),
    (9, "Octavo", 8),
    (10, "Noveno", 9),
    (11, "Décimo", 10),
    (12, "Once", 11),
]

# Especialidades de los 12 profesores (id_profesor, código, nombre área)
TEACHERS_INFO = [
    (1, "MAT", "Matemáticas"),
    (2, "ESP", "Lengua Castellana"),
    (3, "ING", "Inglés"),
    (4, "CND", "Ciencias Naturales"),
    (5, "CSO", "Ciencias Sociales"),
    (6, "EFI", "Educación Física"),
    (7, "ART", "Educación Artística"),
    (8, "INF", "Tecnología e Informática"),
    (9, "QUI", "Química"),
    (10, "FIS", "Física"),
    (11, "BIO", "Biología"),
    (12, "ETI", "Ética y Valores"),
]

# Estudiantes por grado en 2026 (según 02-data-base.sql)
STUDENTS_BY_GRADE = {
    1: [10, 29, 30, 32],
    2: [7, 9, 43, 48],
    3: [17, 18, 21, 28],
    4: [6, 19, 33, 42],
    5: [12, 15, 16, 31],
    6: [2, 13, 23, 47],
    7: [1, 4, 8, 38],
    8: [5, 14, 34, 35],
    9: [24, 26, 39, 46],
    10: [27, 37, 40, 41],
    11: [20, 25, 36, 44],
    12: [3, 11, 22, 45],
}

# Bloques horarios por slot k (0..4)
# Slot 0: Lun, Mié, Vie 07:00-08:00 (3h)
# Slot 1: Lun, Mié, Vie 08:00-09:00 (3h)
# Slot 2: Lun, Mié, Vie 09:00-10:00 (3h)
# Slot 3: Mar, Jue 07:00-08:00 (2h)
# Slot 4: Mar, Jue 08:00-09:00 (2h)
SLOT_SCHEDULES = {
    0: [("Lunes", "07:00:00", "08:00:00"), ("Miércoles", "07:00:00", "08:00:00"), ("Viernes", "07:00:00", "08:00:00")],
    1: [("Lunes", "08:00:00", "09:00:00"), ("Miércoles", "08:00:00", "09:00:00"), ("Viernes", "08:00:00", "09:00:00")],
    2: [("Lunes", "09:00:00", "10:00:00"), ("Miércoles", "09:00:00", "10:00:00"), ("Viernes", "09:00:00", "10:00:00")],
    3: [("Martes", "07:00:00", "08:00:00"), ("Jueves", "07:00:00", "08:00:00")],
    4: [("Martes", "08:00:00", "09:00:00"), ("Jueves", "08:00:00", "09:00:00")],
}

SLOT_HOURS = {
    0: 3,
    1: 3,
    2: 3,
    3: 2,
    4: 2,
}


def build_data():
    rng = random.Random(SEED)

    subjects = []
    assignments = []
    schedules = []
    subject_teacher_map = {}
    subject_slot_map = {}
    grade_subjects_map = {g: [] for g in range(1, 13)}

    sub_id = 1
    sched_id = 1

    # 1. Materias y Asignaciones
    for g_id, g_name, _ in GRADES:
        for k in range(5):
            t_id = ((g_id - 1 + k) % 12) + 1
            t_prefix = TEACHERS_INFO[t_id - 1][1]
            t_area = TEACHERS_INFO[t_id - 1][2]
            code = f"{t_prefix}-{g_id:02d}"
            name = f"{t_area} - Grado {g_name}"
            desc = f"Programa curricular de {t_area} para el grado {g_name}."
            hours = SLOT_HOURS[k]

            subjects.append((sub_id, code, name, desc, g_id, hours, True))
            assignments.append((sub_id, t_id, sub_id, 2026, "A"))
            subject_teacher_map[sub_id] = t_id
            subject_slot_map[sub_id] = k
            grade_subjects_map[g_id].append(sub_id)

            for day, start_t, end_t in SLOT_SCHEDULES[k]:
                schedules.append((sched_id, sub_id, day, start_t, end_t))
                sched_id += 1

            sub_id += 1

    # 2. Inscripciones en materias (períodos I, II, III, IV para todos los 48 estudiantes)
    enrollments = []
    enr_id = 1
    period_dates = {
        "I": "2026-01-20",
        "II": "2026-04-06",
        "III": "2026-07-06",
        "IV": "2026-09-14",
    }
    for g_id in range(1, 13):
        student_ids = STUDENTS_BY_GRADE[g_id]
        sub_ids = grade_subjects_map[g_id]
        for sid in student_ids:
            for s_materia in sub_ids:
                for period in ["I", "II", "III", "IV"]:
                    enrollments.append(
                        (enr_id, sid, s_materia, 2026, period, "A", period_dates[period], "Activa")
                    )
                    enr_id += 1

    # 3. Registros académicos (períodos I y II para todos los 48 estudiantes en las 5 materias = 480 notas)
    records = []
    rec_id = 1
    grade_pool = [
        3.2, 3.5, 3.8, 4.0, 4.2, 4.5, 4.7, 4.8, 5.0,
        3.0, 3.6, 4.1, 4.4, 4.6, 4.9, 2.5, 2.8, 3.3, 3.7, 4.3
    ]
    rec_dates = {"I": "2026-03-27", "II": "2026-06-12"}

    for period in ["I", "II"]:
        rdate = rec_dates[period]
        for g_id in range(1, 13):
            for sid in STUDENTS_BY_GRADE[g_id]:
                for s_materia in grade_subjects_map[g_id]:
                    prof_id = subject_teacher_map[s_materia]
                    grade_val = rng.choice(grade_pool)
                    if grade_val >= 4.6:
                        obs = "Excelente desempeño y comprensión de la materia."
                    elif grade_val >= 4.0:
                        obs = "Buen nivel de aprendizaje, cumple oportunamente."
                    elif grade_val >= 3.0:
                        obs = "Desempeño básico satisfactorio en los objetivos."
                    else:
                        obs = "Rendimiento bajo, requiere refuerzo pedagógico."

                    records.append(
                        (rec_id, sid, s_materia, prof_id, 2026, period, grade_val, obs, rdate)
                    )
                    rec_id += 1

    # 4. Asistencias (4 semanas de febrero de 2026: 02/02 al 27/02)
    # Lunes a Viernes
    # Semanas ISO (YEARWEEK(..., 3)):
    # Sem 1: 2026-02-02 .. 2026-02-06 (202606)
    # Sem 2: 2026-02-09 .. 2026-02-13 (202607)
    # Sem 3: 2026-02-16 .. 2026-02-20 (202608)
    # Sem 4: 2026-02-23 .. 2026-02-27 (202609)
    attendance = []
    att_id = 1

    day_slots = {
        "Monday": [0, 1, 2],
        "Tuesday": [3, 4],
        "Wednesday": [0, 1, 2],
        "Thursday": [3, 4],
        "Friday": [0, 1, 2],
    }

    start_monday = date(2026, 2, 2)
    calendar_days = []
    for week_idx in range(4):
        for day_offset in range(5):
            curr_d = start_monday + timedelta(days=week_idx * 7 + day_offset)
            calendar_days.append(curr_d)

    for curr_d in calendar_days:
        day_name = curr_d.strftime("%A")
        active_slots = day_slots[day_name]
        d_str = curr_d.strftime("%Y-%m-%d")

        for g_id in range(1, 13):
            subs = grade_subjects_map[g_id]
            for sid in STUDENTS_BY_GRADE[g_id]:
                for s_materia in subs:
                    k = subject_slot_map[s_materia]
                    if k not in active_slots:
                        continue

                    prof_id = subject_teacher_map[s_materia]
                    status = "Presente"
                    obs = None

                    # CASO ESPECIAL 1: Estudiante 1 en Semana 2 (2026-02-09 a 2026-02-13)
                    # Ausente en 4 días distintos (Lun, Mar, Mié, Jue) -> dispara alerta (>3 días)
                    if sid == 1 and curr_d == date(2026, 2, 9) and k == 0:
                        status, obs = "Ausente", "Inasistencia injustificada a primera hora"
                    elif sid == 1 and curr_d == date(2026, 2, 10) and k == 3:
                        status, obs = "Ausente", "Inasistencia injustificada sin reporte de acudiente"
                    elif sid == 1 and curr_d == date(2026, 2, 11) and k == 0:
                        status, obs = "Ausente", "Inasistencia injustificada reiterada"
                    elif sid == 1 and curr_d == date(2026, 2, 12) and k == 4:
                        status, obs = "Ausente", "Inasistencia injustificada a jornada completa"

                    # CASO ESPECIAL 2: Estudiante 14 en Semana 3 (2026-02-16 a 2026-02-20)
                    # Ausente en 5 días distintos -> dispara alerta (>3 días)
                    elif sid == 14 and curr_d == date(2026, 2, 16) and k == 1:
                        status, obs = "Ausente", "Falta no justificada"
                    elif sid == 14 and curr_d == date(2026, 2, 17) and k == 3:
                        status, obs = "Ausente", "Falta no justificada"
                    elif sid == 14 and curr_d == date(2026, 2, 18) and k == 2:
                        status, obs = "Ausente", "Falta no justificada"
                    elif sid == 14 and curr_d == date(2026, 2, 19) and k == 4:
                        status, obs = "Ausente", "Falta no justificada"
                    elif sid == 14 and curr_d == date(2026, 2, 20) and k == 0:
                        status, obs = "Ausente", "Falta no justificada"

                    # CASO ESPECIAL 3: Estudiante 3 en Semana 1 (2026-02-02)
                    # Ausente en las 3 materias del MISMO DÍA (Lunes 02/02), resto de semana Presente.
                    # Días distintos con ausencia = 1 (NO debe disparar alerta semanal > 3).
                    elif sid == 3 and curr_d == date(2026, 2, 2):
                        status, obs = "Ausente", "Calamidad doméstica familiar reportada por acudiente"

                    # Variación determinista para otros estudiantes
                    else:
                        roll = rng.random()
                        if roll < 0.02:
                            status, obs = "Tardanza", "Llegada con retraso al inicio de clase"
                        elif roll < 0.035:
                            status, obs = "Excusa Médica", "Presentó soporte de atención médica general"
                        elif roll < 0.045:
                            status, obs = "Ausente", "Inasistencia sin justificación reportada"
                        else:
                            status, obs = "Presente", None

                    attendance.append(
                        (att_id, sid, s_materia, d_str, status, obs, prof_id)
                    )
                    att_id += 1

    return subjects, assignments, schedules, enrollments, records, attendance


def validate_data(subjects, assignments, schedules, enrollments, records, attendance):
    """Verifica reglas de negocio y restricciones UNIQUE."""
    # 1. Materias
    assert len(subjects) == 60, f"Debe haber 60 materias, hay {len(subjects)}"
    sub_codes = [s[1] for s in subjects]
    assert len(set(sub_codes)) == 60, "Hay códigos de materia duplicados"
    for s in subjects:
        assert 2 <= s[5] <= 5, f"Horas semanales fuera de rango: {s}"

    # 2. Asignaciones
    assert len(assignments) == 60, f"Debe haber 60 asignaciones, hay {len(assignments)}"
    asg_unique = set()
    asg_teachers = set()
    asg_subjects = set()
    for a in assignments:
        key = (a[1], a[2], a[3], a[4])
        assert key not in asg_unique, f"Asignación duplicada: {key}"
        asg_unique.add(key)
        asg_teachers.add(a[1])
        asg_subjects.add(a[2])
    assert len(asg_teachers) == 12, "No cubre los 12 profesores"
    assert len(asg_subjects) == 60, "No cubre las 60 materias"

    # 3. Horarios y traslapes
    asg_to_teacher = {a[0]: a[1] for a in assignments}
    sched_unique = set()
    teacher_times = {t: [] for t in range(1, 13)}

    for sc in schedules:
        sc_id, asg_id, day, start_t, end_t = sc
        key = (asg_id, day, start_t)
        assert key not in sched_unique, f"Horario duplicado para asignación: {key}"
        sched_unique.add(key)
        assert end_t > start_t, f"hora_fin debe ser mayor a hora_inicio: {sc}"

        t_id = asg_to_teacher[asg_id]
        teacher_times[t_id].append((day, start_t, end_t, asg_id))

    # Verificar NO traslapes por profesor
    for t_id, slots in teacher_times.items():
        by_day = {}
        for day, st, et, asg in slots:
            by_day.setdefault(day, []).append((st, et, asg))
        for day, times in by_day.items():
            sorted_times = sorted(times, key=lambda x: x[0])
            for i in range(len(sorted_times) - 1):
                cur_st, cur_et, cur_asg = sorted_times[i]
                nxt_st, nxt_et, nxt_asg = sorted_times[i + 1]
                assert cur_et <= nxt_st, (
                    f"Traslape detectado en profesor {t_id} el {day}: "
                    f"[{cur_st}-{cur_et}] (asg {cur_asg}) y [{nxt_st}-{nxt_et}] (asg {nxt_asg})"
                )

    # 4. Inscripciones
    assert len(enrollments) >= 240, f"Inscripciones insuficientes: {len(enrollments)}"
    enr_unique = set()
    for e in enrollments:
        key = (e[1], e[2], e[3], e[4])
        assert key not in enr_unique, f"Inscripción duplicada: {key}"
        enr_unique.add(key)

    # 5. Registros académicos
    assert len(records) >= 300, f"Debe haber 300+ registros, hay {len(records)}"
    rec_unique = set()
    enrolled_set = {(e[1], e[2], e[3], e[4]) for e in enrollments}
    asg_sub_to_prof = {a[2]: a[1] for a in assignments}

    for r in records:
        r_id, sid, sub_id, prof_id, year, period, grade, obs, rdate = r
        key = (sid, sub_id, year, period)
        assert key not in rec_unique, f"Registro académico duplicado: {key}"
        rec_unique.add(key)
        assert 0.0 <= grade <= 5.0, f"Nota fuera de rango: {grade}"
        assert prof_id == asg_sub_to_prof[sub_id], f"Profesor no coincide con asignación: {r}"
        assert key in enrolled_set, f"Estudiante no inscrito en ese período: {key}"

    # 6. Asistencias
    att_unique = set()
    absences_by_student_week = {}

    for att in attendance:
        att_id, sid, sub_id, dt_str, status, obs, reg_by = att
        key = (sid, sub_id, dt_str)
        assert key not in att_unique, f"Asistencia duplicada: {key}"
        att_unique.add(key)
        assert reg_by == asg_sub_to_prof[sub_id], f"Profesor que registra no coincide: {att}"

        if status == "Ausente":
            dt = date.fromisoformat(dt_str)
            # YEARWEEK(..., 3) equivale a (año, semana_iso)
            iso_year, iso_week, _ = dt.isocalendar()
            w_key = (sid, iso_year, iso_week)
            absences_by_student_week.setdefault(w_key, set()).add(dt_str)

    # Verificar caso de alerta: al menos un estudiante con > 3 días distintos
    alert_triggered = [
        (k, len(days)) for k, days in absences_by_student_week.items() if len(days) > 3
    ]
    assert len(alert_triggered) >= 2, f"Deben existir casos de alerta por ausencias semanales (>3 días distintos): {alert_triggered}"

    # Verificar caso de varias ausencias el mismo día que NO dispara alerta (estudiante 3 en sem 1)
    st3_w1_abs = absences_by_student_week.get((3, 2026, 6), set())
    assert len(st3_w1_abs) == 1, (
        f"Estudiante 3 debe tener ausencias en exactamente 1 día de sem 1 (no dispara alerta), tiene: {len(st3_w1_abs)}"
    )
    st3_w1_att_count = sum(
        1 for att in attendance if att[1] == 3 and att[3] == "2026-02-02" and att[4] == "Ausente"
    )
    assert st3_w1_att_count == 3, f"Estudiante 3 debe tener 3 ausencias el mismo día, tiene {st3_w1_att_count}"


def generate_sql(subjects, assignments, schedules, enrollments, records, attendance):
    lines = []
    lines.append("-- ============================================================================")
    lines.append("-- Little Horizons ERP - Datos de prueba del Área A (Paso 7)")
    lines.append("-- Generado automáticamente por docs/sql/tools/gen_data_a.py (determinista).")
    lines.append("-- NO EDITAR A MANO.")
    lines.append("--")
    lines.append("-- REGLAS DOCUMENTADAS:")
    lines.append("-- 1. Materias (60 en total):")
    lines.append("--    - 5 materias por grado (grados 1 a 12 de 01-schema.sql).")
    lines.append("--    - Códigos únicos de materia con prefijo de área y sufijo de grado (ej: MAT-01).")
    lines.append("--    - Horas semanales entre 2 y 5 (3h para slots lunes/miércoles/viernes, 2h para martes/jueves).")
    lines.append("-- 2. Asignaciones de Profesor (60 en total):")
    lines.append("--    - Año académico 2026, sección 'A'.")
    lines.append("--    - Cobertura completa de los 12 profesores (5 materias por profesor, balanceado).")
    lines.append("--    - Cobertura de las 60 materias.")
    lines.append("-- 3. Horarios de Clase (156 bloques):")
    lines.append("--    - Días Lunes a Viernes, en franja válida 07:00:00 a 10:00:00 (dentro de 06:00 a 18:00).")
    lines.append("--    - hora_fin > hora_inicio estricto en todos los registros.")
    lines.append("--    - CERO traslapes de horario por profesor y CERO traslapes por grado.")
    lines.append("--    - Cumplimiento de restricción UNIQUE (id_asignacion, dia_semana, hora_inicio).")
    lines.append("-- 4. Inscripciones en Materias (960 en total):")
    lines.append("--    - 48 estudiantes inscritos en las 5 materias de su grado correspondiente.")
    lines.append("--    - Períodos académicos I, II, III y IV del año 2026, sección 'A', estado 'Activa'.")
    lines.append("-- 5. Registros Académicos (480 notas en total >= 300 requeridas):")
    lines.append("--    - Períodos I y II del año 2026 para todos los estudiantes en sus 5 materias.")
    lines.append("--    - Notas entre 0.0 y 5.0 con un decimal (escala colombiana).")
    lines.append("--    - id_profesor corresponde al profesor asignado a la materia.")
    lines.append("--    - Cumple UNIQUE (id_estudiante, id_materia, año_academico, periodo_academico).")
    lines.append("-- 6. Asistencias Diarias (2496 registros):")
    lines.append("--    - 4 semanas completas de febrero de 2026 (días Lunes a Viernes, fechas no futuras).")
    lines.append("--    - registrado_por corresponde al profesor asignado a la materia.")
    lines.append("--    - Casos de prueba de alertas semanales (>3 días distintos con 'Ausente'):")
    lines.append("--        * Estudiante 1 en semana 202607: 4 días distintos con ausencia (alerta activa).")
    lines.append("--        * Estudiante 14 en semana 202608: 5 días distintos con ausencia (alerta activa).")
    lines.append("--    - Caso de prueba de conteo por días distintos (NO alerta):")
    lines.append("--        * Estudiante 3 en semana 202606: 3 ausencias el mismo día (Lunes 02/02), 1 solo día ausente (sin alerta).")
    lines.append("--    - Cumple UNIQUE (id_estudiante, id_materia, fecha_asistencia).")
    lines.append("-- ============================================================================")
    lines.append("USE erp_little_horizons;\n")

    # Materias
    lines.append("-- 1. MATERIAS (60 materias, 5 por grado)")
    lines.append("INSERT INTO materias (id_materia, codigo_materia, nombre_materia, descripcion, id_grado, horas_semanales, esta_activa) VALUES")
    sub_sql = []
    for s in subjects:
        sid, code, name, desc, gid, hrs, act = s
        sub_sql.append(f"({sid}, '{code}', '{name}', '{desc}', {gid}, {hrs}, {1 if act else 0})")
    lines.append(",\n".join(sub_sql) + ";\n")

    # Asignaciones
    lines.append("-- 2. ASIGNACIONES DE PROFESOR (Año 2026, Sección 'A')")
    lines.append("INSERT INTO asignaciones_profesor (id_asignacion, id_profesor, id_materia, año_academico, seccion) VALUES")
    asg_sql = []
    for a in assignments:
        aid, tid, mid, yr, sec = a
        asg_sql.append(f"({aid}, {tid}, {mid}, {yr}, '{sec}')")
    lines.append(",\n".join(asg_sql) + ";\n")

    # Horarios
    lines.append("-- 3. HORARIOS DE CLASE (Sin traslapes por profesor ni por grado)")
    lines.append("INSERT INTO horarios_clase (id_horario, id_asignacion, dia_semana, hora_inicio, hora_fin) VALUES")
    sch_sql = []
    for sc in schedules:
        scid, aid, day, st, et = sc
        sch_sql.append(f"({scid}, {aid}, '{day}', '{st}', '{et}')")
    lines.append(",\n".join(sch_sql) + ";\n")

    # Inscripciones
    lines.append("-- 4. INSCRIPCIONES EN MATERIA (2026, Períodos I-IV)")
    lines.append("INSERT INTO inscripciones_materia (id_inscripcion, id_estudiante, id_materia, año_academico, periodo_academico, seccion, fecha_inscripcion, estado) VALUES")
    enr_sql = []
    for e in enrollments:
        eid, sid, mid, yr, per, sec, f_enr, st = e
        enr_sql.append(f"({eid}, {sid}, {mid}, {yr}, '{per}', '{sec}', '{f_enr}', '{st}')")
    lines.append(",\n".join(enr_sql) + ";\n")

    # Registros Académicos
    lines.append("-- 5. REGISTROS ACADÉMICOS (Notas 2026, Períodos I y II)")
    lines.append("INSERT INTO registros_academicos (id_registro, id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, observaciones, fecha_registro) VALUES")
    rec_sql = []
    for r in records:
        rid, sid, mid, pid, yr, per, val, obs, rdate = r
        rec_sql.append(f"({rid}, {sid}, {mid}, pid={pid}, yr={yr}, per='{per}', val={val:.1f}, obs='{obs}', rdate='{rdate}')".replace("pid=", "").replace("yr=", "").replace("per=", "").replace("val=", "").replace("obs=", "").replace("rdate=", ""))
    lines.append(",\n".join(rec_sql) + ";\n")

    # Asistencias
    lines.append("-- 6. ASISTENCIAS (Febrero 2026, 4 semanas)")
    lines.append("INSERT INTO asistencias (id_asistencia, id_estudiante, id_materia, fecha_asistencia, estado_asistencia, observaciones, registrado_por) VALUES")
    att_sql = []
    for att in attendance:
        atid, sid, mid, dt_str, st, obs, reg = att
        obs_val = f"'{obs}'" if obs is not None else "NULL"
        att_sql.append(f"({atid}, {sid}, {mid}, '{dt_str}', '{st}', {obs_val}, {reg})")
    lines.append(",\n".join(att_sql) + ";\n")

    return "\n".join(lines)


def main():
    print("Construyendo datos deterministas del Área A...")
    subjects, assignments, schedules, enrollments, records, attendance = build_data()

    print("Validando restricciones y reglas de negocio...")
    validate_data(subjects, assignments, schedules, enrollments, records, attendance)
    print("Validaciones superadas con éxito.")

    sql_content = generate_sql(subjects, assignments, schedules, enrollments, records, attendance)

    out_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "11-data-a.sql"))

    print(f"Escribiendo archivo {out_path}...")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(sql_content)

    print("Archivo generado exitosamente.")


if __name__ == "__main__":
    main()
