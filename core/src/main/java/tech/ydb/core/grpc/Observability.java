package tech.ydb.core.grpc;

public final class Observability {
    public static final String TRACING_CHAIN = "ydb-sdk-tracing";
    public static final String METRICS_CHAIN = "ydb-sdk-metrics";

    public static final String TRACING_CHAIN_VERSION = "0.1.0";
    public static final String METRICS_CHAIN_VERSION = "0.1.0";

    public static final String TRACING_CHAIN_TOKEN = TRACING_CHAIN + "/" + TRACING_CHAIN_VERSION;
    public static final String METRICS_CHAIN_TOKEN = METRICS_CHAIN + "/" + METRICS_CHAIN_VERSION;

    private Observability() {
    }

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
