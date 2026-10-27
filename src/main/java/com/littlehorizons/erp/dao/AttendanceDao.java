package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceDao {

    record WeeklyAbsenceAlert(int studentId, int yearWeek, int absenceDays) {
    }

    int insert(Attendance attendance);

    /** Guarda todos los registros de asistencia en una sola transacción. */
    void insertAll(List<Attendance> attendances);

    int update(Attendance attendance);

    Optional<Attendance> find(int studentId, int subjectId, LocalDate date);

    boolean exists(int studentId, int subjectId, LocalDate date);

    List<Attendance> findByStudentAndRange(int studentId, LocalDate start, LocalDate end);

    /** Días distintos con 'Ausente' en la semana calendario dada por año y número de semana (YEARWEEK(..., 3)). */
    int countAbsenceDaysByWeek(int studentId, int year, int week);

    /** Días distintos con 'Ausente' en la semana calendario que contiene la fecha (YEARWEEK(..., 3)). */
    int countAbsenceDaysByDate(int studentId, LocalDate date);

    /**
     * Estudiantes que superan el umbral de ausencias en una semana (días distintos con 'Ausente'
     * agrupados por YEARWEEK(fecha_asistencia, 3) mayores a {@code threshold}).
     */
    List<WeeklyAbsenceAlert> findStudentsAboveThreshold(int threshold, int year);

    /** Total de inasistencias (registros con 'Ausente') de un estudiante en un año. */
    int countTotalAbsences(int studentId, int year);
}
