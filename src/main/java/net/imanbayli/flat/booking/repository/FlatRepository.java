package net.imanbayli.flat.booking.repository;

import net.imanbayli.flat.booking.model.Flat;

import java.util.Optional;

public interface FlatRepository {
    Optional<Flat> findById(String id);
    void save(Flat flat);
}
