package tech.ydb.table.query;

import tech.ydb.table.YdbTable;

/**
 * @author Sergey Polovko
 */
public class ExplainDataQueryResult {

    private final String queryAst;
    private final String queryPlan;

    public ExplainDataQueryResult(YdbTable.ExplainQueryResult query) {
        this.queryAst = query.getQueryAst();
        this.queryPlan = query.getQueryPlan();
    }

    public String getQueryAst() {
        return queryAst;
    }

    public String getQueryPlan() {
        return queryPlan;
    }
}
