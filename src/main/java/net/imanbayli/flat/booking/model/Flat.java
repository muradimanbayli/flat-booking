package net.imanbayli.flat.booking.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.List;

public class Flat {
    private String id;
    private String shortDescription;
    private String address;
    private List<ReserveSlot> reservedSlots;
    private Landlord landlord;

    public Flat() {
        reservedSlots = new ArrayList<>();
    }

    public Flat(String id, String shortDescription, String address) {
        this();
        this.id = id;
        this.shortDescription = shortDescription;
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<ReserveSlot> getReserves() {
        return reservedSlots;
    }

    public void setReserves(List<ReserveSlot> reserves) {
        this.reservedSlots = reserves;
    }

    public Landlord getLandlord() {
        return landlord;
    }

    public void setLandlord(Landlord landlord) {
        this.landlord = landlord;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Flat flat = (Flat) o;
        return id.equals(flat.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Flat{" +
                "id='" + id + '\'' +
                ", shortDescription='" + shortDescription + '\'' +
                ", address='" + address + '\'' +
                ", reservedSlots=" + reservedSlots +
                ", landlord=" + landlord +
                '}';
    }
}
