package tech.ydb.table.impl.pool;

/**
 * @author Sergey Polovko
 */
class Resource {

    public enum State {
        OK,
        BROKEN,
        DESTROYED,
    }

    private final int id;
    private volatile State state;
    private volatile boolean keepAlived = false;

    Resource(int id) {
        this.id = id;
        this.state = State.OK;
    }

    int getId() {
        return id;
    }

    State getState() {
        return state;
    }

    boolean isKeepAlived() {
        return keepAlived;
    }

    void markBroken() {
        state = State.BROKEN;
    }

    void markDestroyed() {
        state = State.DESTROYED;
    }

    void markKeepAlived() {
        keepAlived = true;
    }

    @Override
    public String toString() {
        return "Resource{id=" + id + ", state=" + state + ", keepAlived=" + keepAlived + '}';
    }
}
