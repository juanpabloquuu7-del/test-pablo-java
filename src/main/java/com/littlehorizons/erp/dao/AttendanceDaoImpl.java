package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Attendance;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class AttendanceDaoImpl extends BaseJdbcDao implements AttendanceDao {

    @Override
    public int insert(Attendance a) {
        if (a == null) {
            throw new IllegalArgumentException("La asistencia no puede ser nula.");
        }
        Date d = a.date() != null ? Date.valueOf(a.date()) : Date.valueOf(LocalDate.now());
        return insert(
                "INSERT INTO asistencias (id_estudiante, id_materia, fecha_asistencia, estado_asistencia, observaciones, registrado_por) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                a.studentId(),
                a.subjectId(),
                d,
                a.status(),
                a.observations(),
                a.registeredBy()
        );
    }

    @Override
    public void insertAll(List<Attendance> attendances) {
        if (attendances == null || attendances.isEmpty()) {
            return;
        }
        inTransaction(c -> {
            for (Attendance a : attendances) {
                if (a == null) {
                    continue;
                }
                Date d = a.date() != null ? Date.valueOf(a.date()) : Date.valueOf(LocalDate.now());
                insert(
                        c,
                        "INSERT INTO asistencias (id_estudiante, id_materia, fecha_asistencia, estado_asistencia, observaciones, registrado_por) "
                                + "VALUES (?, ?, ?, ?, ?, ?)",
                        a.studentId(),
                        a.subjectId(),
                        d,
                        a.status(),
                        a.observations(),
                        a.registeredBy()
                );
            }
            return null;
        });
    }

    @Override
    public int update(Attendance a) {
        if (a == null) {
            throw new IllegalArgumentException("La asistencia no puede ser nula.");
        }
        if (a.id() > 0) {
            return update(
                    "UPDATE asistencias SET estado_asistencia = ?, observaciones = ?, registrado_por = ? WHERE id_asistencia = ?",
                    a.status(),
                    a.observations(),
                    a.registeredBy(),
                    a.id()
            );
        }
        Date d = a.date() != null ? Date.valueOf(a.date()) : Date.valueOf(LocalDate.now());
        return update(
                "UPDATE asistencias SET estado_asistencia = ?, observaciones = ?, registrado_por = ? "
                        + "WHERE id_estudiante = ? AND id_materia = ? AND fecha_asistencia = ?",
                a.status(),
                a.observations(),
                a.registeredBy(),
                a.studentId(),
                a.subjectId(),
                d
        );
    }

    @Override
    public Optional<Attendance> find(int studentId, int subjectId, LocalDate date) {
        if (date == null) {
            return Optional.empty();
        }
        return queryOne(
                "SELECT * FROM asistencias WHERE id_estudiante = ? AND id_materia = ? AND fecha_asistencia = ?",
                this::map,
                studentId,
                subjectId,
                Date.valueOf(date)
        );
    }

    @Override
    public boolean exists(int studentId, int subjectId, LocalDate date) {
        if (date == null) {
            return false;
        }
        List<Integer> list = query(
                "SELECT 1 FROM asistencias WHERE id_estudiante = ? AND id_materia = ? AND fecha_asistencia = ?",
                rs -> rs.getInt(1),
                studentId,
                subjectId,
                Date.valueOf(date)
        );
        return !list.isEmpty();
    }

    @Override
    public List<Attendance> findByStudentAndRange(int studentId, LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return List.of();
        }
        return query(
                "SELECT * FROM asistencias WHERE id_estudiante = ? AND fecha_asistencia BETWEEN ? AND ? "
                        + "ORDER BY fecha_asistencia ASC, id_materia ASC",
                this::map,
                studentId,
                Date.valueOf(start),
                Date.valueOf(end)
        );
    }

    @Override
    public int countAbsenceDaysByWeek(int studentId, int year, int week) {
        int yearWeek = year * 100 + week;
        List<Integer> list = query(
                "SELECT COUNT(DISTINCT fecha_asistencia) FROM asistencias "
                        + "WHERE id_estudiante = ? AND estado_asistencia = 'Ausente' AND YEARWEEK(fecha_asistencia, 3) = ?",
                rs -> rs.getInt(1),
                studentId,
                yearWeek
        );
        return list.isEmpty() ? 0 : list.get(0);
    }

    @Override
    public int countAbsenceDaysByDate(int studentId, LocalDate date) {
        if (date == null) {
            return 0;
        }
        List<Integer> list = query(
                "SELECT COUNT(DISTINCT fecha_asistencia) FROM asistencias "
                        + "WHERE id_estudiante = ? AND estado_asistencia = 'Ausente' AND YEARWEEK(fecha_asistencia, 3) = YEARWEEK(?, 3)",
                rs -> rs.getInt(1),
                studentId,
                Date.valueOf(date)
        );
        return list.isEmpty() ? 0 : list.get(0);
    }

    @Override
    public List<WeeklyAbsenceAlert> findStudentsAboveThreshold(int threshold, int year) {
        if (threshold < 0) {
            threshold = 0;
        }
        return query(
                "SELECT id_estudiante, YEARWEEK(fecha_asistencia, 3) AS semana, COUNT(DISTINCT fecha_asistencia) AS dias_ausente "
                        + "FROM asistencias "
                        + "WHERE estado_asistencia = 'Ausente' AND YEAR(fecha_asistencia) = ? "
                        + "GROUP BY id_estudiante, YEARWEEK(fecha_asistencia, 3) "
                        + "HAVING COUNT(DISTINCT fecha_asistencia) > ? "
                        + "ORDER BY semana DESC, dias_ausente DESC",
                rs -> new WeeklyAbsenceAlert(
                        rs.getInt("id_estudiante"),
                        rs.getInt("semana"),
                        rs.getInt("dias_ausente")
                ),
                year, threshold
        );
    }

    @Override
    public int countTotalAbsences(int studentId, int year) {
        if (studentId <= 0 || year <= 0) {
            return 0;
        }
        List<Integer> list = query(
                "SELECT COUNT(*) FROM asistencias "
                        + "WHERE id_estudiante = ? AND estado_asistencia = 'Ausente' AND YEAR(fecha_asistencia) = ?",
                rs -> rs.getInt(1),
                studentId,
                year
        );
        return list.isEmpty() ? 0 : list.get(0);
    }

    private Attendance map(ResultSet rs) throws SQLException {
        Date d = rs.getDate("fecha_asistencia");
        int regBy = rs.getInt("registrado_por");
        Integer registeredBy = rs.wasNull() ? null : regBy;
        return new Attendance(
                rs.getInt("id_asistencia"),
                rs.getInt("id_estudiante"),
                rs.getInt("id_materia"),
                d != null ? d.toLocalDate() : null,
                rs.getString("estado_asistencia"),
                rs.getString("observaciones"),
                registeredBy
        );
    }
}
