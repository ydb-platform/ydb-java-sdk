package tech.ydb.topic.write.impl;

import java.time.Instant;
import java.util.List;

import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.topic.description.MetadataItem;
import tech.ydb.topic.write.Message;

public class MessageMeta {
    private final Long userSeqNo;
    private final Instant createdAt;
    private final int uncompressedSize;
    private final List<MetadataItem> items;
    private final YdbTransaction transaction;

    public MessageMeta(Message message, YdbTransaction transaction) {
        this.userSeqNo = message.getSeqNo();
        this.createdAt = message.getCreateTimestamp();
        this.uncompressedSize = message.getData().length;
        this.items = message.getMetadataItems();
        this.transaction = transaction;
    }

    public int getUncompressedSize() {
        return uncompressedSize;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<MetadataItem> getItems() {
        return items;
    }

    public Long getUserSeqNo() {
        return userSeqNo;
    }

    public YdbTransaction getTransaction() {
        return transaction;
    }
}
