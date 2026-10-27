import domain.Booking;
import domain.Guest;
import domain.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.InMemoryBookingRepository;
import service.HotelService;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class HotelServiceTest {

    private HotelService hotelService;
    private Room room;
    private Guest guest1;
    private Guest guest2;

    @BeforeEach
    void setUp() {
        InMemoryBookingRepository repo = new InMemoryBookingRepository();
        hotelService = new HotelService(repo);

        room = new Room(10L, "101", 2);
        guest1 = new Guest(1L, "Ivan Ivanov", "+79990001122");
        guest2 = new Guest(2L, "Petr Petrov", "+79995552211");
    }

    // Тест 1 — успешный сценарий
    @Test
    void test1_successBooking() {
        Booking booking = hotelService.createBooking(
                null,
                guest1,
                room,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 15)
        );

        assertNotNull(booking);
        assertNotNull(booking.getId());
        assertEquals(guest1.getId(), booking.getGuest().getId());
        assertEquals(room.getId(), booking.getRoom().getId());
    }

    // Тест 2 — ошибочный сценарий: не может взять это же повторно
    @Test
    void test2_cannotBookSameDatesAgain() {
        hotelService.createBooking(
                null,
                guest1,
                room,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 15)
        );

        assertThrows(IllegalStateException.class, () -> hotelService.createBooking(
                null,
                guest1,
                room,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 15)
        ));
    }

    @Test
    void test3_otherGuestCannotBookTakenSlot() {
        hotelService.createBooking(
                null,
                guest1,
                room,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 15)
        );

        assertThrows(IllegalStateException.class, () -> hotelService.createBooking(
                null,
                guest2,
                room,
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 18)
        ));
    }
    @Test
    void test4_savedBookingCanBeFound() {
        Booking booking = hotelService.createBooking(
                null,
                guest1,
                room,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 5)
        );

        Booking found = hotelService.getBooking(booking.getId());

        assertEquals(booking.getId(), found.getId());
        assertEquals(room.getNumber(), found.getRoom().getNumber());
        assertEquals(LocalDate.of(2026, 10, 1), found.getCheckIn());
        assertEquals(LocalDate.of(2026, 10, 5), found.getCheckOut());
    }
}