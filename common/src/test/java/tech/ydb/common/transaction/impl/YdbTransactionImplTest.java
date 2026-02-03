package tech.ydb.common.transaction.impl;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.common.transaction.TxMode;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class YdbTransactionImplTest {
    private class MockTx extends YdbTransactionImpl {

        public MockTx(TxMode txMode, String txId) {
            super(txMode, txId);
        }

        @Override
        public String getSessionId() {
            return "MOCK";
        }
    }

    @Test
    public void baseTest() {
        MockTx tx = new MockTx(TxMode.SNAPSHOT_RO, "test-id");

        Assert.assertEquals("test-id", tx.getId());
        Assert.assertEquals("MOCK", tx.getSessionId());
        Assert.assertEquals(TxMode.SNAPSHOT_RO, tx.getTxMode());
        Assert.assertTrue(tx.isActive());
        Assert.assertFalse(tx.getStatusFuture().isDone());
    }

    @Test
    public void nullTest() {
        MockTx tx = new MockTx(TxMode.NONE, null);

        Assert.assertNull(tx.getId());
        Assert.assertEquals("MOCK", tx.getSessionId());
        Assert.assertEquals(TxMode.NONE, tx.getTxMode());
        Assert.assertFalse(tx.isActive());
        Assert.assertFalse(tx.getStatusFuture().isDone());
    }
}
