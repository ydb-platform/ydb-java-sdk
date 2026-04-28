package tech.ydb.opentelemetry;

import java.util.Objects;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;

import tech.ydb.core.Status;
import tech.ydb.core.metrics.Meter;
import tech.ydb.core.metrics.SessionPoolObserver;

public final class OpenTelemetryMeter implements Meter {
    private final io.opentelemetry.api.metrics.Meter meter;

    private final DoubleHistogram operationDuration;    // db.client.operation.duration
    private final LongCounter operationFailed;          // ydb.client.operation.failed
    private final DoubleHistogram sessionCreateTime;    // ydb.query.session.create_time

    private final Attributes baseAttributes;            // db.system.name, db.namespace, server.address, server.port

    private OpenTelemetryMeter(io.opentelemetry.api.metrics.Meter meter,
                               String database, String host, int port) {
        this.meter = Objects.requireNonNull(meter, "meter is null");
        this.operationDuration = meter.histogramBuilder("db.client.operation.duration")
                .setUnit("s")
                .build();
        this.operationFailed = meter.counterBuilder("ydb.client.operation.failed")
                .setUnit("{command}")
                .build();
        this.sessionCreateTime = meter.histogramBuilder("ydb.query.session.create_time")
                .setUnit("s")
                .build();

        this.baseAttributes = Attributes.of(
                AttributeKey.stringKey("db.system.name"), "ydb",
                AttributeKey.stringKey("db.namespace"), database,
                AttributeKey.stringKey("server.address"), host,
                AttributeKey.longKey("server.port"), (long) port
        );
    }

    public static OpenTelemetryMeter fromOpenTelemetry(OpenTelemetry openTelemetry,
                                                       String database, String host, int port) {
        io.opentelemetry.api.metrics.Meter meter = openTelemetry
                .getMeter("tech.ydb.sdk");

        return new OpenTelemetryMeter(meter, database, host, port);
    }

    public static OpenTelemetryMeter createGlobal(String database, String host, int port) {
        return fromOpenTelemetry(GlobalOpenTelemetry.get(), database, host, port);
    }

    @Override
    public void recordOperation(String name, long durationNanos, Status status) {
        Attributes attrs = baseAttributes.toBuilder()
                .put("ydb.operation.name", name)
                .build();

        operationDuration.record(durationNanos / 1_000_000_000.0, attrs);

        if (status != null && !status.isSuccess()) {
            Attributes errAttrs = attrs.toBuilder()
                    .put("db.response.status_code", status.getCode().toString())
                    .build();
            operationFailed.add(1, errAttrs);
        }
    }

    @Override
    public void registerSessionPool(String poolName, SessionPoolObserver observer) {
        meter.upDownCounterBuilder("ydb.query.session.count")
                .setUnit("{connection}")
                .buildWithCallback(measurement -> {
                    Attributes idle = Attributes.of(
                            AttributeKey.stringKey("ydb.query.session.pool.name"), poolName,
                            AttributeKey.stringKey("ydb.query.session.state"), "idle"
                    );
                    Attributes used = Attributes.of(
                            AttributeKey.stringKey("ydb.query.session.pool.name"), poolName,
                            AttributeKey.stringKey("ydb.query.session.state"), "used"
                    );
                    measurement.record(observer.getIdleCount(), idle);
                    measurement.record(observer.getUsedCount(), used);
                });
    }

    @Override
    public void recordSessionCreateTime(String poolName, long durationNanos) {
        Attributes attrs = Attributes.of(
                AttributeKey.stringKey("ydb.query.session.pool.name"), poolName
        );
        sessionCreateTime.record(durationNanos / 1_000_000_000.0, attrs);
    }
}
