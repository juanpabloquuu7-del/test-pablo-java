package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.AverageLine;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class AcademicRecordDaoImpl extends BaseJdbcDao implements AcademicRecordDao {

    @Override
    public int insert(AcademicRecord r) {
        if (r == null) {
            throw new IllegalArgumentException("El registro académico no puede ser nulo.");
        }
        Date recordDate = r.recordDate() != null ? Date.valueOf(r.recordDate()) : Date.valueOf(LocalDate.now());
        return insert(
                "INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, observaciones, fecha_registro) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                r.studentId(),
                r.subjectId(),
                r.teacherId(),
                r.year(),
                r.period(),
                r.grade(),
                r.observations(),
                recordDate
        );
    }

    @Override
    public int update(AcademicRecord r) {
        if (r == null) {
            throw new IllegalArgumentException("El registro académico no puede ser nulo.");
        }
        Date recordDate = r.recordDate() != null ? Date.valueOf(r.recordDate()) : Date.valueOf(LocalDate.now());
        if (r.id() > 0) {
            return update(
                    "UPDATE registros_academicos SET valor_nota = ?, observaciones = ?, fecha_registro = ?, id_profesor = ? "
                            + "WHERE id_registro = ?",
                    r.grade(),
                    r.observations(),
                    recordDate,
                    r.teacherId(),
                    r.id()
            );
        }
        return update(
                "UPDATE registros_academicos SET valor_nota = ?, observaciones = ?, fecha_registro = ?, id_profesor = ? "
                        + "WHERE id_estudiante = ? AND id_materia = ? AND año_academico = ? AND periodo_academico = ?",
                r.grade(),
                r.observations(),
                recordDate,
                r.teacherId(),
                r.studentId(),
                r.subjectId(),
                r.year(),
                r.period()
        );
    }

    @Override
    public Optional<AcademicRecord> find(int studentId, int subjectId, int year, String period) {
        if (period == null || period.isBlank()) {
            return Optional.empty();
        }
        return queryOne(
                "SELECT * FROM registros_academicos WHERE id_estudiante = ? AND id_materia = ? AND año_academico = ? AND periodo_academico = ?",
                this::map,
                studentId,
                subjectId,
                year,
                period
        );
    }

    @Override
    public List<AcademicRecord> findByStudent(int studentId) {
        if (studentId <= 0) {
            return List.of();
        }
        return query(
                "SELECT * FROM registros_academicos WHERE id_estudiante = ? "
                        + "ORDER BY año_academico, CASE periodo_academico WHEN 'I' THEN 1 WHEN 'II' THEN 2 WHEN 'III' THEN 3 WHEN 'IV' THEN 4 WHEN 'Final' THEN 5 ELSE 6 END",
                this::map,
                studentId
        );
    }

    @Override
    public List<AcademicRecord> findByStudentAndYear(int studentId, int year) {
        if (studentId <= 0 || year <= 0) {
            return List.of();
        }
        return query(
                "SELECT * FROM registros_academicos WHERE id_estudiante = ? AND año_academico = ? "
                        + "ORDER BY CASE periodo_academico WHEN 'I' THEN 1 WHEN 'II' THEN 2 WHEN 'III' THEN 3 WHEN 'IV' THEN 4 WHEN 'Final' THEN 5 ELSE 6 END",
                this::map,
                studentId,
                year
        );
    }

    @Override
    public List<AcademicRecord> findByCourse(int subjectId, int year, String period) {
        if (subjectId <= 0 || year <= 0 || period == null) {
            return List.of();
        }
        return query(
                "SELECT * FROM registros_academicos WHERE id_materia = ? AND año_academico = ? AND periodo_academico = ? "
                        + "ORDER BY id_estudiante",
                this::map,
                subjectId,
                year,
                period
        );
    }

    @Override
    public Optional<BigDecimal> findAverageByStudentAndSubject(int studentId, int subjectId, int year) {
        if (studentId <= 0 || subjectId <= 0 || year <= 0) {
            return Optional.empty();
        }
        List<BigDecimal> list = query(
                "SELECT ROUND(AVG(valor_nota), 1) FROM registros_academicos "
                        + "WHERE id_estudiante = ? AND id_materia = ? AND año_academico = ? AND periodo_academico != 'Final'",
                rs -> rs.getBigDecimal(1),
                studentId,
                subjectId,
                year
        );
        return (list.isEmpty() || list.get(0) == null) ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<BigDecimal> findAverageByStudentAndPeriod(int studentId, int year, String period) {
        if (studentId <= 0 || year <= 0 || period == null) {
            return Optional.empty();
        }
        List<BigDecimal> list = query(
                "SELECT ROUND(AVG(valor_nota), 1) FROM registros_academicos "
                        + "WHERE id_estudiante = ? AND año_academico = ? AND periodo_academico = ? AND periodo_academico != 'Final'",
                rs -> rs.getBigDecimal(1),
                studentId,
                year,
                period
        );
        return (list.isEmpty() || list.get(0) == null) ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<BigDecimal> findCourseAverage(int subjectId, int year, String period) {
        if (subjectId <= 0 || year <= 0) {
            return Optional.empty();
        }
        String sql;
        Object[] params;
        if (period != null && !period.isBlank()) {
            sql = "SELECT ROUND(AVG(valor_nota), 1) FROM registros_academicos "
                    + "WHERE id_materia = ? AND año_academico = ? AND periodo_academico = ? AND periodo_academico != 'Final'";
            params = new Object[]{subjectId, year, period};
        } else {
            sql = "SELECT ROUND(AVG(valor_nota), 1) FROM registros_academicos "
                    + "WHERE id_materia = ? AND año_academico = ? AND periodo_academico != 'Final'";
            params = new Object[]{subjectId, year};
        }
        List<BigDecimal> list = query(sql, rs -> rs.getBigDecimal(1), params);
        return (list.isEmpty() || list.get(0) == null) ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<AverageLine> findSubjectAveragesByStudent(int studentId, int year) {
        if (studentId <= 0 || year <= 0) {
            return List.of();
        }
        return query(
                "SELECT m.nombre_materia, ROUND(AVG(r.valor_nota), 1) "
                        + "FROM registros_academicos r "
                        + "JOIN materias m ON r.id_materia = m.id_materia "
                        + "WHERE r.id_estudiante = ? AND r.año_academico = ? AND r.periodo_academico != 'Final' "
                        + "GROUP BY r.id_materia, m.nombre_materia "
                        + "ORDER BY m.nombre_materia",
                rs -> new AverageLine(rs.getString(1), rs.getBigDecimal(2)),
                studentId,
                year
        );
    }

    @Override
    public List<AverageLine> findCourseAveragesByYearAndPeriod(int year, String period) {
        if (year <= 0 || period == null) {
            return List.of();
        }
        return query(
                "SELECT m.nombre_materia, ROUND(AVG(r.valor_nota), 1) "
                        + "FROM registros_academicos r "
                        + "JOIN materias m ON r.id_materia = m.id_materia "
                        + "WHERE r.año_academico = ? AND r.periodo_academico = ? AND r.periodo_academico != 'Final' "
                        + "GROUP BY r.id_materia, m.nombre_materia "
                        + "ORDER BY m.nombre_materia",
                rs -> new AverageLine(rs.getString(1), rs.getBigDecimal(2)),
                year,
                period
        );
    }

    private AcademicRecord map(ResultSet rs) throws SQLException {
        Date regDate = rs.getDate("fecha_registro");
        return new AcademicRecord(
                rs.getInt("id_registro"),
                rs.getInt("id_estudiante"),
                rs.getInt("id_materia"),
                rs.getInt("id_profesor"),
                rs.getInt("año_academico"),
                rs.getString("periodo_academico"),
                rs.getBigDecimal("valor_nota"),
                rs.getString("observaciones"),
                regDate != null ? regDate.toLocalDate() : null
        );
    }
}
