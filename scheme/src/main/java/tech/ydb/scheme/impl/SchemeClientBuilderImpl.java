package tech.ydb.scheme.impl;

import tech.ydb.scheme.SchemeClient;



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
