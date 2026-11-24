import app.HotelServer;
import monitoring.AppMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.InMemoryBookingRepository;
import service.HotelService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HotelEndpointTest {
    private AppMetrics metrics;
    private HotelServer server;
    private HttpClient client;

    @BeforeEach
    void setUp() throws Exception {
        metrics = AppMetrics.prometheus();
        HotelService hotelService = new HotelService(new InMemoryBookingRepository(), metrics);
        server = new HotelServer(hotelService, metrics, 0);
        server.start();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void metricsEndpointShowsPrometheusText() throws Exception {
        HttpResponse<String> response = client.send(get("/metrics"), HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("application_operations_total"));
        assertTrue(response.body().contains("hotel_bookings_total"));
        assertTrue(response.body().contains("application_operation_duration_seconds_count"));
        assertTrue(metric(response.body(), "hotel_bookings_total") >= 1.0);
    }

    @Test
    void successAndOccupiedOperationsChangeMetrics() throws Exception {
        long bookingsBefore = metrics.bookingsCount();
        long occupiedBefore = metrics.occupiedCount();

        HttpResponse<String> success = client.send(post(
                "guestName=Ivan+Ivanov&phone=123&roomNumber=102&checkIn=2026-12-01&checkOut=2026-12-05"
        ), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> occupied = client.send(post(
                "guestName=Olga+Smirnova&phone=123&roomNumber=101&checkIn=2026-11-03&checkOut=2026-11-08"
        ), HttpResponse.BodyHandlers.ofString());

        assertEquals(200, success.statusCode());
        assertTrue(success.body().contains("Бронирование создано"));
        assertEquals(409, occupied.statusCode());
        assertTrue(occupied.body().contains("уже забронирован"));
        assertEquals(bookingsBefore + 1L, metrics.bookingsCount());
        assertEquals(occupiedBefore + 1L, metrics.occupiedCount());
    }

    private HttpRequest get(String path) {
        return HttpRequest.newBuilder(URI.create(base() + path)).GET().build();
    }

    private HttpRequest post(String form) {
        return HttpRequest.newBuilder(URI.create(base() + "/bookings"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
    }

    private String base() {
        return "http://127.0.0.1:" + server.port();
    }

    private static double metric(String body, String name) {
        for (String line : body.split("\n")) {
            if (line.startsWith(name + " ") || line.startsWith(name + "{")) {
                String[] parts = line.trim().split(" ");
                return Double.parseDouble(parts[parts.length - 1]);
            }
        }
        throw new AssertionError("Metric not found: " + name);
    }
}
