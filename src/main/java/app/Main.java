package app;

import domain.Booking;
import domain.Guest;
import domain.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.InMemoryBookingRepository;
import service.HotelService;

import java.time.LocalDate;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        InMemoryBookingRepository repo = new InMemoryBookingRepository();
        HotelService hotelService = new HotelService(repo);

        Guest guest1 = new Guest(1L, "Ivan Ivanov", "+79990001122");
        Guest guest2 = new Guest(2L, "Petr Petrov", "+79995552211");
        Room room = new Room(10L, "101", 2);

        Booking first = hotelService.createBooking(
                null, guest1, room,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 15)
        );
        log.info("Successful booking stored, id={}", first.getId());

        try {
            Booking conflict = hotelService.createBooking(
                    null, guest2, room,
                    LocalDate.of(2026, 9, 12),
                    LocalDate.of(2026, 9, 18)
            );
            log.error("Conflict was not detected, booking id={}", conflict.getId());
        } catch (IllegalStateException ex) {
            log.warn("Booking was rejected because the room is taken: {}", ex.getMessage());
        }

        Booking next = hotelService.createBooking(
                null, guest2, room,
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 20)
        );
        log.info("Next booking stored, id={}", next.getId());

        Booking loaded = hotelService.getBooking(first.getId());
        log.info("Loaded booking id={}", loaded.getId());
    }
}
