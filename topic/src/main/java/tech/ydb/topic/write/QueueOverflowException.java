package tech.ydb.topic.write;

/**
 * @author Nikolay Perfilov
 */
public class QueueOverflowException extends Exception {
    private static final long serialVersionUID = 2807265315663088217L;

    public QueueOverflowException(String errorMessage) {
        super(errorMessage);
    }
}
