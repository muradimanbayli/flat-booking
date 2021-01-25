package net.imanbayli.flat.booking.service;

import net.imanbayli.flat.booking.model.ReservationResponse;
import net.imanbayli.flat.booking.model.ReserveSlot;

import java.time.LocalDateTime;
import java.util.List;

public interface FlatService {
    ReservationResponse reserve(String flatId, ReserveSlot requestSlot);
    ReservationResponse approve(String flatId, String reservationId);
    ReservationResponse reject(String flatId, String reservationId);
    ReservationResponse cancel(String flatId, String reservationId);
    List<String> viewOccupiedDates(String flatId);

}
