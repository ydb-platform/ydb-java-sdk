package tech.ydb.topic.impl;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class DebugToolsTest {
    @Test
    public void createDebugIdTest() {
        Assert.assertEquals("custom-id", DebugTools.createDebugId("custom-id"));

        String newID = DebugTools.createDebugId(null);
        Assert.assertNotNull(newID);
        Assert.assertEquals(6, newID.length());
    }
}
