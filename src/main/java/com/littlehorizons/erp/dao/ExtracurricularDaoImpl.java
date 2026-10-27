package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.EnrollOutcome;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.EventType;
import com.littlehorizons.erp.model.SchoolEvent;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ExtracurricularDaoImpl extends BaseJdbcDao implements ExtracurricularDao {

    private static final String STUDENT_NAME =
            "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido)";
    private static final String TEACHER_NAME =
            "CONCAT_WS(' ', p.primer_nombre, p.segundo_nombre, p.primer_apellido, p.segundo_apellido)";

    private static final String SELECT_CLUB = "SELECT c.*, " + TEACHER_NAME + " AS supervisor, "
            + "(SELECT COUNT(*) FROM membresias_club m WHERE m.id_club = c.id_club "
            + "AND m.estado_membresia = 'Activa') AS miembros "
            + "FROM clubes c LEFT JOIN profesores p ON p.id_profesor = c.id_profesor_supervisor ";

    private static final String SELECT_MEMBERSHIP = "SELECT m.*, c.nombre_club, " + STUDENT_NAME + " AS estudiante "
            + "FROM membresias_club m JOIN clubes c ON c.id_club = m.id_club "
            + "JOIN estudiantes e ON e.id_estudiante = m.id_estudiante ";

    private static final String SELECT_EVENT = "SELECT v.*, " + TEACHER_NAME + " AS organizador, "
            + "(SELECT COUNT(*) FROM participantes_evento pe WHERE pe.id_evento = v.id_evento) AS participantes "
            + "FROM eventos v LEFT JOIN profesores p ON p.id_profesor = v.id_organizador ";

    private static final String SELECT_PARTICIPANT = "SELECT pe.*, " + STUDENT_NAME + " AS estudiante "
            + "FROM participantes_evento pe JOIN estudiantes e ON e.id_estudiante = pe.id_estudiante ";

    // ---------- Clubes ----------

    @Override
    public List<Club> findAllClubs() {
        return query(SELECT_CLUB + "ORDER BY c.nombre_club", this::mapClub);
    }

    @Override
    public Optional<Club> findClubById(int id) {
        return queryOne(SELECT_CLUB + "WHERE c.id_club = ?", this::mapClub, id);
    }

    @Override
    public Optional<Club> findClubByName(String name) {
        return queryOne(SELECT_CLUB + "WHERE c.nombre_club = ?", this::mapClub, name);
    }

    @Override
    public int insertClub(Club c) {
        return insert("INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, capacidad_maxima, "
                        + "horario_reunion, esta_activo) VALUES (?, ?, ?, ?, ?, ?)",
                c.name(), c.description(), c.supervisorId(), c.maxCapacity(), c.schedule(), c.active());
    }

    @Override
    public void updateClub(Club c) {
        update("UPDATE clubes SET nombre_club = ?, descripcion = ?, id_profesor_supervisor = ?, "
                        + "capacidad_maxima = ?, horario_reunion = ?, esta_activo = ? WHERE id_club = ?",
                c.name(), c.description(), c.supervisorId(), c.maxCapacity(), c.schedule(), c.active(), c.id());
    }

    // ---------- Membresías ----------

    @Override
    public EnrollOutcome enroll(int clubId, int studentId, LocalDate joinDate) {
        return inTransaction(c -> {
            List<int[]> club = query(c, "SELECT capacidad_maxima, esta_activo FROM clubes WHERE id_club = ? FOR UPDATE",
                    rs -> new int[]{rs.getInt(1), rs.getInt(2)}, clubId);
            if (club.isEmpty() || club.get(0)[1] == 0) {
                return EnrollOutcome.CLUB_UNAVAILABLE;
            }
            List<Integer> already = query(c, "SELECT 1 FROM membresias_club WHERE id_club = ? "
                    + "AND id_estudiante = ? AND estado_membresia = 'Activa'", rs -> rs.getInt(1), clubId, studentId);
            if (!already.isEmpty()) {
                return EnrollOutcome.ALREADY_MEMBER;
            }
            int members = query(c, "SELECT COUNT(*) FROM membresias_club WHERE id_club = ? "
                    + "AND estado_membresia = 'Activa'", rs -> rs.getInt(1), clubId).get(0);
            if (members >= club.get(0)[0]) {
                return EnrollOutcome.CLUB_FULL;
            }
            insert(c, "INSERT INTO membresias_club (id_estudiante, id_club, fecha_ingreso, estado_membresia) "
                    + "VALUES (?, ?, ?, 'Activa')", studentId, clubId, Date.valueOf(joinDate));
            return EnrollOutcome.ENROLLED;
        });
    }

    @Override
    public Optional<ClubMembership> findActiveMembership(int clubId, int studentId) {
        return queryOne(SELECT_MEMBERSHIP + "WHERE m.id_club = ? AND m.id_estudiante = ? "
                + "AND m.estado_membresia = 'Activa'", this::mapMembership, clubId, studentId);
    }

    @Override
    public boolean withdraw(int clubId, int studentId, LocalDate leaveDate, String reason) {
        return update("UPDATE membresias_club SET estado_membresia = 'Retirada', fecha_retiro = ?, "
                        + "motivo_retiro = ? WHERE id_club = ? AND id_estudiante = ? AND estado_membresia = 'Activa'",
                Date.valueOf(leaveDate), reason, clubId, studentId) > 0;
    }

    @Override
    public List<ClubMembership> findMembersOfClub(int clubId) {
        return query(SELECT_MEMBERSHIP + "WHERE m.id_club = ? ORDER BY m.estado_membresia, estudiante",
                this::mapMembership, clubId);
    }

    @Override
    public List<ClubMembership> findMembershipsOfStudent(int studentId) {
        return query(SELECT_MEMBERSHIP + "WHERE m.id_estudiante = ? ORDER BY m.estado_membresia, c.nombre_club",
                this::mapMembership, studentId);
    }

    // ---------- Eventos ----------

    @Override
    public int insertEvent(SchoolEvent e) {
        return insert("INSERT INTO eventos (nombre_evento, descripcion, tipo_evento, fecha_evento, lugar, "
                        + "tiene_competencia, id_organizador) VALUES (?, ?, ?, ?, ?, ?, ?)",
                e.name(), e.description(), e.type().label(), Date.valueOf(e.date()), e.location(),
                e.competition(), e.organizerId());
    }

    @Override
    public List<SchoolEvent> findAllEvents() {
        return query(SELECT_EVENT + "ORDER BY v.fecha_evento DESC, v.nombre_evento", this::mapEvent);
    }

    @Override
    public Optional<SchoolEvent> findEventById(int id) {
        return queryOne(SELECT_EVENT + "WHERE v.id_evento = ?", this::mapEvent, id);
    }

    @Override
    public List<EventParticipant> findParticipants(int eventId) {
        return query(SELECT_PARTICIPANT + "WHERE pe.id_evento = ? ORDER BY pe.posicion_obtenida IS NULL, "
                + "pe.posicion_obtenida, estudiante", this::mapParticipant, eventId);
    }

    @Override
    public Optional<EventParticipant> findParticipant(int eventId, int studentId) {
        return queryOne(SELECT_PARTICIPANT + "WHERE pe.id_evento = ? AND pe.id_estudiante = ?",
                this::mapParticipant, eventId, studentId);
    }

    @Override
    public void addParticipant(int eventId, int studentId, String role, String position) {
        insert("INSERT INTO participantes_evento (id_evento, id_estudiante, rol_participacion, posicion_obtenida) "
                + "VALUES (?, ?, ?, ?)", eventId, studentId, role, position);
    }

    @Override
    public void updatePosition(int eventId, int studentId, String position) {
        update("UPDATE participantes_evento SET posicion_obtenida = ? WHERE id_evento = ? AND id_estudiante = ?",
                position, eventId, studentId);
    }

    // ---------- Mapeo ----------

    private static LocalDate toLocal(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Club mapClub(ResultSet rs) throws SQLException {
        int supervisor = rs.getInt("id_profesor_supervisor");
        Integer supervisorId = rs.wasNull() ? null : supervisor;
        return new Club(rs.getInt("id_club"), rs.getString("nombre_club"), rs.getString("descripcion"),
                supervisorId, blankToNull(rs.getString("supervisor")), rs.getInt("capacidad_maxima"),
                rs.getString("horario_reunion"), rs.getBoolean("esta_activo"), rs.getInt("miembros"));
    }

    private ClubMembership mapMembership(ResultSet rs) throws SQLException {
        return new ClubMembership(rs.getInt("id_membresia"), rs.getInt("id_estudiante"), rs.getString("estudiante"),
                rs.getInt("id_club"), rs.getString("nombre_club"), toLocal(rs.getDate("fecha_ingreso")),
                rs.getString("estado_membresia"), toLocal(rs.getDate("fecha_retiro")), rs.getString("motivo_retiro"));
    }

    private SchoolEvent mapEvent(ResultSet rs) throws SQLException {
        int organizer = rs.getInt("id_organizador");
        Integer organizerId = rs.wasNull() ? null : organizer;
        return new SchoolEvent(rs.getInt("id_evento"), rs.getString("nombre_evento"), rs.getString("descripcion"),
                EventType.fromLabel(rs.getString("tipo_evento")), toLocal(rs.getDate("fecha_evento")),
                rs.getString("lugar"), rs.getBoolean("tiene_competencia"), organizerId,
                blankToNull(rs.getString("organizador")), rs.getInt("participantes"));
    }

    private EventParticipant mapParticipant(ResultSet rs) throws SQLException {
        return new EventParticipant(rs.getInt("id_participacion"), rs.getInt("id_evento"), rs.getInt("id_estudiante"),
                rs.getString("estudiante"), rs.getString("rol_participacion"), rs.getString("posicion_obtenida"));
    }
}
