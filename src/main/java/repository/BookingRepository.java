package repository;
import domain.Booking;
import domain.Room;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {
    Booking save(Booking booking);

    Optional<Booking> findById(Long id);

    List<Booking> findBookingsForRoomBetween(Room room, LocalDate from, LocalDate to);
}
