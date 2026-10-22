package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.EventType;
import com.littlehorizons.erp.model.SchoolEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Entrada y salida de consola del módulo de actividades extracurriculares. */
public class ExtracurricularView extends BaseView {

    public ExtracurricularView(Scanner scanner) {
        super(scanner);
    }

    public int showExtracurricularMenu() {
        showMenu("Actividades Extracurriculares",
                "Registrar club",
                "Editar club",
                "Listar clubes",
                "Inscribir estudiante en un club",
                "Retirar estudiante de un club",
                "Miembros de un club",
                "Clubes de un estudiante",
                "Registrar evento",
                "Listar eventos",
                "Agregar participante a un evento",
                "Registrar posición en un evento",
                "Participantes de un evento");
        return readOption(12);
    }

    public int readId(String what) {
        return readInt("Id " + what + ": ", 1, Integer.MAX_VALUE);
    }

    /** Id de profesor opcional: Enter lo omite (devuelve null). */
    public Integer readOptionalTeacherId(String prompt) {
        while (true) {
            String text = readOptional(prompt);
            if (text == null) {
                return null;
            }
            try {
                int id = Integer.parseInt(text);
                if (id >= 1) {
                    return id;
                }
            } catch (NumberFormatException e) {
                // cae al mensaje de error
            }
            showError("Ingrese un id de profesor válido o presione Enter para omitir.");
        }
    }

    public EventType chooseEventType() {
        List<String> labels = new ArrayList<>();
        for (EventType type : EventType.values()) {
            labels.add(type.label());
        }
        return EventType.values()[readChoice("Tipo de evento: ", labels)];
    }

    public String choosePosition(List<String> positions) {
        return positions.get(readChoice("Posición: ", positions));
    }

    public void showClubs(List<Club> clubs) {
        List<String[]> rows = new ArrayList<>();
        for (Club c : clubs) {
            rows.add(new String[]{String.valueOf(c.id()), c.name(), c.supervisorName() == null ? "-" : c.supervisorName(),
                    c.activeMembers() + "/" + c.maxCapacity(), c.schedule() == null ? "-" : c.schedule(),
                    c.active() ? "Activo" : "Inactivo"});
        }
        printTable(new String[]{"Id", "Club", "Profesor encargado", "Miembros/Cupo", "Horario", "Estado"}, rows);
    }

    public void showClubDetail(Club c) {
        System.out.println("Club:          " + c.name() + " (id " + c.id() + ")");
        System.out.println("Descripción:   " + (c.description() == null ? "-" : c.description()));
        System.out.println("Encargado:     " + (c.supervisorName() == null ? "-" : c.supervisorName()));
        System.out.println("Cupo:          " + c.activeMembers() + " de " + c.maxCapacity());
        System.out.println("Horario:       " + (c.schedule() == null ? "-" : c.schedule()));
        System.out.println("Estado:        " + (c.active() ? "Activo" : "Inactivo"));
    }

    public void showMemberships(List<ClubMembership> memberships) {
        List<String[]> rows = new ArrayList<>();
        for (ClubMembership m : memberships) {
            rows.add(new String[]{String.valueOf(m.studentId()), m.studentName(), m.clubName(),
                    m.joinDate().toString(), m.status(),
                    m.leaveDate() == null ? "-" : m.leaveDate().toString(),
                    m.leaveReason() == null ? "-" : m.leaveReason()});
        }
        printTable(new String[]{"Est.", "Estudiante", "Club", "Ingreso", "Estado", "Retiro", "Motivo"}, rows);
    }

    public void showEvents(List<SchoolEvent> events) {
        List<String[]> rows = new ArrayList<>();
        for (SchoolEvent e : events) {
            rows.add(new String[]{String.valueOf(e.id()), e.name(), e.type().label(), e.date().toString(),
                    e.location() == null ? "-" : e.location(), e.competition() ? "Sí" : "No",
                    String.valueOf(e.participants())});
        }
        printTable(new String[]{"Id", "Evento", "Tipo", "Fecha", "Lugar", "Competencia", "Particip."}, rows);
    }

    public void showParticipants(List<EventParticipant> participants) {
        List<String[]> rows = new ArrayList<>();
        for (EventParticipant p : participants) {
            rows.add(new String[]{String.valueOf(p.studentId()), p.studentName(), p.role(),
                    p.position() == null ? "-" : p.position()});
        }
        printTable(new String[]{"Est.", "Estudiante", "Rol", "Posición"}, rows);
    }
}
