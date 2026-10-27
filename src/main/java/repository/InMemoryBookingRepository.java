package repository;

import domain.Booking;
import domain.Room;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemoryBookingRepository implements BookingRepository {

    private final Map<Long, Booking> storage = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Booking save(Booking booking) {
        if (booking.getId() == null) {
            booking.setId(idGenerator.getAndIncrement());
        }
        storage.put(booking.getId(), booking);
        return booking;
    }

    @Override
    public Optional<Booking> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Booking> findBookingsForRoomBetween(Room room, LocalDate from, LocalDate to) {
        return storage.values().stream()
                .filter(b -> b.getRoom() != null && Objects.equals(b.getRoom().getId(), room.getId()))
                .filter(b -> overlaps(b.getCheckIn(), b.getCheckOut(), from, to))
                .collect(Collectors.toList());
    }

    private boolean overlaps(LocalDate aStart, LocalDate aEnd, LocalDate bStart, LocalDate bEnd) {
        return !aStart.isAfter(bEnd.minusDays(1)) && !bStart.isAfter(aEnd.minusDays(1));
    }
}