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
    private final String javaVersion;

    private final Counter operationsTotal;
    private final Counter operationsSuccessTotal;
    private final Histogram operationLatencySeconds;
    private final Gauge pendingOperations;

    private int totalSuccess = 0;
    private int totalErrors = 0;

    public MetricsReporter(String promPgwUrl, String jobName) {
        this.jobName = jobName;
        this.javaVersion = System.getProperty("java.version");

        try {
            URL url = URI.create(promPgwUrl).toURL();
            this.pushGateway = new PushGateway(url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize PushGateway: " + promPgwUrl, e);
        }

        // Total operations (including errors)
        this.operationsTotal = Counter.build()
                .name("sdk_operations_total")
                .labelNames("operation_type", "sdk", "sdk_version", "workload", "workload_version")
                .help("Total number of operations performed by the SDK")
                .register(registry);

        // Successful operations only
        this.operationsSuccessTotal = Counter.build()
                .name("sdk_operations_success_total")
                .labelNames("operation_type", "sdk", "sdk_version", "workload", "workload_version")
                .help("Total number of successful operations")
                .register(registry);

        // Operation latency
        this.operationLatencySeconds = Histogram.build()
                .name("sdk_operation_latency_seconds")
                .labelNames("operation_type", "operation_status", "sdk", "sdk_version", "workload", "workload_version")
                .help("Operation latency in seconds")
                .buckets(0.001, 0.002, 0.003, 0.004, 0.005, 0.0075, 0.010, 0.020, 0.050, 0.100, 0.200, 0.500, 1.000)
                .register(registry);

        // Pending operations gauge
        this.pendingOperations = Gauge.build()
                .name("sdk_pending_operations")
                .labelNames("operation_type", "sdk", "sdk_version", "workload", "workload_version")
                .help("Current number of pending operations")
                .register(registry);
    }

    /**
     * Record successful operation
     */
    public void recordSuccess(String operation, double latencySeconds) {
        // Increment total operations
        operationsTotal.labels(operation, "java", javaVersion, jobName, "0.0.0").inc();

        // Increment successful operations
        operationsSuccessTotal.labels(operation, "java", javaVersion, jobName, "0.0.0").inc();

        // Record latency
        operationLatencySeconds.labels(operation, "success", "java", javaVersion, jobName, "0.0.0")
                .observe(latencySeconds);

        totalSuccess++;

        // Log every 100 operations
        if (totalSuccess % 100 == 0) {
            System.out.printf("✅ [%s] Success #%d, latency: %.3f ms%n",
                    operation, totalSuccess, latencySeconds * 1000);
        }
    }

    /**
     * Record failed operation
     */
    public void recordError(String operation, String errorType) {
        // Increment total operations (errors are also operations)
        operationsTotal.labels(operation, "java", javaVersion, jobName, "0.0.0").inc();

        // Note: We could add error latency here if we tracked it
        // For now, we just count the error

        totalErrors++;

        // Log errors
        System.out.printf("❌ [%s] Error #%d, type: %s%n",
                operation, totalErrors, errorType);
    }

    /**
     * Set pending operations count
     */
    public void setPendingOperations(String operationType, int count) {
        pendingOperations.labels(operationType, "java", javaVersion, jobName, "0.0.0").set(count);
    }

    /**
     * Push metrics to Prometheus Push Gateway
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

            System.out.println("📤 Metrics pushed to Prometheus");
            System.out.println("   Success total: " + totalSuccess);
            System.out.println("   Errors total: " + totalErrors);

            log.debug("Metrics pushed to Prometheus");
        } catch (IOException e) {
            System.err.println("❌ Failed to push metrics: " + e.getMessage());
            log.error("Failed to push metrics", e);
        }
    }

    /**
     * Save metrics summary to file
     */
    public void saveToFile(String filename, double latencySeconds) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("SUCCESS_COUNT=" + totalSuccess);
            writer.println("ERROR_COUNT=" + totalErrors);
            writer.println("LATENCY_MS=" + String.format("%.2f", latencySeconds * 1000));
            writer.println("PENDING_OPERATIONS=0");

            System.out.println("💾 Metrics saved to file:");
            System.out.println("   Success: " + totalSuccess);
            System.out.println("   Errors: " + totalErrors);
            System.out.println("   Latency: " + String.format("%.2f ms", latencySeconds * 1000));

            log.info("Metrics saved to {}", filename);
        } catch (IOException e) {
            log.error("Failed to save metrics to file", e);
        }
    }
}