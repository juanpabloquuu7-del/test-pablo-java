package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.service.ExtracurricularService;
import com.littlehorizons.erp.view.ExtracurricularView;

import java.time.LocalDate;

/** Módulo 4 del menú: clubes, membresías, eventos y participantes. */
public class ExtracurricularController extends BaseController implements MenuModule {

    private final ExtracurricularView extraView;
    private final ExtracurricularService service;

    public ExtracurricularController(ExtracurricularView view, ExtracurricularService service) {
        super(view);
        this.extraView = view;
        this.service = service;
    }

    @Override
    public int position() {
        return 4;
    }

    @Override
    public void run() {
        while (true) {
            int option = extraView.showExtracurricularMenu();
            if (option == 0) {
                return;
            }
            safely(() -> execute(option));
        }
    }

    private void execute(int option) {
        switch (option) {
            case 1 -> registerClub();
            case 2 -> editClub();
            case 3 -> extraView.showClubs(service.listClubs());
            case 4 -> enroll();
            case 5 -> withdraw();
            case 6 -> showMembers();
            case 7 -> showStudentClubs();
            case 8 -> registerEvent();
            case 9 -> extraView.showEvents(service.listEvents());
            case 10 -> addParticipant();
            case 11 -> setPosition();
            case 12 -> showParticipants();
            default -> extraView.showError("Opción no válida.");
        }
    }

    private void registerClub() {
        String name = extraView.readRequired("Nombre del club: ");
        String description = extraView.readOptional("Descripción");
        int capacity = extraView.readInt("Cupo máximo (1 a " + ExtracurricularService.MAX_CLUB_CAPACITY + "): ",
                1, ExtracurricularService.MAX_CLUB_CAPACITY);
        Integer teacherId = extraView.readOptionalTeacherId("Id del profesor encargado");
        String schedule = extraView.readOptional("Horario de reunión (ej. Martes 3-5 PM)");
        Club club = service.registerClub(name, description, teacherId, capacity, schedule);
        extraView.showSuccess("Club " + club.name() + " registrado con id " + club.id() + ".");
    }

    private void editClub() {
        Club current = service.getClub(extraView.readId("del club"));
        extraView.showClubDetail(current);
        extraView.showInfo("Escriba los nuevos datos; Enter conserva el valor actual.");
        String name = extraView.readOptional("Nombre [" + current.name() + "]");
        String description = extraView.readOptional("Descripción");
        int capacity = current.maxCapacity();
        if (extraView.readYesNo("¿Cambiar el cupo? Actual: " + current.maxCapacity())) {
            capacity = extraView.readInt("Nuevo cupo (1 a " + ExtracurricularService.MAX_CLUB_CAPACITY + "): ",
                    1, ExtracurricularService.MAX_CLUB_CAPACITY);
        }
        Integer teacherId = current.supervisorId();
        if (extraView.readYesNo("¿Cambiar el profesor encargado?")) {
            teacherId = extraView.readOptionalTeacherId("Id del nuevo profesor (Enter para quitarlo)");
        }
        String schedule = extraView.readOptional("Horario de reunión");
        boolean active = extraView.readYesNo("¿El club queda activo?");
        Club updated = service.updateClub(current.id(),
                name != null ? name : current.name(),
                description != null ? description : current.description(),
                teacherId, capacity,
                schedule != null ? schedule : current.schedule(), active);
        extraView.showSuccess("Club " + updated.name() + " actualizado.");
    }

    private void enroll() {
        extraView.showClubs(service.listClubs());
        int clubId = extraView.readId("del club");
        int studentId = extraView.readId("del estudiante");
        ClubMembership m = service.enroll(clubId, studentId);
        extraView.showSuccess(m.studentName() + " quedó inscrito en " + m.clubName() + ".");
    }

    private void withdraw() {
        int clubId = extraView.readId("del club");
        int studentId = extraView.readId("del estudiante");
        LocalDate date = LocalDate.now();
        if (!extraView.readYesNo("¿El retiro es hoy (" + date + ")?")) {
            date = extraView.readDate("Fecha de retiro");
        }
        String reason = extraView.readRequired("Motivo del retiro: ");
        ClubMembership m = service.withdraw(clubId, studentId, date, reason);
        extraView.showSuccess(m.studentName() + " fue retirado de " + m.clubName() + " (estado Retirada).");
    }

    private void showMembers() {
        extraView.showMemberships(service.membersOf(extraView.readId("del club")));
    }

    private void showStudentClubs() {
        extraView.showMemberships(service.membershipsOf(extraView.readId("del estudiante")));
    }

    private void registerEvent() {
        String name = extraView.readRequired("Nombre del evento: ");
        String description = extraView.readOptional("Descripción");
        var type = extraView.chooseEventType();
        LocalDate date = extraView.readDate("Fecha del evento");
        String location = extraView.readOptional("Lugar");
        boolean competition = extraView.readYesNo("¿Es un evento con competencia?");
        Integer organizerId = extraView.readOptionalTeacherId("Id del profesor organizador");
        SchoolEvent event = service.registerEvent(name, description, type, date, location, competition, organizerId);
        extraView.showSuccess("Evento " + event.name() + " registrado con id " + event.id() + ".");
    }

    private void addParticipant() {
        extraView.showEvents(service.listEvents());
        int eventId = extraView.readId("del evento");
        int studentId = extraView.readId("del estudiante");
        EventParticipant p = service.addParticipant(eventId, studentId);
        extraView.showSuccess(p.studentName() + " quedó registrado como " + p.role() + ".");
    }

    private void setPosition() {
        int eventId = extraView.readId("del evento");
        extraView.showParticipants(service.participantsOf(eventId));
        int studentId = extraView.readId("del estudiante");
        String position = extraView.choosePosition(ExtracurricularService.POSITIONS);
        EventParticipant p = service.setPosition(eventId, studentId, position);
        extraView.showSuccess("Posición de " + p.studentName() + ": " + p.position() + ".");
    }

    private void showParticipants() {
        extraView.showParticipants(service.participantsOf(extraView.readId("del evento")));
    }
}
