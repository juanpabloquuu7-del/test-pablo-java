package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.TeachingAssignment;

import java.util.List;
import java.util.Optional;

public interface AssignmentDao {

    int insert(TeachingAssignment assignment);

    Optional<TeachingAssignment> findById(int id);

    List<TeachingAssignment> findByTeacher(int teacherId, int year);

    List<TeachingAssignment> findBySubject(int subjectId, int year);

    boolean exists(int teacherId, int subjectId, int year, String section);

    /** True si el profesor tiene asignada la materia en el año (cualquier sección). */
    boolean teacherHasSubject(int teacherId, int subjectId, int year);

    /** True si ya hay notas registradas por ese profesor en la materia y año de la asignación. */
    boolean hasGrades(int assignmentId);

    int delete(int assignmentId);

    int insertSchedule(ClassSchedule schedule);

    List<ClassSchedule> findSchedulesByAssignment(int assignmentId);

    /** Todos los horarios de las asignaciones del profesor en el año. */
    List<ClassSchedule> findSchedulesByTeacher(int teacherId, int year);

    int deleteSchedule(int scheduleId);
}
