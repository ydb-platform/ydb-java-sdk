package tech.ydb.topic.description;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**

 *
 * Interface for custom codec implementation.
 * <p>
 *
 * You can use custom codec as below
 * 1. Implement interface methods
 *    Specify getId which return value more than 10000. This value identify codec across others
 * 2. Use code below to write data
 *   Codec codecImpl = ....
 *   Topic client = TopicClient.newClient(ydbTransport).build();
 * <p>
 *   client.registerCodec(codecImpl);
 *   WriterSettings settings = WriterSettings.newBuilder()
 *                 .setTopicPath(topicName)
 *                 .setCodec(codecId)
 *                 .build();
 * <p>
 *    SyncWriter writer = client.createSyncWriter(settings);
 * <p>
 * 3. Use to read data. Codec should be registered in {@link CodecRegistry}
 *   Codec codecImpl = ....
 *   Topic client = TopicClient.newClient(ydbTransport).build();
 * <p>
 *   ReaderSettings readerSettings = ReaderSettings.newBuilder()
 *                  .addTopic(TopicReadSettings.newBuilder().setPath(topicName).build())
 *                  .setConsumerName(TEST_CONSUMER1)
 *                  .build();
 * <p>
 *   SyncReader reader = client.createSyncReader(readerSettings);
 *
 * @author Nikolay Perfilov
 */
public interface Codec {
    int RAW = 1;
    int GZIP = 2;
    int LZOP = 3;
    int ZSTD = 4;

    /**
     * Get codec identifier
     * @return codec identifier
     */
    int getId();

    /**
     * Decode data
     *
     * @param byteArrayInputStream input stream
     * @return output stream
     * @throws IOException throws when error occurs
     */

    InputStream decode(InputStream byteArrayInputStream) throws IOException;

    /**
     * Encode data
     *
     * @param byteArrayOutputStream output stream
     * @return output stream
     * @throws IOException throws when error occurs
     */
    OutputStream encode(OutputStream byteArrayOutputStream) throws IOException;

}
