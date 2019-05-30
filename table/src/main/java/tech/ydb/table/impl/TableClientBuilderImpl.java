package tech.ydb.table.impl;

import java.util.concurrent.TimeUnit;

import tech.ydb.table.TableClient;
import tech.ydb.table.rpc.TableRpc;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * @author Sergey Polovko
 */
public class TableClientBuilderImpl implements TableClient.Builder {

    protected final TableRpc tableRpc;
    protected int queryCacheSize = 1000;
    protected boolean keepQueryText = true;
    protected SessionPoolOptions sessionPoolOptions = SessionPoolOptions.DEFAULT;

    public TableClientBuilderImpl(TableRpc tableRpc) {
        this.tableRpc = tableRpc;
    }

    @Override
    public TableClient.Builder queryCacheSize(int size) {
        checkArgument(size >= 0, "queryCacheSize(%d) is negative", size);
        this.queryCacheSize = size;
        return this;
    }

    @Override
    public TableClient.Builder keepQueryText(boolean keep) {
        this.keepQueryText = keep;
        return this;
    }

    @Override
    public TableClient.Builder sessionPoolSize(int minSize, int maxSize) {
        checkArgument(minSize >= 0, "sessionPoolMinSize(%d) is negative", minSize);
        checkArgument(maxSize >= 0, "sessionPoolMaxSize(%d) is negative", maxSize);
        checkArgument(
            minSize <= maxSize,
            "sessionPoolMinSize(%d) is greater than sessionPoolMaxSize(%d)",
            minSize, maxSize);
        this.sessionPoolOptions = sessionPoolOptions.withSize(minSize, maxSize);
        return this;
    }

    @Override
    public TableClient.Builder sessionKeepAliveTime(long time, TimeUnit timeUnit) {
        checkArgument(time >= 0, "sessionKeepAliveTime(%d, %s) is negative", time, timeUnit);
        long timeMillis = timeUnit.toMillis(time);
        checkArgument(
            timeMillis >= TimeUnit.SECONDS.toMillis(1),
            "sessionKeepAliveTime(%d, %s) is less than 1 second",
            time, timeUnit);
        checkArgument(
            timeMillis <= TimeUnit.MINUTES.toMillis(30),
            "sessionKeepAliveTime(%d, %s) is greater than 30 minutes",
            time, timeUnit);
        this.sessionPoolOptions = sessionPoolOptions.withKeepAliveTimeMillis(timeMillis);
        return this;
    }

    @Override
    public TableClient.Builder sessionMaxIdleTime(long time, TimeUnit timeUnit) {
        checkArgument(time >= 0, "sessionMaxIdleTime(%d, %s) is negative", time, timeUnit);
        long timeMillis = timeUnit.toMillis(time);
        checkArgument(
            timeMillis >= TimeUnit.SECONDS.toMillis(1),
            "sessionMaxIdleTime(%d, %s) is less than 1 second",
            time, timeUnit);
        checkArgument(
            timeMillis <= TimeUnit.MINUTES.toMillis(30),
            "sessionMaxIdleTime(%d, %s) is greater than 30 minutes",
            time, timeUnit);
        this.sessionPoolOptions = sessionPoolOptions.withMaxIdleTimeMillis(timeMillis);
        return this;
    }

    @Override
    public TableClient.Builder sessionCreationMaxRetries(int maxRetries) {
        checkArgument(maxRetries >= 0, "sessionCreationMaxRetries(%d) is negative", maxRetries);
        this.sessionPoolOptions = sessionPoolOptions.withCreationMaxRetries(maxRetries);
        return this;
    }

    @Override
    public TableClient build() {
        return new TableClientImpl(this);
    }
}
