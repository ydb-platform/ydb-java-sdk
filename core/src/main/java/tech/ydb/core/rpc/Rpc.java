package tech.ydb.core.rpc;

/**
 * @author Sergey Polovko
 */
public interface Rpc extends AutoCloseable {

    String getDatabase();

    @Override
    void close();
}
