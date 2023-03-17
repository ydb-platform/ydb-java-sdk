package tech.ydb.topic.write;

/**
 * @author Nikolay Perfilov
 */
public class QueueOverflowException extends Exception {
    public QueueOverflowException(String errorMessage) {
        super(errorMessage);
    }
}
