package tech.ydb.table.result;

/**
 * @author Sergey Polovko
 */
public interface StructReader {

    int getStructMembersCount();

    String getStructMemberName(int index);

    ValueReader getStructMember(int index);

    ValueReader getStructMember(String name);
}
