package tech.ydb.table.impl;

import tech.ydb.table.SchemeClient;
import tech.ydb.table.rpc.SchemeRpc;


/**
 * @author Sergey Polovko
 */
public class SchemeClientBuilderImpl implements SchemeClient.Builder {

    protected final SchemeRpc schemeRpc;

    public SchemeClientBuilderImpl(SchemeRpc schemeRpc) {
        this.schemeRpc = schemeRpc;
    }

    @Override
    public SchemeClient build() {
        return new SchemeClientImpl(this);
    }
}
