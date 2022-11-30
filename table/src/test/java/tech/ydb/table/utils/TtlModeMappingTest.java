package tech.ydb.table.utils;

import org.junit.Assert;
import org.junit.Test;
import tech.ydb.table.YdbTable;
import tech.ydb.table.description.TableTtl;

public class TtlModeMappingTest {
    @Test
    public void protoMappingTest() {
        YdbTable.TtlSettings.ModeCase[] apiValues = YdbTable.TtlSettings.ModeCase.values();
        for (YdbTable.TtlSettings.ModeCase apiValue : apiValues) {
            TableTtl.TtlMode sdkTtlMode = TableTtl.TtlMode.forCase(apiValue.getNumber());
            Assert.assertNotNull(String.format("TtlMode not defined for %s", apiValue), sdkTtlMode);
        }
    }
}
