package tech.ydb.core.grpc;

/**
 * SDK observability adoption chains reported via the {@code x-ydb-sdk-build-info} header.
 * <p>
 * A chain is a {@code name/version} token appended to the base {@code ydb-java-sdk/<version>} using
 * {@code ;} as separator; chain versions are bumped in-code when the underlying integration changes
 * shape (metric names, attribute keys, tracing conventions, ...).
 */
public final class Observability {
    public static final String TRACING_CHAIN = "ydb-sdk-tracing";
    public static final String METRICS_CHAIN = "ydb-sdk-metrics";

    public static final String TRACING_CHAIN_VERSION = "0.1.0";
    public static final String METRICS_CHAIN_VERSION = "0.1.0";

    public static final String TRACING_CHAIN_TOKEN = TRACING_CHAIN + "/" + TRACING_CHAIN_VERSION;
    public static final String METRICS_CHAIN_TOKEN = METRICS_CHAIN + "/" + METRICS_CHAIN_VERSION;

    private Observability() {
    }

    /**
     * Appends the enabled observability adoption chains to the base build-info string, preserving the
     * existing token order ({@code base;tracing;metrics}). Disabled integrations contribute nothing, so a
     * fully disabled observability leaves the base build-info untouched.
     *
     * @param base    base build-info (for example {@code ydb-java-sdk/2.4.0;ydb-jdbc-driver/2.4.0})
     * @param tracing whether the client-side OpenTelemetry tracing is configured
     * @param metrics whether the client-side OpenTelemetry metrics are configured
     * @return build-info string carrying the enabled adoption chains
     */
    public static String appendChains(String base, boolean tracing, boolean metrics) {
        if (!tracing && !metrics) {
            return base;
        }
        StringBuilder sb = new StringBuilder(base);
        if (tracing) {
            sb.append(';').append(TRACING_CHAIN_TOKEN);
        }
        if (metrics) {
            sb.append(';').append(METRICS_CHAIN_TOKEN);
        }
        return sb.toString();
    }
}
