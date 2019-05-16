package tech.ydb.table.query;

/**
 * @author Sergey Polovko
 */
public class ExplainDataQueryResult {

    private final String queryAst;
    private final String queryPlan;

    public ExplainDataQueryResult(String queryAst, String queryPlan) {
        this.queryAst = queryAst;
        this.queryPlan = queryPlan;
    }

    public String getQueryAst() {
        return queryAst;
    }

    public String getQueryPlan() {
        return queryPlan;
    }
}
