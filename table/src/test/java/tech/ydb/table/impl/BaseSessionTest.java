package tech.ydb.table.impl;

import org.junit.Assert;

import tech.ydb.table.description.ColumnFamily;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class BaseSessionTest {

    /**
     * Verifies that the {@link ColumnFamily.Compression} enum declares exactly the expected constants:
     * {@code COMPRESSION_NONE} and {@code COMPRESSION_LZ4}.
     * <p>
     * This check matters because the values are used when mapping to and from the protobuf representation. Any change
     * to the enum would break this test and signals that
     * {@link BaseSession#buildColumnFamily(tech.ydb.table.description.ColumnFamily)} and
     * {@link BaseSession#mapDescribeTable(tech.ydb.core.Result, tech.ydb.table.settings.DescribeTableSettings)}
     * need to be updated as well.
     */
    public void columnFamilyTest() {
        Assert.assertArrayEquals(new ColumnFamily.Compression[] {
            ColumnFamily.Compression.COMPRESSION_NONE,
            ColumnFamily.Compression.COMPRESSION_LZ4
        }, ColumnFamily.Compression.values());
    }
}
