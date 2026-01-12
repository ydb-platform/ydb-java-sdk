package tech.ydb.slo;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.PushGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.Map;

public class MetricsReporter {
    private static final Logger log = LoggerFactory.getLogger(MetricsReporter.class);

    private final CollectorRegistry registry = new CollectorRegistry();
    private final PushGateway pushGateway;
    private final String jobName;

    private final Counter successCounter;
    private final Counter errorCounter;
    private final Histogram latencyHistogram;
    private final Gauge activeConnections;

    private int totalSuccess = 0;
    private int totalErrors = 0;

    public MetricsReporter(String promPgwUrl, String jobName) {
        this.jobName = jobName;

        try {
            URL url = URI.create(promPgwUrl).toURL();
            this.pushGateway = new PushGateway(url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize PushGateway: " + promPgwUrl, e);
        }

        this.successCounter = Counter.build()
                .name("slo_success_total")
                .labelNames("operation", "workload")
                .help("Total successful operations")
                .register(registry);

        this.errorCounter = Counter.build()
                .name("slo_errors_total")
                .help("Total failed operations")
                .labelNames("operation", "error_type", "workload")
                .register(registry);

        this.latencyHistogram = Histogram.build()
                .name("slo_latency_seconds")
                .labelNames("operation", "workload")
                .help("Operation latency in seconds")
                .buckets(0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0)
                .register(registry);

        this.activeConnections = Gauge.build()
                .name("slo_active_connections")
                .help("Number of active connections")
                .register(registry);
    }

    public void recordSuccess(String operation, double latencySeconds) {
        successCounter.labels(operation, jobName).inc();
        latencyHistogram.labels(operation, jobName).observe(latencySeconds);
        totalSuccess++;
    }

    public void recordError(String operation, String errorType) {
        errorCounter.labels(operation, errorType, jobName).inc();
        totalErrors++;
    }

    public void setActiveConnections(int count) {
        activeConnections.set(count);
    }

    /*
    public void push() {
        try {
            pushGateway.push(registry, jobName);
            log.debug("Metrics pushed to Prometheus");
        } catch (IOException e) {
            log.error("Failed to push metrics to Prometheus", e);
        }
    }
    */

    public void push() {
        try {
            pushGateway.pushAdd(
                    registry,
                    jobName,
                    Map.of(
                            "workload", jobName,
                            "instance", "jdbc"
                    )
            );
            log.debug("Metrics pushed to Prometheus");
        } catch (IOException e) {
            log.error("Failed to push metrics", e);
        }
    }

    public void pushAdd() {
        try {
            pushGateway.pushAdd(registry, jobName);
            log.debug("Metrics pushed (add) to Prometheus");
        } catch (IOException e) {
            log.error("Failed to push metrics to Prometheus", e);
        }
    }

    public void saveToFile(String filename, double latencySeconds) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("SUCCESS_COUNT=" + totalSuccess);
            writer.println("ERROR_COUNT=" + totalErrors);
            writer.println("LATENCY_MS=" + String.format("%.2f", latencySeconds * 1000));
            writer.println("ACTIVE_CONNECTIONS=" + (int)activeConnections.get());
            log.info("Metrics saved to {}", filename);
        } catch (IOException e) {
            log.error("Failed to save metrics to file", e);
        }
    }
}