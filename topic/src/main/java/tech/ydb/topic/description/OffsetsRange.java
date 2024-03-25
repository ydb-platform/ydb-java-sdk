package tech.ydb.topic.description;

/**
 * @author Nikolay Perfilov
 */
public interface OffsetsRange {
    long getStart();

    long getEnd();
}
