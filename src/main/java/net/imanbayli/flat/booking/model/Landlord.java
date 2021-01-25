package net.imanbayli.flat.booking.model;

import java.util.List;

public class Landlord {
    private String id;
    private String firstName;
    private String lastName;
    private List<Flat> flats;

    public Landlord() {
    }

    public Landlord(String id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public static Landlord of(String id) {
        Landlord landlord = new Landlord();
        landlord.setId(id);
        return landlord;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public List<Flat> getFlats() {
        return flats;
    }

    public void setFlats(List<Flat> flats) {
        this.flats = flats;
    }
}
