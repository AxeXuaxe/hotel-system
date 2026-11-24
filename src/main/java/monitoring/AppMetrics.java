package monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.util.concurrent.TimeUnit;

/**
 * Реестр метрик приложения. Имена без суффикса _total:
 * Prometheus добавит его сам при отдаче счётчика.
 */
public final class AppMetrics {
    private final MeterRegistry registry;
    private final Counter operations;
    private final Counter successes;
    private final Counter errors;
    private final Counter bookings;
    private final Counter occupied;
    private final Counter bookingErrors;
    private final Timer operationDuration;

    private AppMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.operations = counter(registry, "application_operations",
                "Number of hotel application operations");
        this.successes = counter(registry, "application_operations_success",
                "Number of successful hotel application operations");
        this.errors = counter(registry, "application_operations_error",
                "Number of failed hotel application operations");
        this.bookings = counter(registry, "hotel_bookings",
                "Number of successfully created room bookings");
        this.occupied = counter(registry, "hotel_booking_occupied",
                "Attempts to book a room that is already taken");
        this.bookingErrors = counter(registry, "hotel_booking_errors",
                "Booking operations that failed because of invalid data");
        this.operationDuration = Timer.builder("application_operation_duration")
                .description("Duration of hotel application operations")
                .register(registry);
    }

    public static AppMetrics prometheus() {
        return new AppMetrics(new PrometheusMeterRegistry(PrometheusConfig.DEFAULT));
    }

    public static AppMetrics inMemory() {
        return new AppMetrics(new SimpleMeterRegistry());
    }

    public String scrape() {
        if (registry instanceof PrometheusMeterRegistry) {
            return ((PrometheusMeterRegistry) registry).scrape();
        }
        throw new IllegalStateException("Prometheus registry is required for /metrics");
    }

    public void operation() {
        operations.increment();
    }

    public void success() {
        successes.increment();
    }

    public void error() {
        errors.increment();
    }

    public void bookingCreated() {
        bookings.increment();
    }

    public void roomOccupied() {
        occupied.increment();
    }

    public void bookingError() {
        bookingErrors.increment();
    }

    public void recordDuration(long nanos) {
        operationDuration.record(nanos, TimeUnit.NANOSECONDS);
    }

    public long operationsCount() {
        return (long) operations.count();
    }

    public long successCount() {
        return (long) successes.count();
    }

    public long errorCount() {
        return (long) errors.count();
    }

    public long bookingsCount() {
        return (long) bookings.count();
    }

    public long occupiedCount() {
        return (long) occupied.count();
    }

    public long bookingErrorsCount() {
        return (long) bookingErrors.count();
    }

    public long durationCount() {
        return operationDuration.count();
    }

    private static Counter counter(MeterRegistry registry, String name, String description) {
        return Counter.builder(name)
                .description(description)
                .register(registry);
    }
}
