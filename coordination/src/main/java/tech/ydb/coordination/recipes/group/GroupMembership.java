package tech.ydb.coordination.recipes.group;

import java.io.Closeable;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import tech.ydb.coordination.recipes.util.Listenable;
import tech.ydb.coordination.recipes.util.SessionListenableProvider;

public interface GroupMembership extends Closeable, SessionListenableProvider {
    /**
     * Adds instance to the group and start observing members
     */
    void start();

    /**
     * Get cached members of the group or null
     */
    @Nullable
    List<GroupMember> getCurrentMembers();

    /**
     * Get listenable to subscribe to members list update
     */
    Listenable<List<GroupMember>> getMembersListenable();
}
