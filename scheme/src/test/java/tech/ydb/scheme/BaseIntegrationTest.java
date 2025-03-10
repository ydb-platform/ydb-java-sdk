package tech.ydb.scheme;

import java.util.Optional;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.scheme.description.DescribePathResult;
import tech.ydb.scheme.description.Entry;
import tech.ydb.scheme.description.EntryType;
import tech.ydb.scheme.description.ListDirectoryResult;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class BaseIntegrationTest {
    @ClassRule
    public final static GrpcTransportRule transport = new GrpcTransportRule();

    private final static SchemeClient client = SchemeClient.newClient(transport).build();

    @AfterClass
    public static void close() {
        client.close();
    }

    @Test
    public void rootPathTest() {
        Result<DescribePathResult> describeRoot = client.describePath("/").join();
        Assert.assertTrue(describeRoot.isSuccess());
        DescribePathResult root = describeRoot.getValue();
        Assert.assertEquals("/", root.getEntry().getName());
        Assert.assertEquals("", root.getEntry().getOwner());

        Result<ListDirectoryResult> listRoot = client.listDirectory("/").join();
        Assert.assertTrue(listRoot.isSuccess());
        ListDirectoryResult listResult = listRoot.getValue();
        Assert.assertEquals("/", listResult.getEntry().getName());
        Assert.assertEquals("", listResult.getEntry().getOwner());
        Assert.assertEquals("/", listResult.getEntry().getName());
        Assert.assertEquals(0l, listResult.getEntry().getSizeBytes());
        Assert.assertEquals(EntryType.DIRECTORY, listResult.getEntry().getType());
        Assert.assertEquals("Entry{name='/', type=DIRECTORY}", listResult.getEntry().toString());


        Assert.assertEquals(1, listResult.getEntryChildren().size());
        listResult.getEntryChildren().get(0);
        Entry firstChild = listResult.getEntryChildren().get(0);
        Assert.assertTrue(transport.getDatabase().startsWith("/" + firstChild.getName()));
        Assert.assertEquals(EntryType.DIRECTORY, firstChild.getType());
    }

    @Test
    public void invalidPathTest() {
        Result<DescribePathResult> describeInvalid = client.describePath("/invalid-path").join();
        Assert.assertFalse(describeInvalid.isSuccess());
        Assert.assertEquals(StatusCode.SCHEME_ERROR, describeInvalid.getStatus().getCode());
        Assert.assertEquals(
                "Status{code = SCHEME_ERROR(code=400070), issues = [Root not found (S_ERROR)]}",
                describeInvalid.getStatus().toString()
        );

        Result<ListDirectoryResult> listInvalid = client.listDirectory("/invalid-path").join();
        Assert.assertFalse(listInvalid.isSuccess());
        Assert.assertEquals(StatusCode.SCHEME_ERROR, listInvalid.getStatus().getCode());
        Assert.assertEquals(
                "Status{code = SCHEME_ERROR(code=400070), issues = [Root not found (S_ERROR)]}",
                listInvalid.getStatus().toString()
        );

        Status makeDirectory= client.makeDirectory("/invalid-path").join();
        Assert.assertFalse(makeDirectory.isSuccess());
        Assert.assertEquals(StatusCode.BAD_REQUEST, makeDirectory.getCode());

        Status removeDirectory= client.removeDirectory("/invalid-path").join();
        Assert.assertFalse(removeDirectory.isSuccess());
        Assert.assertEquals(StatusCode.SCHEME_ERROR, removeDirectory.getCode());
        Assert.assertEquals(
                "Status{code = SCHEME_ERROR(code=400070), issues = [#200200 Path does not exist (S_ERROR)]}",
                removeDirectory.toString()
        );
    }

    @Test
    public void createAndDeleteTest() {
        String basePath = transport.getDatabase();
        String dirName = "test_dir";

        Status dirCreate1 = client.makeDirectory(basePath + "/" + dirName).join();
        Assert.assertTrue(dirCreate1.isSuccess());

        // Directory creating is idempotent
        Status dirCreate2 = client.makeDirectory(basePath + "/" + dirName).join();
        Assert.assertTrue(dirCreate2.isSuccess());

        Result<DescribePathResult> dirEntry = client.describePath(basePath + "/" + dirName).join();
        Assert.assertTrue(dirEntry.isSuccess());
        Assert.assertEquals(dirName, dirEntry.getValue().getEntry().getName());

        Result<ListDirectoryResult> rootList = client.listDirectory(basePath).join();
        Assert.assertTrue(rootList.isSuccess());
        Optional<Entry> child = rootList.getValue().getEntryChildren().stream()
                .filter(e -> dirName.equals(e.getName())).findFirst();
        Assert.assertTrue(child.isPresent());
        Assert.assertEquals(child.get(), dirEntry.getValue().getEntry());

        Status dirDelete = client.removeDirectory(basePath + "/" + dirName).join();
        Assert.assertTrue(dirDelete.isSuccess());
    }
}
