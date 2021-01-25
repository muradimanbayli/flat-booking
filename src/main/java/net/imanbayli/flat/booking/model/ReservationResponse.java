package net.imanbayli.flat.booking.model;

public class ReservationResponse {
    private String id;

    public ReservationResponse() {
    }

    public ReservationResponse(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
