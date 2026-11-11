package service;

import domain.Booking;
import domain.Guest;
import domain.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.BookingRepository;

import java.time.LocalDate;
import java.util.List;

public class HotelService {
    private static final Logger log = LoggerFactory.getLogger(HotelService.class);

    private final BookingRepository bookingRepository;

    public HotelService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public Booking createBooking(Long idIfNeeded,
                                 Guest guest,
                                 Room room,
                                 LocalDate checkIn,
                                 LocalDate checkOut) {
        log.info(
                "Booking request received: guestId={}, roomNumber={}, checkIn={}, checkOut={}",
                guest == null ? null : guest.getId(),
                room == null ? null : room.getNumber(),
                checkIn,
                checkOut);

        try {
            validate(guest, room, checkIn, checkOut);
        } catch (IllegalArgumentException ex) {
            log.error("Booking operation failed: {}", ex.getMessage());
            throw ex;
        }

        List<Booking> conflicts = bookingRepository
                .findBookingsForRoomBetween(room, checkIn, checkOut);

        if (!conflicts.isEmpty()) {
            log.warn(
                    "Room {} is already booked for dates {} - {}",
                    room.getNumber(),
                    checkIn,
                    checkOut);
            throw new IllegalStateException("RoomAAA is already booked for given dates.");
        }

        Booking booking = new Booking();
        booking.setId(idIfNeeded);
        booking.setGuest(guest);
        booking.setRoom(room);
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);

        try {
            Booking saved = bookingRepository.save(booking);
            log.info(
                    "Booking created: id={}, guestId={}, roomNumber={}",
                    saved.getId(),
                    guest.getId(),
                    room.getNumber());
            return saved;
        } catch (RuntimeException ex) {
            log.error("Booking operation failed: {}", ex.getMessage());
            throw ex;
        }
    }

    public Booking getBooking(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> {
            log.error("Booking operation failed: booking not found, id={}", id);
            return new IllegalArgumentException("Booking not found: " + id);
        });
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
