package tech.ydb.query.result;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryInfo {
    private final QueryStats stats;

    public QueryInfo(QueryStats stats) {
        this.stats = stats;
    }

    public boolean hasStats() {
        return stats != null;
    }

    public QueryStats getStats() {
        return stats;
    }
}
