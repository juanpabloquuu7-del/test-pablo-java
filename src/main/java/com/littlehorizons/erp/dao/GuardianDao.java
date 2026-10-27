package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.GuardianLink;

import java.util.List;
import java.util.Optional;

public interface GuardianDao {

    int insert(Guardian guardian);

    int update(Guardian guardian);

    Optional<Guardian> findById(int id);

    Optional<Guardian> findByDocument(String documentNumber);

    List<GuardianLink> findByStudent(int studentId);

    void link(int studentId, int guardianId, boolean primary, boolean emergencyContact);

    void unlink(int studentId, int guardianId);

    /** Deja como único principal al acudiente indicado (transacción). */
    void setPrimary(int studentId, int guardianId);

    int countStudents(int guardianId);
}
