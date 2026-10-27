package app;

import domain.Booking;
import domain.Guest;
import domain.Room;
import repository.InMemoryBookingRepository;
import service.HotelService;

import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        InMemoryBookingRepository repo = new InMemoryBookingRepository();
        HotelService hotelService = new HotelService(repo); // DI

        Guest guest1 = new Guest(1L, "Ivan Ivanov", "+79990001122");
        Guest guest2 = new Guest(2L, "Petr Petrov", "+79995552211");
        Room room = new Room(10L, "101", 2);

        // 1) Успешное бронирование
        Booking b1 = hotelService.createBooking(
                null, guest1, room,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 15)
        );

        System.out.println("Booking #1 id = " + b1.getId());

        // 2) Конфликт: пересечение с [10..15) => [12..18) пересекается
        try {
            Booking b2 = hotelService.createBooking(
                    null, guest2, room,
                    LocalDate.of(2026, 9, 12),
                    LocalDate.of(2026, 9, 18)
            );
            System.out.println("ERROR: конфликт не пойман, booking #2 id = " + b2.getId());
        } catch (IllegalStateException e) {
            System.out.println("OK: конфликт пойман: " + e.getMessage());
        }

        // 3)
        // [15..20) не пересекается с [10..15)
        Booking b3 = hotelService.createBooking(
                null, guest2, room,
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 20)
        );

        System.out.println("Booking #3 id = " + b3.getId());

        Booking loaded = hotelService.getBooking(b1.getId());
        System.out.println("Loaded booking id = " + loaded.getId());
    }
}