package tech.ydb.table.transaction;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.proto.table.YdbTable.OnlineModeSettings;
import tech.ydb.proto.table.YdbTable.SerializableModeSettings;
import tech.ydb.proto.table.YdbTable.StaleModeSettings;
import tech.ydb.proto.table.YdbTable.TransactionControl;
import tech.ydb.proto.table.YdbTable.TransactionSettings;

/**
 * @author Sergey Polovko
 */
public class TxControlTest {

    @Test
    public void id() {
        TransactionControl.Builder txPb = TransactionControl.newBuilder()
            .setCommitTx(true)
            .setTxId("some-id");

        TxControl<?> tx = TxControl.id("some-id");
        Assert.assertTrue(tx.isCommitTx());
        Assert.assertEquals(txPb.build(), tx.toPb());

        TxControl<?> txNoCommit = tx.setCommitTx(false);
        Assert.assertFalse(txNoCommit.isCommitTx());
        Assert.assertEquals(txPb.setCommitTx(false).build(), txNoCommit.toPb());

        Assert.assertSame(tx, tx.setCommitTx(true));
        Assert.assertSame(txNoCommit, txNoCommit.setCommitTx(false));
    }

    @Test
    public void serializableRw() {
        TransactionControl.Builder txPb = TransactionControl.newBuilder()
            .setCommitTx(true)
            .setBeginTx(TransactionSettings.newBuilder()
                .setSerializableReadWrite(SerializableModeSettings.getDefaultInstance())
                .build());

        TxControl<?> tx = TxControl.serializableRw();
        Assert.assertTrue(tx.isCommitTx());
        Assert.assertEquals(txPb.build(), tx.toPb());

        TxControl<?> txNoCommit = tx.setCommitTx(false);
        Assert.assertFalse(txNoCommit.isCommitTx());
        Assert.assertEquals(txPb.setCommitTx(false).build(), txNoCommit.toPb());

        Assert.assertSame(tx, tx.setCommitTx(true));
        Assert.assertSame(txNoCommit, tx.setCommitTx(false));
        Assert.assertSame(tx, txNoCommit.setCommitTx(true));
        Assert.assertSame(txNoCommit, txNoCommit.setCommitTx(false));
    }

    @Test
    public void staleRo() {
        TransactionControl.Builder txPb = TransactionControl.newBuilder()
            .setCommitTx(true)
            .setBeginTx(TransactionSettings.newBuilder()
                .setStaleReadOnly(StaleModeSettings.getDefaultInstance())
                .build());

        TxControl<?> tx = TxControl.staleRo();
        Assert.assertTrue(tx.isCommitTx());
        Assert.assertEquals(txPb.build(), tx.toPb());

        TxControl<?> txNoCommit = tx.setCommitTx(false);
        Assert.assertFalse(txNoCommit.isCommitTx());
        Assert.assertEquals(txPb.setCommitTx(false).build(), txNoCommit.toPb());

        Assert.assertSame(tx, tx.setCommitTx(true));
        Assert.assertSame(txNoCommit, tx.setCommitTx(false));
        Assert.assertSame(tx, txNoCommit.setCommitTx(true));
        Assert.assertSame(txNoCommit, txNoCommit.setCommitTx(false));
    }

    @Test
    public void onlineRo() {
        TransactionControl.Builder txPb = TransactionControl.newBuilder()
            .setCommitTx(true)
            .setBeginTx(TransactionSettings.newBuilder()
                .setOnlineReadOnly(OnlineModeSettings.getDefaultInstance())
                .build());

        TxControl.TxOnlineRo tx = TxControl.onlineRo();
        Assert.assertFalse(tx.isAllowInconsistentReads());
        Assert.assertTrue(tx.isCommitTx());
        Assert.assertEquals(txPb.build(), tx.toPb());

        TxControl.TxOnlineRo txNoCommit = tx.setCommitTx(false);
        Assert.assertFalse(tx.isAllowInconsistentReads());
        Assert.assertFalse(txNoCommit.isCommitTx());
        Assert.assertEquals(txPb.setCommitTx(false).build(), txNoCommit.toPb());

        Assert.assertSame(tx, tx.setCommitTx(true));
        Assert.assertSame(txNoCommit, tx.setCommitTx(false));
        Assert.assertSame(tx, txNoCommit.setCommitTx(true));
        Assert.assertSame(txNoCommit, txNoCommit.setCommitTx(false));

        TxControl.TxOnlineRo txInconsistentReads = tx.setAllowInconsistentReads(true);
        Assert.assertTrue(txInconsistentReads.isAllowInconsistentReads());
        Assert.assertEquals(TransactionControl.newBuilder()
                .setCommitTx(true)
                .setBeginTx(TransactionSettings.newBuilder()
                    .setOnlineReadOnly(OnlineModeSettings.newBuilder().setAllowInconsistentReads(true)))
                .build(), txInconsistentReads.toPb());

        TxControl.TxOnlineRo txInconsistentReadsNoCommit = txNoCommit.setAllowInconsistentReads(true);
        Assert.assertTrue(txInconsistentReads.isAllowInconsistentReads());
        Assert.assertEquals(TransactionControl.newBuilder()
                .setCommitTx(false)
                .setBeginTx(TransactionSettings.newBuilder()
                    .setOnlineReadOnly(OnlineModeSettings.newBuilder().setAllowInconsistentReads(true)))
                .build(), txInconsistentReadsNoCommit.toPb());

        Assert.assertSame(tx, tx.setAllowInconsistentReads(false));
        Assert.assertSame(txNoCommit, txNoCommit.setAllowInconsistentReads(false));
        Assert.assertSame(txInconsistentReads, txInconsistentReads.setAllowInconsistentReads(true));
        Assert.assertSame(txInconsistentReadsNoCommit, txInconsistentReadsNoCommit.setAllowInconsistentReads(true));
    }
}
