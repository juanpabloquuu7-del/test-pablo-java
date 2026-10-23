package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Teacher;

import java.util.List;
import java.util.Optional;

public interface TeacherDao {

    int insert(Teacher teacher);

    int update(Teacher teacher);

    Optional<Teacher> findById(int id);

    Optional<Teacher> findByDocument(String documentNumber);

    Optional<Teacher> findByEmail(String email);

    List<Teacher> findAllActive();

    List<Teacher> findAll();

    int updateStatus(int teacherId, String status);
}
