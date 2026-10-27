package service;
import domain.Booking;
import domain.Guest;
import domain.Room;
import repository.BookingRepository;

import java.time.LocalDate;
import java.util.List;

public class HotelService {
    private final BookingRepository bookingRepository; // DI

    public HotelService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public Booking createBooking(Long idIfNeeded,
                                 Guest guest,
                                 Room room,
                                 LocalDate checkIn,
                                 LocalDate checkOut) {

        validate(guest, room, checkIn, checkOut);

        List<Booking> conflicts = bookingRepository
                .findBookingsForRoomBetween(room, checkIn, checkOut);

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Room is already booked for given dates.");
        }

        Booking booking = new Booking();
        booking.setId(idIfNeeded);
        booking.setGuest(guest);
        booking.setRoom(room);
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);

        return bookingRepository.save(booking);
    }

    public Booking getBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));
    }

    private void validate(Guest guest, Room room, LocalDate checkIn, LocalDate checkOut) {
        if (guest == null || room == null || checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Guest, room and dates must be provided.");
        }
        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn.");
        }
        if (room.getCapacity() <= 0) {
            throw new IllegalArgumentException("Room capacity must be positive.");
        }
    }
}
