package tech.ydb.topic.description;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface for custom codec implementation.
 * <p>
 * You can use custom codec as below
 * 1. Implement interface methods
 * 2. Use in write data
 *   CustomTopicCodec customCodecImpl = ....
 *   Topic client = TopicClient.newClient(ydbTransport).build();
 * <p>
 *   client.registerCodec(10113, customCodecImpl);
 *   WriterSettings settings = WriterSettings.newBuilder()
 *                 .setTopicPath(topicName)
 *                 .setCodec(codecId)
 *                 .build();
 * <p>
 *    SyncWriter writer = client.createSyncWriter(settings);
 * <p>
 * 3. Use in read data
 *   CustomTopicCodec customCodecImpl = ....
 *   Topic client = TopicClient.newClient(ydbTransport).build();
 * <p>
 *   ReaderSettings readerSettings = ReaderSettings.newBuilder()
 *                  .addTopic(TopicReadSettings.newBuilder().setPath(topicName).build())
 *                  .setConsumerName(TEST_CONSUMER1)
 *                  .build();
 * <p>
 *   SyncReader reader = client.createSyncReader(readerSettings);
 *
 */
public interface CustomTopicCodec {

    /**
     * Decode data
     *
     * @param byteArrayOutputStream input stream
     * @return output stream
     * @throws IOException throws when error occurs
     */
    InputStream decode(ByteArrayInputStream byteArrayOutputStream) throws IOException;

    /**
     * Encode data
     *
     * @param byteArrayInputStream input stream
     * @return output stream
     * @throws IOException throws when error occurs
     */
    OutputStream encode(ByteArrayOutputStream byteArrayInputStream) throws IOException;
}
