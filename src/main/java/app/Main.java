package app;

import monitoring.AppMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.InMemoryBookingRepository;
import service.HotelService;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final int DEFAULT_PORT = 8081;

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        AppMetrics metrics = AppMetrics.prometheus();
        HotelService hotelService = new HotelService(new InMemoryBookingRepository(), metrics);
        HotelServer server = new HotelServer(hotelService, metrics, port);
        server.start();
        log.info("Hotel application is running at http://localhost:{}/", server.port());
        log.info("Prometheus metrics: http://localhost:{}/metrics", server.port());
        Thread.currentThread().join();
    }
}
