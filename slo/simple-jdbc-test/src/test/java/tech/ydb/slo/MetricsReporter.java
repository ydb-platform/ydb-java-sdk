package tech.ydb.slo;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.PushGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public class MetricsReporter {
    private static final Logger log = LoggerFactory.getLogger(MetricsReporter.class);

    private final CollectorRegistry registry = new CollectorRegistry();
    private final PushGateway pushGateway;
    private final String jobName;

    // Метрики
    private final Counter successCounter;
    private final Counter errorCounter;
    private final Histogram latencyHistogram;
    private final Gauge activeConnections;

    public MetricsReporter(String promPgwUrl, String jobName) {
        this.jobName = jobName;

        try {
            this.pushGateway = new PushGateway(new URL(promPgwUrl));
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize PushGateway: " + promPgwUrl, e);
        }

        // Инициализация метрик
        this.successCounter = Counter.build()
                .name("jdbc_test_success_total")
                .help("Total successful operations")
                .labelNames("operation")
                .register(registry);

        this.errorCounter = Counter.build()
                .name("jdbc_test_errors_total")
                .help("Total failed operations")
                .labelNames("operation", "error_type")
                .register(registry);

        this.latencyHistogram = Histogram.build()
                .name("jdbc_test_latency_seconds")
                .help("Operation latency in seconds")
                .labelNames("operation")
                .buckets(0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0)
                .register(registry);

        this.activeConnections = Gauge.build()
                .name("jdbc_test_active_connections")
                .help("Number of active connections")
                .register(registry);
    }

    public void recordSuccess(String operation, double latencySeconds) {
        successCounter.labels(operation).inc();
        latencyHistogram.labels(operation).observe(latencySeconds);
    }

    public void recordError(String operation, String errorType) {
        errorCounter.labels(operation, errorType).inc();
    }

    public void setActiveConnections(int count) {
        activeConnections.set(count);
    }

    public void push() {
        try {
            pushGateway.push(registry, jobName);
            log.info("Metrics pushed to Prometheus");
        } catch (IOException e) {
            log.error("Failed to push metrics to Prometheus", e);
        }
    }

    public void pushAdd() {
        try {
            pushGateway.pushAdd(registry, jobName);
            log.info("Metrics pushed (add) to Prometheus");
        } catch (IOException e) {
            log.error("Failed to push metrics to Prometheus", e);
        }
    }
}