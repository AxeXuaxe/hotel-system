package service;

import domain.Booking;
import domain.Guest;
import domain.Room;
import monitoring.AppMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.BookingRepository;

import java.time.LocalDate;
import java.util.List;

public class HotelService {
    private static final Logger log = LoggerFactory.getLogger(HotelService.class);

    private final BookingRepository bookingRepository;
    private final AppMetrics metrics;

    public HotelService(BookingRepository bookingRepository) {
        this(bookingRepository, AppMetrics.inMemory());
    }

    public HotelService(BookingRepository bookingRepository, AppMetrics metrics) {
        this.bookingRepository = bookingRepository;
        this.metrics = metrics;
    }

    public Booking createBooking(Long idIfNeeded,
                                 Guest guest,
                                 Room room,
                                 LocalDate checkIn,
                                 LocalDate checkOut) {
        long started = System.nanoTime();
        metrics.operation();
        log.info(
                "Booking request received: guestId={}, roomNumber={}, checkIn={}, checkOut={}",
                guest == null ? null : guest.getId(),
                room == null ? null : room.getNumber(),
                checkIn,
                checkOut);
        try {
            try {
                validate(guest, room, checkIn, checkOut);
            } catch (IllegalArgumentException ex) {
                metrics.error();
                metrics.bookingError();
                log.error("Booking operation failed: {}", ex.getMessage());
                throw ex;
            }

            List<Booking> conflicts = bookingRepository
                    .findBookingsForRoomBetween(room, checkIn, checkOut);
            if (!conflicts.isEmpty()) {
                metrics.error();
                metrics.roomOccupied();
                log.warn(
                        "Room {} is already booked for dates {} - {}",
                        room.getNumber(),
                        checkIn,
                        checkOut);
                throw new IllegalStateException("Room is already booked for given dates.");
            }

            Booking booking = new Booking();
            booking.setId(idIfNeeded);
            booking.setGuest(guest);
            booking.setRoom(room);
            booking.setCheckIn(checkIn);
            booking.setCheckOut(checkOut);
            try {
                Booking saved = bookingRepository.save(booking);
                metrics.success();
                metrics.bookingCreated();
                log.info(
                        "Booking created: id={}, guestId={}, roomNumber={}",
                        saved.getId(),
                        guest.getId(),
                        room.getNumber());
                return saved;
            } catch (RuntimeException ex) {
                metrics.error();
                metrics.bookingError();
                log.error("Booking operation failed: {}", ex.getMessage());
                throw ex;
            }
        } finally {
            metrics.recordDuration(System.nanoTime() - started);
        }
    }

    public Booking getBooking(Long id) {
        long started = System.nanoTime();
        metrics.operation();
        try {
            return bookingRepository.findById(id).map(found -> {
                metrics.success();
                log.info("Booking loaded: id={}", found.getId());
                return found;
            }).orElseThrow(() -> {
                metrics.error();
                metrics.bookingError();
                log.error("Booking operation failed: booking not found, id={}", id);
                return new IllegalArgumentException("Booking not found: " + id);
            });
        } finally {
            metrics.recordDuration(System.nanoTime() - started);
        }
    }

    public void recordFailedOperation(String reason) {
        long started = System.nanoTime();
        metrics.operation();
        try {
            metrics.error();
            metrics.bookingError();
            log.error("Booking operation failed: {}", reason);
        } finally {
            metrics.recordDuration(System.nanoTime() - started);
        }
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
