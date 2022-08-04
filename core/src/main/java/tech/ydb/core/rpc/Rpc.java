package tech.ydb.core.rpc;

import javax.annotation.Nullable;

/**
 * @author Sergey Polovko
 */
public interface Rpc extends AutoCloseable {

    String getDatabase();

    @Override
    void close();
}
