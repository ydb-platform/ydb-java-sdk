package tech.ydb.table.settings;

/**
 * @author Sergey Polovko
 */
class RequestSettings<Self extends RequestSettings> {

    private String traceId;

    public String getTraceId() {
        return traceId;
    }

    public Self setTraceId(String traceId) {
        this.traceId = traceId;
        return self();
    }

    @SuppressWarnings("unchecked")
    private Self self() {
        return (Self) this;
    }
}
