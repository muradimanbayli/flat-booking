package net.imanbayli.flat.booking.service.provider;

import net.imanbayli.flat.booking.exception.FlatNotFoundException;
import net.imanbayli.flat.booking.exception.IllegalTimeslotException;
import net.imanbayli.flat.booking.exception.ReservationNotFoundException;
import net.imanbayli.flat.booking.model.Flat;
import net.imanbayli.flat.booking.model.Landlord;
import net.imanbayli.flat.booking.model.ReservationResponse;
import net.imanbayli.flat.booking.model.ReserveSlot;
import net.imanbayli.flat.booking.repository.FlatRepository;
import net.imanbayli.flat.booking.service.NotificationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;


public class FlatServiceDefaultProviderTest {
    private FlatServiceDefaultProvider service;
    private FlatRepository flatRepositoryMock;
    private NotificationService notificationServiceMock;

    @Before
    public void setup() {
        flatRepositoryMock = Mockito.mock(FlatRepository.class);
        notificationServiceMock = Mockito.mock(NotificationService.class);
        service = new FlatServiceDefaultProvider(flatRepositoryMock, notificationServiceMock);
    }

    @Test
    public void test_reserve_When_ReserveTimeInPast_Expect_IllegalTimeslotException(){
        //given
        ReserveSlot reserveSlot = new ReserveSlot();
        reserveSlot.setDateTime(LocalDateTime.now().minusDays(30));
        //when
        IllegalTimeslotException exception = assertThrows(IllegalTimeslotException.class, () -> service.reserve(null, reserveSlot));
        //expect
        assertEquals("You cannot book time in past", exception.getMessage());

    }

    @Test
    public void test_reserve_When_ReserveTimeIsNull_Expect_IllegalTimeslotException(){
        //given
        ReserveSlot reserveSlot = new ReserveSlot();
        //when
        IllegalTimeslotException exception = assertThrows(IllegalTimeslotException.class, () -> service.reserve(null, reserveSlot));
        //expect
        assertEquals("Datetime cannot be null", exception.getMessage());
    }

    @Test
    public void test_reserve_When_ReserveTimeIsLessThan24Hours_Expect_IllegalTimeslotException(){
        //given
        ReserveSlot reserveSlot = new ReserveSlot();
        reserveSlot.setDateTime(LocalDateTime.now().plusHours(23));
        //when
        IllegalTimeslotException exception = assertThrows(IllegalTimeslotException.class, () -> service.reserve(null, reserveSlot));
        //expect
        assertEquals("You cannot book appointment for time less than 24 hours", exception.getMessage());
    }

    @Test
    public void test_reserve_When_ReserveTimeNotMatch20minuteInterval_Expect_IllegalTimeslotException(){
        //given
        ReserveSlot reserveSlot = new ReserveSlot();
        reserveSlot.setDateTime(LocalDateTime.now().plusDays(4).withMinute(25));
        //when
        IllegalTimeslotException exception = assertThrows(IllegalTimeslotException.class, () -> service.reserve(null, reserveSlot));
        //expect
        assertEquals("You can only book a timeslot of 20 minutes", exception.getMessage());
    }

    @Test
    public void test_reserve_When_ReserveTimeNotInValidViewHours_Expect_IllegalTimeslotException(){
        //given
        ReserveSlot reserveSlot = new ReserveSlot();
        reserveSlot.setDateTime(LocalDateTime.now().plusDays(4).withMinute(20).withHour(21));
        //when
        IllegalTimeslotException exception = assertThrows(IllegalTimeslotException.class, () -> service.reserve(null, reserveSlot));
        //expect
        assertEquals("You can only book a timeslot between 10:00 and 20:00", exception.getMessage());
    }


    @Test
    public void test_reserve_When_ReserveTimeMoreThan7Days_Expect_IllegalTimeslotException(){
        //given
        ReserveSlot reserveSlot = new ReserveSlot();
        reserveSlot.setDateTime(LocalDateTime.now().plusDays(8).withMinute(20).withHour(14));
        //when
        IllegalTimeslotException exception = assertThrows(IllegalTimeslotException.class, () -> service.reserve(null, reserveSlot));

        //expect
        assertEquals("You can only book for the next 7 days", exception.getMessage());
    }

    @Test
    public void test_reserve_When_FlatIdIsNotValid_Expect_FlatNotFoundException(){
        //given
        String flatId = "f1";
        ReserveSlot reserveSlot = new ReserveSlot();
        reserveSlot.setDateTime(LocalDateTime.now().plusDays(3).withMinute(20).withHour(14));
        Mockito.when(flatRepositoryMock.findById(flatId)).thenReturn(Optional.empty());
        //when
        FlatNotFoundException exception = assertThrows(FlatNotFoundException.class, () -> service.reserve(flatId, reserveSlot));

        //expect
        assertEquals("f1 not found", exception.getMessage());
    }

    @Test
    public void test_reserve_When_BookingTimeIsValid_Expect_BookedTimeSlot(){
        //given
        String flatId = "f1";
        ReserveSlot reserveSlot = new ReserveSlot();
        reserveSlot.setDateTime(LocalDateTime.now().plusDays(3).withMinute(20).withHour(14));
        Flat flat = new Flat();
        flat.setId(flatId);
        flat.setLandlord(Landlord.of(UUID.randomUUID().toString()));
        Mockito.when(flatRepositoryMock.findById(flatId)).thenReturn(Optional.of(flat));
        //when
        service.reserve(flatId, reserveSlot);
        //expect
        ArgumentCaptor<Flat> flatCaptor = ArgumentCaptor.forClass(Flat.class);
        Mockito.verify(flatRepositoryMock).save(flatCaptor.capture());
        assertEquals(1, flatCaptor.getValue().getReserves().size());
        ReserveSlot slot = flatCaptor.getValue().getReserves().get(0);
        assertEquals(ReserveSlot.Status.PENDING, slot.getStatus());
        assertEquals(reserveSlot.getDateTime().withSecond(0).withNano(0), slot.getDateTime());
        Mockito.verify(notificationServiceMock).send(flat.getLandlord().getId(), "the reservation for your flat is pending, please approve or reject it");
    }

    @Test
    public void test_reserve_When_TimeslotRejectedByLandlord_Expect_IllegalTimeslotException(){
        //given
        String flatId = "f1";
        ReserveSlot reserveSlot = new ReserveSlot();
        reserveSlot.setDateTime(LocalDateTime.now().plusDays(3).withMinute(20).withHour(14));
        Flat flat = new Flat();
        ReserveSlot slot = new ReserveSlot();
        slot.setDateTime(reserveSlot.getDateTime().withNano(0).withSecond(0));
        slot.setStatus(ReserveSlot.Status.REJECTED);
        flat.getReserves().add(slot);
        Mockito.when(flatRepositoryMock.findById(flatId)).thenReturn(Optional.of(flat));
        //when
        IllegalTimeslotException exception = assertThrows(IllegalTimeslotException.class, () -> service.reserve(flatId, reserveSlot));

        //expect
        assertEquals("You cannot book this timeslot, since it has reject been by the landlord", exception.getMessage());
    }

    @Test
    public void test_reserve_When_TimeslotOccupiedByAnotherTenant_Expect_IllegalTimeslotException(){
        //given
        String flatId = "f1";
        ReserveSlot reserveSlot = new ReserveSlot();
        reserveSlot.setDateTime(LocalDateTime.now().plusDays(3).withMinute(20).withHour(14));
        Flat flat = new Flat();
        ReserveSlot slot = new ReserveSlot();
        slot.setDateTime(reserveSlot.getDateTime().withNano(0).withSecond(0));
        slot.setStatus(ReserveSlot.Status.APPROVED);
        flat.getReserves().add(slot);
        Mockito.when(flatRepositoryMock.findById(flatId)).thenReturn(Optional.of(flat));
        //when
        IllegalTimeslotException exception = assertThrows(IllegalTimeslotException.class, () -> service.reserve(flatId, reserveSlot));

        //expect
        assertEquals("You cannot book this timeslot, it has already been occupied by another tenant", exception.getMessage());
    }

    @Test
    public void test_viewOccupiedDates_When_FlatIdIsNotValid_Expect_FlatNotFoundException(){
        //given
        String flatId = "f1";
        Mockito.when(flatRepositoryMock.findById(flatId)).thenReturn(Optional.empty());
        //when
        FlatNotFoundException exception = assertThrows(FlatNotFoundException.class, () -> service.viewOccupiedDates(flatId));
        //expect
        assertEquals("f1 not found", exception.getMessage());
    }

    @Test
    public void test_viewOccupiedDates_When_FlatIdValidAndThereIsApprovedStatus_Expect_ReturnTimeslotInString(){
        //given
        String flatId = "f1";
        Flat flat = new Flat();
        ReserveSlot slot = new ReserveSlot();
        slot.setDateTime(LocalDateTime.of(2021, 01, 01, 16, 20));
        slot.setStatus(ReserveSlot.Status.APPROVED);
        flat.getReserves().add(slot);
        Mockito.when(flatRepositoryMock.findById(flatId)).thenReturn(Optional.of(flat));
        //when
        List<String> dates = service.viewOccupiedDates(flatId);
        //expect
        assertEquals(1, dates.size());
        assertEquals("2021-01-01T16:20:00", dates.get(0));
    }

    @Test
    public void test_approve_When_FlatIdIsNotValid_Expect_FlatNotFoundException(){
        //given
        String flatId = "f1";
        String reservationId = "r1";
        Mockito.when(flatRepositoryMock.findById(flatId)).thenReturn(Optional.empty());
        //when
        FlatNotFoundException exception = assertThrows(FlatNotFoundException.class, () -> service.approve(flatId, reservationId));
        //expect
        assertEquals("f1 not found", exception.getMessage());
    }

    @Test
    public void test_approve_When_ReservationIdIsNotValid_Expect_ReservationNotFoundException(){
        //given
        String flatId = "f1";
        String reservationId = "r1";
        Mockito.when(flatRepositoryMock.findById(flatId)).thenReturn(Optional.of(new Flat()));
        //when
        ReservationNotFoundException exception = assertThrows(ReservationNotFoundException.class, () -> service.approve(flatId, reservationId));
        //expect
        assertEquals("r1 not found", exception.getMessage());
    }

    @Test
    public void test_approve_When_ReservationStatusCancelled_Expect_ReservationNotFoundException(){
        //given
        String flatId = "f1";
        String reservationId = "r1";
        Flat flat = new Flat();
        ReserveSlot slot = new ReserveSlot();
        slot.setDateTime(LocalDateTime.of(2021, 01, 01, 16, 20));
        slot.setStatus(ReserveSlot.Status.CANCELED);
        slot.setId(reservationId);
        flat.getReserves().add(slot);
        Mockito.when(flatRepositoryMock.findById(flatId)).thenReturn(Optional.of(flat));
        //when
        ReservationNotFoundException exception = assertThrows(ReservationNotFoundException.class, () -> service.approve(flatId, reservationId));
        //expect
        assertEquals("You cannot approve this reservation, it has already been cancelled by tenant", exception.getMessage());
    }

    @Test
    public void test_approve_When_ReservationStatusPending_Expect_reservationShouldApprove(){
        //given
        String flatId = "f1";
        String reservationId = "r1";
        Flat flat = new Flat();
        ReserveSlot slot = new ReserveSlot();
        slot.setDateTime(LocalDateTime.of(2021, 01, 01, 16, 20));
        slot.setStatus(ReserveSlot.Status.PENDING);
        slot.setId(reservationId);
        slot.setTenantId("t1");
        flat.getReserves().add(slot);
        Mockito.when(flatRepositoryMock.findById(flatId)).thenReturn(Optional.of(flat));
        //when
        ReservationResponse response = service.approve(flatId, reservationId);
        //expect
        assertNotNull(response.getId());
        Mockito.verify(notificationServiceMock).send("t1", "Your reservation has been approved");
        ArgumentCaptor<Flat> flatCaptor = ArgumentCaptor.forClass(Flat.class);
        Mockito.verify(flatRepositoryMock).save(flatCaptor.capture());
        assertEquals(flatCaptor.getValue().getReserves().get(0).getStatus(), ReserveSlot.Status.APPROVED);
    }

    @Test
    public void test_cancel_When_ReservationExist_Expect_reservationShouldCancel(){
        //given
        String flatId = "f1";
        String reservationId = "r1";
        Flat flat = new Flat();
        flat.setLandlord(new Landlord("L1", null, null));
        ReserveSlot slot = new ReserveSlot();
        slot.setDateTime(LocalDateTime.of(2021, 01, 01, 16, 20));
        slot.setStatus(ReserveSlot.Status.PENDING);
        slot.setId(reservationId);
        slot.setTenantId("t1");
        flat.getReserves().add(slot);
        Mockito.when(flatRepositoryMock.findById(flatId)).thenReturn(Optional.of(flat));
        //when
        ReservationResponse response = service.cancel(flatId, reservationId);
        //expect
        assertNotNull(response.getId());
        Mockito.verify(notificationServiceMock).send("L1", "Your reservation has been cancelled");
        ArgumentCaptor<Flat> flatCaptor = ArgumentCaptor.forClass(Flat.class);
        Mockito.verify(flatRepositoryMock).save(flatCaptor.capture());
        assertEquals(flatCaptor.getValue().getReserves().get(0).getStatus(), ReserveSlot.Status.CANCELED);
    }

    @Test
    public void test_reject_When_ReservationExist_Expect_reservationShouldReject(){
        //given
        String flatId = "f1";
        String reservationId = "r1";
        Flat flat = new Flat();
        flat.setLandlord(new Landlord("L1", null, null));
        ReserveSlot slot = new ReserveSlot();
        slot.setDateTime(LocalDateTime.of(2021, 01, 01, 16, 20));
        slot.setStatus(ReserveSlot.Status.PENDING);
        slot.setId(reservationId);
        slot.setTenantId("t1");
        flat.getReserves().add(slot);
        Mockito.when(flatRepositoryMock.findById(flatId)).thenReturn(Optional.of(flat));
        //when
        ReservationResponse response = service.reject(flatId, reservationId);
        //expect
        assertNotNull(response.getId());
        Mockito.verify(notificationServiceMock).send("t1", "Your reservation has been rejected");
        ArgumentCaptor<Flat> flatCaptor = ArgumentCaptor.forClass(Flat.class);
        Mockito.verify(flatRepositoryMock).save(flatCaptor.capture());
        assertEquals(flatCaptor.getValue().getReserves().get(0).getStatus(), ReserveSlot.Status.REJECTED);
    }

}