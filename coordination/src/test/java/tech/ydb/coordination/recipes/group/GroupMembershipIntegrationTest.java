package tech.ydb.coordination.recipes.group;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        return getGroupMembership(testName, testName);
    }

    private GroupMembership getGroupMembership(
            String coordinationNodePath,
            String groupName
    ) {
        client.createNode(coordinationNodePath).join().expectSuccess(
                "cannot create coordination node on path: " + coordinationNodePath
        );
        return new GroupMembership(
                client,
                coordinationNodePath,
                groupName
        );
    }

    @Test
    public void successTest() throws Exception {
        String testName = "successTest";

        GroupMembership groupMembership = getGroupMembership(testName);
        groupMembership.start();


        AwaitAssert.await().until(() -> {
            if (groupMembership.getCurrentMembers() == null) {
                return false;
            }
            return groupMembership.getCurrentMembers().size() == 1;
        });

        groupMembership.close();
    }

}
