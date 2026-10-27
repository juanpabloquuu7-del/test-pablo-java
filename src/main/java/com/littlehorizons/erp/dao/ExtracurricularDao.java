package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.EnrollOutcome;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.SchoolEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de clubes, membresías, eventos y participantes. */
public interface ExtracurricularDao {

    // ---------- Clubes ----------

    List<Club> findAllClubs();

    Optional<Club> findClubById(int id);

    Optional<Club> findClubByName(String name);

    int insertClub(Club club);

    /** Actualiza nombre, descripción, supervisor, cupo, horario y estado del club. */
    void updateClub(Club club);

    // ---------- Membresías ----------

    /**
     * Inscribe al estudiante en una sola transacción: bloquea la fila del club, comprueba que esté activo,
     * que el estudiante no tenga ya una membresía Activa y que haya cupo, y luego inserta la membresía.
     */
    EnrollOutcome enroll(int clubId, int studentId, LocalDate joinDate);

    Optional<ClubMembership> findActiveMembership(int clubId, int studentId);

    /** Marca la membresía Activa como Retirada. Devuelve false si el estudiante no estaba activo en el club. */
    boolean withdraw(int clubId, int studentId, LocalDate leaveDate, String reason);

    List<ClubMembership> findMembersOfClub(int clubId);

    List<ClubMembership> findMembershipsOfStudent(int studentId);

    // ---------- Eventos ----------

    int insertEvent(SchoolEvent event);

    List<SchoolEvent> findAllEvents();

    Optional<SchoolEvent> findEventById(int id);

    List<EventParticipant> findParticipants(int eventId);

    Optional<EventParticipant> findParticipant(int eventId, int studentId);

    void addParticipant(int eventId, int studentId, String role, String position);

    void updatePosition(int eventId, int studentId, String position);
}
