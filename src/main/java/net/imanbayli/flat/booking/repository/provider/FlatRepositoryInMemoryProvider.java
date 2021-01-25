package net.imanbayli.flat.booking.repository.provider;

import net.imanbayli.flat.booking.model.Flat;
import net.imanbayli.flat.booking.repository.FlatRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FlatRepositoryInMemoryProvider implements FlatRepository {
    private static Map<String, Flat> data = new HashMap<>();

    @Override
    public Optional<Flat> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public void save(Flat flat) {
        data.put(flat.getId(), flat);
    }


}
