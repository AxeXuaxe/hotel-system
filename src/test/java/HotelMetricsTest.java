import domain.Guest;
import domain.Room;
import monitoring.AppMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.InMemoryBookingRepository;
import service.HotelService;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HotelMetricsTest {
    private AppMetrics metrics;
    private HotelService hotelService;
    private Room room;
    private Guest guest;

    @BeforeEach
    void setUp() {
        metrics = AppMetrics.prometheus();
        hotelService = new HotelService(new InMemoryBookingRepository(), metrics);
        room = new Room(10L, "101", 2);
        guest = new Guest(1L, "Ivan Ivanov", "+79990001122");
    }

    @Test
    void successIncreasesBookingMetrics() {
        hotelService.createBooking(
                null,
                guest,
                room,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5));

        assertEquals(1L, metrics.operationsCount());
        assertEquals(1L, metrics.successCount());
        assertEquals(1L, metrics.bookingsCount());
        assertEquals(0L, metrics.errorCount());
        assertEquals(0L, metrics.occupiedCount());
        assertEquals(1L, metrics.durationCount());
        String scrape = metrics.scrape();
        assertTrue(scrape.contains("application_operations_total"));
        assertTrue(scrape.contains("hotel_bookings_total"));
        assertFalse(scrape.contains("application_operations_total_total"));
    }

    @Test
    void occupiedRoomIncreasesSeparateMetric() {
        hotelService.createBooking(
                null,
                guest,
                room,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5));
        Guest second = new Guest(2L, "Petr Petrov", "+79995552211");

        assertThrows(IllegalStateException.class, () -> hotelService.createBooking(
                null,
                second,
                room,
                LocalDate.of(2026, 12, 2),
                LocalDate.of(2026, 12, 6)));

        assertEquals(2L, metrics.operationsCount());
        assertEquals(1L, metrics.successCount());
        assertEquals(1L, metrics.errorCount());
        assertEquals(1L, metrics.occupiedCount());
        assertEquals(1L, metrics.bookingsCount());
        assertEquals(0L, metrics.bookingErrorsCount());
    }

    @Test
    void invalidDatesIncreaseBookingErrors() {
        assertThrows(IllegalArgumentException.class, () -> hotelService.createBooking(
                null,
                guest,
                room,
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 1)));

        assertEquals(1L, metrics.operationsCount());
        assertEquals(0L, metrics.successCount());
        assertEquals(1L, metrics.errorCount());
        assertEquals(1L, metrics.bookingErrorsCount());
        assertEquals(0L, metrics.occupiedCount());
        assertEquals(0L, metrics.bookingsCount());
    }

    @Test
    void missingBookingIncreasesErrors() {
        assertThrows(IllegalArgumentException.class, () -> hotelService.getBooking(999L));

        assertEquals(1L, metrics.operationsCount());
        assertEquals(1L, metrics.errorCount());
        assertEquals(1L, metrics.bookingErrorsCount());
        assertEquals(0L, metrics.successCount());
    }
}
