package net.imanbayli.flat.booking.service.provider;

import net.imanbayli.flat.booking.exception.FlatNotFoundException;
import net.imanbayli.flat.booking.exception.IllegalTimeslotException;
import net.imanbayli.flat.booking.exception.ReservationNotFoundException;
import net.imanbayli.flat.booking.model.Flat;
import net.imanbayli.flat.booking.model.ReservationResponse;
import net.imanbayli.flat.booking.model.ReserveSlot;
import net.imanbayli.flat.booking.repository.FlatRepository;
import net.imanbayli.flat.booking.service.FlatService;
import net.imanbayli.flat.booking.service.NotificationService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ValueRange;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class FlatServiceDefaultProvider implements FlatService {
    private static final int STAR_VIEW_HOUR = 10;
    private static final int END_VIEW_HOUR = 19;
    private static final int VIEW_SLOT = 20;
    private static final int MIN_HOURS_BEFORE_BOOKING = 24;
    private static final int MAX_DAY_FUTURE_BOOKING = 7;

    private final FlatRepository flatRepository;
    private final NotificationService notificationService;

    public FlatServiceDefaultProvider(FlatRepository flatRepository,
                                      NotificationService notificationService) {
       this.flatRepository = flatRepository;
       this.notificationService = notificationService;
    }

    public ReservationResponse reserve(String flatId, ReserveSlot requestSlot){
        validateSlotDatetime(requestSlot.getDateTime());
        Flat flat = flatRepository.findById(flatId)
                .orElseThrow(() -> new FlatNotFoundException(flatId + " not found"));
        ReserveSlot reserveSlot = fillValuesForPendingSlot(requestSlot);
        Optional<ReserveSlot> existSlot = flat.getReserves().stream()
                .filter(r->r.getDateTime().equals(reserveSlot.getDateTime()))
                .findAny();
        existSlot.ifPresent(slot -> validateStatus(slot.getStatus()));
        flat.getReserves().add(reserveSlot);
        flatRepository.save(flat);
        notificationService.send(flat.getLandlord().getId(), "the reservation for your flat is pending, please approve or reject it");
        return new ReservationResponse(reserveSlot.getId());
    }

    @Override
    public ReservationResponse approve(String flatId, String reservationId) {
        Flat flat = flatRepository.findById(flatId)
                .orElseThrow(() -> new FlatNotFoundException(flatId + " not found"));

        ReserveSlot slot = flat.getReserves().stream()
                .filter(r->r.getId().equals(reservationId))
                .findAny()
                .orElseThrow(() -> new ReservationNotFoundException(reservationId+ " not found"));

        if(slot.getStatus() == ReserveSlot.Status.CANCELED) {
            throw new ReservationNotFoundException("You cannot approve this reservation, it has already been cancelled by tenant");
        }

        slot.setStatus(ReserveSlot.Status.APPROVED);
        flatRepository.save(flat);
        notificationService.send(slot.getTenantId(), "Your reservation has been approved");
        return new ReservationResponse(slot.getId());
    }

    @Override
    public ReservationResponse reject(String flatId, String reservationId) {
        Flat flat = flatRepository.findById(flatId)
                .orElseThrow(() -> new FlatNotFoundException(flatId + " not found"));

        ReserveSlot slot = flat.getReserves().stream()
                .filter(r->r.getId().equals(reservationId))
                .findAny()
                .orElseThrow(() -> new ReservationNotFoundException(reservationId+ " not found"));

        slot.setStatus(ReserveSlot.Status.REJECTED);
        flatRepository.save(flat);
        notificationService.send(slot.getTenantId(), "Your reservation has been rejected");
        return new ReservationResponse(slot.getId());
    }

    @Override
    public ReservationResponse cancel(String flatId, String reservationId) {
        Flat flat = flatRepository.findById(flatId)
                .orElseThrow(() -> new FlatNotFoundException(flatId + " not found"));

        ReserveSlot slot = flat.getReserves().stream()
                .filter(r->r.getId().equals(reservationId))
                .findAny()
                .orElseThrow(() -> new ReservationNotFoundException(reservationId+ " not found"));

        slot.setStatus(ReserveSlot.Status.CANCELED);
        flatRepository.save(flat);
        notificationService.send(flat.getLandlord().getId(), "Your reservation has been cancelled");
        return new ReservationResponse(slot.getId());
    }

    @Override
    public List<String> viewOccupiedDates(String flatId) {
        Flat flat = flatRepository.findById(flatId)
                .orElseThrow(() -> new FlatNotFoundException(flatId + " not found"));
        return flat.getReserves().stream()
                .filter(r -> r.getStatus() == ReserveSlot.Status.APPROVED || r.getStatus() == ReserveSlot.Status.PENDING)
                .map(ReserveSlot::getDateTime)
                .map(r-> r.format(DateTimeFormatter.ISO_DATE_TIME))
                .collect(Collectors.toList());
    }


    private ReserveSlot fillValuesForPendingSlot(ReserveSlot reserveSlot) {
        reserveSlot.setStatus(ReserveSlot.Status.PENDING);
        reserveSlot.setId(UUID.randomUUID().toString());
        reserveSlot.setTenantId(getUserFromConext());
        reserveSlot.setDateTime(reserveSlot.getDateTime().withSecond(0).withNano(0));
        return reserveSlot;
    }

    private void validateStatus(ReserveSlot.Status status) {
        if(status == ReserveSlot.Status.REJECTED) {
            throw new IllegalTimeslotException("You cannot book this timeslot, since it has reject been by the landlord");
        }

        if(status == ReserveSlot.Status.APPROVED || status == ReserveSlot.Status.PENDING) {
            throw new IllegalTimeslotException("You cannot book this timeslot, it has already been occupied by another tenant");
        }
    }

    private void validateSlotDatetime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();

        if (dateTime == null) {
            throw new IllegalTimeslotException("Datetime cannot be null");
        }
        if (dateTime.isBefore(now)) {
            throw new IllegalTimeslotException("You cannot book time in past");
        }

        if(Duration.between(now, dateTime).toDays() >= MAX_DAY_FUTURE_BOOKING) {
            throw new IllegalTimeslotException("You can only book for the next 7 days");
        }

        if(Duration.between(now, dateTime).toHours() < MIN_HOURS_BEFORE_BOOKING) {
            throw new IllegalTimeslotException("You cannot book appointment for time less than 24 hours");
        }

        if (dateTime.getMinute() % VIEW_SLOT != 0) {
            throw new IllegalTimeslotException("You can only book a timeslot of 20 minutes");
        }

        if (!ValueRange.of(STAR_VIEW_HOUR, END_VIEW_HOUR).isValidValue(dateTime.getHour())) {
            throw new IllegalTimeslotException("You can only book a timeslot between 10:00 and 20:00");
        }
    }

    private String getUserFromConext() {
        return "tenantUserId";
    }
}
