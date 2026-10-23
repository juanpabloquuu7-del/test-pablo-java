package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Subject;

import java.util.List;
import java.util.Optional;

public interface SubjectDao {

    int insert(Subject subject);

    Optional<Subject> findById(int id);

    List<Subject> findByGrade(int gradeId);

    List<Subject> findActive();
}
