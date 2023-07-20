package tech.ydb.query;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface QuerySession extends AutoCloseable {

    @Override
    void close();
}
