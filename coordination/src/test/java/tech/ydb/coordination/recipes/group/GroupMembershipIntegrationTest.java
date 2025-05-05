package tech.ydb.coordination.recipes.group;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.common.retry.RetryForever;
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

    private GroupMembershipImpl getGroupMembership(String testName) {
        return getGroupMembership(testName, testName);
    }

    private GroupMembershipImpl getGroupMembership(
            String coordinationNodePath,
            String groupName
    ) {
        client.createNode(coordinationNodePath).join().expectSuccess(
                "cannot create coordination node on path: " + coordinationNodePath
        );
        return new GroupMembershipImpl(
                client,
                coordinationNodePath,
                groupName,
                new RetryForever(100)
        );
    }

    @Test
    public void successTest() throws Exception {
        String testName = "successTest";

        GroupMembershipImpl groupMembership = getGroupMembership(testName);
        groupMembership.start();

        List<GroupMember> currentMembers = groupMembership.getCurrentMembers();
        Assert.assertEquals(1, currentMembers.size());

        groupMembership.close();
    }

}
