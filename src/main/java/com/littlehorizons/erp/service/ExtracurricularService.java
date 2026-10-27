package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.ExtracurricularDao;
import com.littlehorizons.erp.dao.StudentLookupDao;
import com.littlehorizons.erp.dao.TeacherLookupDao;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.EnrollOutcome;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.EventType;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.StudentRef;
import com.littlehorizons.erp.util.CustomExceptions.BusinessException;
import com.littlehorizons.erp.util.InputValidator;

import java.time.LocalDate;
import java.util.List;

/** Reglas de negocio de clubes, membresías, eventos y participantes. */
public class ExtracurricularService {

    public static final int MAX_CLUB_CAPACITY = 500;
    public static final List<String> POSITIONS = List.of("1er Lugar", "2do Lugar", "3er Lugar", "Participante");

    private final ExtracurricularDao dao;
    private final StudentLookupDao students;
    private final TeacherLookupDao teachers;

    public ExtracurricularService(ExtracurricularDao dao, StudentLookupDao students, TeacherLookupDao teachers) {
        this.dao = dao;
        this.students = students;
        this.teachers = teachers;
    }

    // ---------- Clubes ----------

    public List<Club> listClubs() {
        return dao.findAllClubs();
    }

    public Club getClub(int id) {
        return dao.findClubById(id)
                .orElseThrow(() -> new BusinessException("No existe un club con id " + id + "."));
    }

    public Club registerClub(String name, String description, Integer supervisorId, int maxCapacity, String schedule) {
        validateClubFields(name, supervisorId, maxCapacity, schedule);
        if (dao.findClubByName(name.trim()).isPresent()) {
            throw new BusinessException("Ya existe un club llamado " + name.trim() + ".");
        }
        int id = dao.insertClub(new Club(0, name.trim(), blankToNull(description), supervisorId, null,
                maxCapacity, blankToNull(schedule), true, 0));
        return getClub(id);
    }

    /** El cupo no puede quedar por debajo de los miembros activos actuales. */
    public Club updateClub(int id, String name, String description, Integer supervisorId, int maxCapacity,
                           String schedule, boolean active) {
        Club current = getClub(id);
        validateClubFields(name, supervisorId, maxCapacity, schedule);
        dao.findClubByName(name.trim()).ifPresent(other -> {
            if (other.id() != id) {
                throw new BusinessException("Ya existe otro club llamado " + name.trim() + ".");
            }
        });
        if (maxCapacity < current.activeMembers()) {
            throw new BusinessException("El cupo no puede ser menor a los " + current.activeMembers()
                    + " miembros activos del club.");
        }
        dao.updateClub(new Club(id, name.trim(), blankToNull(description), supervisorId, null, maxCapacity,
                blankToNull(schedule), active, current.activeMembers()));
        return getClub(id);
    }

    // ---------- Membresías ----------

    public ClubMembership enroll(int clubId, int studentId) {
        Club club = getClub(clubId);
        StudentRef student = activeStudent(studentId);
        EnrollOutcome outcome = dao.enroll(club.id(), student.id(), LocalDate.now());
        switch (outcome) {
            case CLUB_UNAVAILABLE -> throw new BusinessException("El club " + club.name() + " no está activo.");
            case ALREADY_MEMBER -> throw new BusinessException(student.fullName()
                    + " ya es miembro activo del club " + club.name() + ".");
            case CLUB_FULL -> throw new BusinessException("El club " + club.name() + " está lleno (cupo de "
                    + club.maxCapacity() + ").");
            default -> {
            }
        }
        return dao.findActiveMembership(club.id(), student.id())
                .orElseThrow(() -> new BusinessException("No se pudo leer la inscripción creada."));
    }

    public ClubMembership withdraw(int clubId, int studentId, LocalDate leaveDate, String reason) {
        Club club = getClub(clubId);
        if (!InputValidator.isNotBlank(reason)) {
            throw new BusinessException("El motivo del retiro es obligatorio.");
        }
        ClubMembership membership = dao.findActiveMembership(club.id(), studentId)
                .orElseThrow(() -> new BusinessException("El estudiante no es miembro activo del club " + club.name() + "."));
        if (leaveDate.isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha de retiro no puede ser futura.");
        }
        if (leaveDate.isBefore(membership.joinDate())) {
            throw new BusinessException("La fecha de retiro no puede ser anterior al ingreso ("
                    + membership.joinDate() + ").");
        }
        if (!dao.withdraw(club.id(), studentId, leaveDate, reason.trim())) {
            throw new BusinessException("La membresía ya había sido retirada.");
        }
        return new ClubMembership(membership.id(), membership.studentId(), membership.studentName(), club.id(),
                club.name(), membership.joinDate(), "Retirada", leaveDate, reason.trim());
    }

    public List<ClubMembership> membersOf(int clubId) {
        return dao.findMembersOfClub(getClub(clubId).id());
    }

    public List<ClubMembership> membershipsOf(int studentId) {
        return dao.findMembershipsOfStudent(existingStudent(studentId).id());
    }

    // ---------- Eventos ----------

    public List<SchoolEvent> listEvents() {
        return dao.findAllEvents();
    }

    public SchoolEvent getEvent(int id) {
        return dao.findEventById(id)
                .orElseThrow(() -> new BusinessException("No existe un evento con id " + id + "."));
    }

    public SchoolEvent registerEvent(String name, String description, EventType type, LocalDate date,
                                     String location, boolean competition, Integer organizerId) {
        if (!InputValidator.isNotBlank(name) || name.trim().length() > 150) {
            throw new BusinessException("El nombre del evento es obligatorio y admite hasta 150 caracteres.");
        }
        if (location != null && location.trim().length() > 150) {
            throw new BusinessException("El lugar admite hasta 150 caracteres.");
        }
        if (organizerId != null && teachers.findActiveById(organizerId).isEmpty()) {
            throw new BusinessException("No existe un profesor activo con id " + organizerId + ".");
        }
        int id = dao.insertEvent(new SchoolEvent(0, name.trim(), blankToNull(description), type, date,
                blankToNull(location), competition, organizerId, null, 0));
        return getEvent(id);
    }

    /** Agrega un participante; en eventos con competencia entra como Competidor, y si no, como Participante. */
    public EventParticipant addParticipant(int eventId, int studentId) {
        SchoolEvent event = getEvent(eventId);
        StudentRef student = activeStudent(studentId);
        if (dao.findParticipant(event.id(), student.id()).isPresent()) {
            throw new BusinessException(student.fullName() + " ya participa en el evento " + event.name() + ".");
        }
        dao.addParticipant(event.id(), student.id(), event.competition() ? "Competidor" : "Participante", null);
        return dao.findParticipant(event.id(), student.id())
                .orElseThrow(() -> new BusinessException("No se pudo leer la participación creada."));
    }

    /** La posición solo aplica en eventos con competencia y debe ser una de {@link #POSITIONS}. */
    public EventParticipant setPosition(int eventId, int studentId, String position) {
        SchoolEvent event = getEvent(eventId);
        if (!event.competition()) {
            throw new BusinessException("El evento " + event.name() + " no tiene competencia; no admite posiciones.");
        }
        if (!POSITIONS.contains(position)) {
            throw new BusinessException("Posición no válida. Use: " + String.join(", ", POSITIONS) + ".");
        }
        if (dao.findParticipant(event.id(), studentId).isEmpty()) {
            throw new BusinessException("El estudiante no participa en el evento " + event.name() + ".");
        }
        dao.updatePosition(event.id(), studentId, position);
        return dao.findParticipant(event.id(), studentId)
                .orElseThrow(() -> new BusinessException("No se pudo leer la participación."));
    }

    public List<EventParticipant> participantsOf(int eventId) {
        return dao.findParticipants(getEvent(eventId).id());
    }

    // ---------- Validaciones ----------

    private void validateClubFields(String name, Integer supervisorId, int maxCapacity, String schedule) {
        if (!InputValidator.isNotBlank(name) || name.trim().length() > 100) {
            throw new BusinessException("El nombre del club es obligatorio y admite hasta 100 caracteres.");
        }
        if (maxCapacity < 1 || maxCapacity > MAX_CLUB_CAPACITY) {
            throw new BusinessException("El cupo debe estar entre 1 y " + MAX_CLUB_CAPACITY + ".");
        }
        if (supervisorId != null && teachers.findActiveById(supervisorId).isEmpty()) {
            throw new BusinessException("No existe un profesor activo con id " + supervisorId + ".");
        }
        if (schedule != null && schedule.trim().length() > 100) {
            throw new BusinessException("El horario admite hasta 100 caracteres.");
        }
    }

    private StudentRef existingStudent(int studentId) {
        return students.findById(studentId)
                .orElseThrow(() -> new BusinessException("No existe el estudiante con id " + studentId + "."));
    }

    private StudentRef activeStudent(int studentId) {
        StudentRef student = existingStudent(studentId);
        if (!student.active()) {
            throw new BusinessException("El estudiante " + student.fullName() + " no está activo.");
        }
        return student;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
