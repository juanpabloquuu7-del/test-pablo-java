package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.AverageLine;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AcademicRecordDao {

    int insert(AcademicRecord record);

    int update(AcademicRecord record);

    Optional<AcademicRecord> find(int studentId, int subjectId, int year, String period);

    /** Historial académico completo de un estudiante ordenado por año y período. */
    List<AcademicRecord> findByStudent(int studentId);

    List<AcademicRecord> findByStudentAndYear(int studentId, int year);

    List<AcademicRecord> findByCourse(int subjectId, int year, String period);

    /** Promedio por materia de un estudiante en un año (solo períodos I a IV). */
    Optional<BigDecimal> findAverageByStudentAndSubject(int studentId, int subjectId, int year);

    /** Promedio general de un estudiante en un período académico (solo períodos I a IV). */
    Optional<BigDecimal> findAverageByStudentAndPeriod(int studentId, int year, String period);

    /** Promedio del curso en una materia y año (opcionalmente filtrado por período I a IV). */
    Optional<BigDecimal> findCourseAverage(int subjectId, int year, String period);

    /** Promedios por materia de un estudiante en un año (solo períodos I a IV). */
    List<AverageLine> findSubjectAveragesByStudent(int studentId, int year);

    /** Promedios de todas las materias en un curso/año/período. */
    List<AverageLine> findCourseAveragesByYearAndPeriod(int year, String period);
}
