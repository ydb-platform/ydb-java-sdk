package tech.ydb.coordination.recipes.group;

import java.util.List;
import java.util.concurrent.Executors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.common.retry.RetryForever;
import tech.ydb.coordination.AwaitAssert;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.test.junit4.GrpcTransportRule;

public class GroupMembershipIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(GroupMembershipIntegrationTest.class);

    @ClassRule
    public static final GrpcTransportRule ydbRule = new GrpcTransportRule();

    private static CoordinationClient client;

    @BeforeClass
    public static void init() {
        client = CoordinationClient.newClient(ydbRule);
    }

    @AfterClass
    public static void clean() {
        ydbRule.close();
    }

    private GroupMembership getGroupMembership(String testName) {
        return getGroupMembership(testName, testName, testName.getBytes());
    }

    private GroupMembership getGroupMembership(
            String coordinationNodePath,
            String groupName,
            byte[] data
    ) {
        client.createNode(coordinationNodePath).join().expectSuccess(
                "cannot create coordination node on path: " + coordinationNodePath
        );
        return new GroupMembership(
                client,
                coordinationNodePath,
                groupName,
                data
        );
    }

    private GroupMembership getGroupMembershipCustom(
            String coordinationNodePath,
            String groupName,
            byte[] data,
            GroupMembershipSettings settings
    ) {
        client.createNode(coordinationNodePath).join().expectSuccess(
                "cannot create coordination node on path: " + coordinationNodePath
        );
        return new GroupMembership(
                client,
                coordinationNodePath,
                groupName,
                data,
                settings
        );
    }

    @Test
    public void successTest() throws Exception {
        String testName = "successTest";

        GroupMembership groupMembership = getGroupMembership(testName);

        groupMembership.getSessionListenable().addListener(
                state -> logger.info("State change: " + state)
        );
        groupMembership.getMembersListenable().addListener(
                groupMembers -> logger.info("Members change: " + groupMembers)
        );

        groupMembership.start();

        AwaitAssert.await().until(() -> {
            if (groupMembership.getCurrentMembers() == null) {
                return false;
            }
            return groupMembership.getCurrentMembers().size() == 1;
        });

        groupMembership.close();
    }

    @Test
    public void everyTest() throws Exception {
        String testName = "everyTest";

        GroupMembership groupMembership = getGroupMembershipCustom(
                testName,
                testName,
                testName.getBytes(),
                GroupMembershipSettings.newBuilder()
                        .withRetryPolicy(new RetryForever(100))
                        .withScheduledExecutor(Executors.newSingleThreadScheduledExecutor())
                        .build()
        );
        groupMembership.start();


        AwaitAssert.await().until(() -> {
            if (groupMembership.getCurrentMembers() == null) {
                return false;
            }
            return groupMembership.getCurrentMembers().size() == 1;
        });

        List<GroupMember> currentMembers = groupMembership.getCurrentMembers();
        GroupMember groupMember1 = currentMembers.get(0);
        logger.info(groupMember1.toString());

        Assert.assertEquals(1L, groupMember1.getSessionId());
        Assert.assertArrayEquals(groupMember1.getData(), testName.getBytes());
        GroupMember groupMember2 = new GroupMember(1L, testName.getBytes());
        Assert.assertEquals(groupMember1, groupMember2);
        Assert.assertEquals(groupMember1.hashCode(), groupMember2.hashCode());

        groupMembership.close();
    }

}
