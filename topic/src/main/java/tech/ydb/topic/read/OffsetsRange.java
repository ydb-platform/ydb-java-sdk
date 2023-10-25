package tech.ydb.topic.read;

/**
 * @author Nikolay Perfilov
 */
public interface OffsetsRange {
    long getStart();

    long getEnd();
}
