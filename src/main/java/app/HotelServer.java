package app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import domain.Booking;
import domain.Guest;
import domain.Room;
import monitoring.AppMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.HotelService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class HotelServer {
    private static final Logger log = LoggerFactory.getLogger(HotelServer.class);

    private final HotelService hotelService;
    private final AppMetrics metrics;
    private final HttpServer server;
    private final Map<String, Room> rooms = new LinkedHashMap<>();
    private final AtomicLong guestIds = new AtomicLong(1);
    private final String template;

    public HotelServer(HotelService hotelService, AppMetrics metrics, int port) throws IOException {
        this.hotelService = hotelService;
        this.metrics = metrics;
        this.template = loadTemplate();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/", this::handle);
        prepareRooms();
        seedOccupiedRoom();
    }

    public void start() {
        server.start();
        log.info("Metrics endpoint is available at http://localhost:{}/metrics", port());
    }

    public void stop() {
        server.stop(0);
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void prepareRooms() {
        addRoom(101L, "101");
        addRoom(102L, "102");
        addRoom(103L, "103");
        addRoom(104L, "104");
    }

    private void addRoom(Long id, String number) {
        rooms.put(number, new Room(id, number, 2));
    }

    private void seedOccupiedRoom() {
        Guest guest = new Guest(guestIds.getAndIncrement(), "Anna Petrova", "+79990000001");
        hotelService.createBooking(
                null,
                guest,
                rooms.get("101"),
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 10));
        log.info("Seed booking created for the occupied-room scenario, room 101");
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if ("/favicon.ico".equals(path)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if ("/metrics".equals(path) && "GET".equals(method)) {
                writeText(exchange, 200, metrics.scrape(),
                        "text/plain; version=0.0.4; charset=utf-8");
                return;
            }
            if ("/".equals(path) && "GET".equals(method)) {
                writeHtml(exchange, 200, render(null, null));
                return;
            }
            if ("/bookings".equals(path) && "POST".equals(method)) {
                createBooking(exchange);
                return;
            }
            if ("/bookings".equals(path) && "GET".equals(method)) {
                findBooking(exchange);
                return;
            }
            writeHtml(exchange, 404, render("error", "Страница не найдена."));
        } catch (RuntimeException ex) {
            log.error("Request failed: {}", ex.getMessage());
            writeHtml(exchange, 500, render("error", "Внутренняя ошибка приложения."));
        } finally {
            exchange.close();
        }
    }

    private void createBooking(HttpExchange exchange) throws IOException {
        String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = parseForm(raw);
        String guestName = form.getOrDefault("guestName", "").trim();
        String phone = form.getOrDefault("phone", "").trim();
        String roomNumber = form.getOrDefault("roomNumber", "").trim();
        String checkIn = form.getOrDefault("checkIn", "").trim();
        String checkOut = form.getOrDefault("checkOut", "").trim();

        if (guestName.isEmpty() || phone.isEmpty() || roomNumber.isEmpty()) {
            hotelService.recordFailedOperation("Guest, room and dates must be provided.");
            writeHtml(exchange, 400, render("error", "Укажите гостя, телефон и номер комнаты."));
            return;
        }
        Room room = rooms.get(roomNumber);
        if (room == null) {
            hotelService.recordFailedOperation("Unknown room " + roomNumber);
            writeHtml(exchange, 400, render("error", "Такого номера нет."));
            return;
        }

        LocalDate inDate;
        LocalDate outDate;
        try {
            inDate = LocalDate.parse(checkIn);
            outDate = LocalDate.parse(checkOut);
        } catch (DateTimeParseException ex) {
            hotelService.recordFailedOperation("Invalid date format");
            writeHtml(exchange, 400, render("error", "Даты должны быть в формате ГГГГ-ММ-ДД."));
            return;
        }

        Guest guest = new Guest(guestIds.getAndIncrement(), guestName, phone);
        try {
            Booking booking = hotelService.createBooking(null, guest, room, inDate, outDate);
            String text = "Бронирование создано. Номер заявки: " + booking.getId()
                    + ", комната " + room.getNumber() + ".";
            writeHtml(exchange, 200, render("ok", text));
        } catch (IllegalStateException ex) {
            writeHtml(exchange, 409, render("error", "Номер уже забронирован на выбранные даты."));
        } catch (IllegalArgumentException ex) {
            writeHtml(exchange, 400, render("error", describe(ex)));
        }
    }

    private void findBooking(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        Map<String, String> params = parseForm(query == null ? "" : query);
        String rawId = params.getOrDefault("id", "").trim();
        long id;
        try {
            id = Long.parseLong(rawId);
        } catch (NumberFormatException ex) {
            hotelService.recordFailedOperation("Booking id is not a number");
            writeHtml(exchange, 400, render("error", "Укажите числовой идентификатор бронирования."));
            return;
        }
        try {
            Booking booking = hotelService.getBooking(id);
            String text = "Бронирование " + booking.getId()
                    + ": " + booking.getGuest().getFullName()
                    + ", комната " + booking.getRoom().getNumber()
                    + ", " + booking.getCheckIn()
                    + " — " + booking.getCheckOut() + ".";
            writeHtml(exchange, 200, render("ok", text));
        } catch (IllegalArgumentException ex) {
            writeHtml(exchange, 404, render("error", "Бронирование не найдено."));
        }
    }

    private static String describe(IllegalArgumentException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        if (message.contains("checkOut")) {
            return "Дата выезда должна быть позже даты заезда.";
        }
        if (message.contains("capacity")) {
            return "Вместимость номера должна быть больше нуля.";
        }
        return "Некорректные данные бронирования.";
    }

    private String render(String kind, String message) {
        String block = "";
        if (message != null && !message.isEmpty()) {
            String css = "ok".equals(kind) ? "ok" : "error";
            block = "<p class=\"banner " + css + "\">" + escape(message) + "</p>";
        }
        return template
                .replace("__OPERATIONS__", Long.toString(metrics.operationsCount()))
                .replace("__SUCCESS__", Long.toString(metrics.successCount()))
                .replace("__ERRORS__", Long.toString(metrics.errorCount()))
                .replace("__BOOKINGS__", Long.toString(metrics.bookingsCount()))
                .replace("__OCCUPIED__", Long.toString(metrics.occupiedCount()))
                .replace("__BOOKING_ERRORS__", Long.toString(metrics.bookingErrorsCount()))
                .replace("__MESSAGE_BLOCK__", block);
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> params = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return params;
        }
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            int separator = pair.indexOf('=');
            if (separator < 0) {
                continue;
            }
            String key = urlDecode(pair.substring(0, separator));
            String value = urlDecode(pair.substring(separator + 1));
            params.put(key, value);
        }
        return params;
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void writeHtml(HttpExchange exchange, int status, String body) throws IOException {
        writeText(exchange, status, body, "text/html; charset=utf-8");
    }

    private static void writeText(HttpExchange exchange, int status, String body, String type)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String loadTemplate() throws IOException {
        InputStream in = HotelServer.class.getClassLoader().getResourceAsStream("page.html");
        if (in == null) {
            throw new IllegalStateException("page.html is missing");
        }
        try (InputStream source = in) {
            return new String(source.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
