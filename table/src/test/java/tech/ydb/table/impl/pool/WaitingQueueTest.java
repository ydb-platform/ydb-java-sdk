package tech.ydb.table.impl.pool;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class WaitingQueueTest {

    private class Resource {
        private final int id;

        Resource(int id) {
            this.id = id;
        }
    }

    private class ResourceHandler implements WaitingQueue.Handler<Resource> {
        private final AtomicInteger idGenerator = new AtomicInteger(0);
        private final Queue<CompletableFuture<Resource>> futures = new ConcurrentLinkedQueue<>();
        private final Set<Resource> active = new HashSet<>();

        @Override
        public CompletableFuture<Resource> create() {
            CompletableFuture<Resource> next = new CompletableFuture<>();
            futures.offer(next);
            return next;
        }

        @Override
        public void destroy(Resource object) {
            active.remove(object);
        }

        ResourceHandler completeNext() {
            CompletableFuture<Resource> next = futures.poll();
            if (next != null) {
                Resource resource = new Resource(idGenerator.incrementAndGet());
                active.add(resource);
                next.complete(resource);
            }
            return this;
        }

        ResourceHandler completeNextWithException(Exception ex) {
            CompletableFuture<Resource> next = futures.poll();
            if (next != null) {
                next.completeExceptionally(ex);
            }
            return this;
        }
    }

    private class QueueChecker {
        private final WaitingQueue<Resource> queue;

        private QueueChecker(WaitingQueue<Resource> queue) {
            this.queue = queue;
        }

        QueueChecker queueSize(int size) {
            Assert.assertEquals("Check queue size", size, queue.queueSize());
            return this;
        }

        QueueChecker idleSize(int size) {
            Assert.assertEquals("Check idle size", size, queue.idleSize());
            return this;
        }

        QueueChecker waitingsCount(int size) {
            Assert.assertEquals("Check waitings size", size, queue.waitingsSize());
            return this;
        }
    }

    private class ResourceHandlerChecker {
        private final ResourceHandler handler;

        public ResourceHandlerChecker(ResourceHandler handler) {
            this.handler = handler;
        }

        ResourceHandlerChecker requestsCount(int size) {
            Assert.assertEquals("Check pending resources count", size, handler.futures.size());
            return this;
        }

        ResourceHandlerChecker activeCount(int size) {
            Assert.assertEquals("Check active resources count", size, handler.active.size());
            return this;
        }
    }

    private Resource acquireReady(WaitingQueue<Resource> queue) {
        CompletableFuture<Resource> future = new CompletableFuture<>();
        queue.acquire(future);
        Assert.assertTrue("Ready future is done", future.isDone());
        Assert.assertFalse("Ready future is not canceled", future.isCompletedExceptionally());
        return future.join();
    }

    private CompletableFuture<Resource> acquirePending(WaitingQueue<Resource> queue) {
        CompletableFuture<Resource> future = new CompletableFuture<>();
        queue.acquire(future);
        Assert.assertFalse("Pending future is not done", future.isDone());
        return future;
    }

    private void acquireRejectedLimitExceeded(WaitingQueue<Resource> queue) {
        CompletableFuture<Resource> future = new CompletableFuture<>();
        queue.acquire(future);
        Assert.assertTrue("Rejected future is done", future.isDone());
        Assert.assertFalse("Pending future is canceled", future.isCancelled());
        Assert.assertTrue("Pending future is rejected", future.isCompletedExceptionally());
        future.whenComplete((obj, tw) -> {
            Assert.assertNull("Future result is null", obj);
            Assert.assertNotNull("Future exception is not null", tw);
            Assert.assertEquals("Exception message", "Objects limit exceeded", tw.getMessage());
        });
    }

    private void acquireRejectedIllegalState(WaitingQueue<Resource> queue) {
        CompletableFuture<Resource> future = new CompletableFuture<>();
        queue.acquire(future);
        Assert.assertTrue("Rejected future is done", future.isDone());
        Assert.assertFalse("Pending future is canceled", future.isCancelled());
        Assert.assertTrue("Pending future is rejected", future.isCompletedExceptionally());
        future.whenComplete((obj, tw) -> {
            Assert.assertNull("Future result is null", obj);
            Assert.assertNotNull("Future exception is not null", tw);
            Assert.assertEquals("Exception message", "Pool is already closed", tw.getMessage());
        });
    }

    private QueueChecker check(WaitingQueue<Resource> queue) {
        return new QueueChecker(queue);
    }

    private ResourceHandlerChecker check(ResourceHandler rs) {
        return new ResourceHandlerChecker(rs);
    }

    private Resource pendingIsReady(CompletableFuture<Resource> future) {
        Assert.assertTrue("Pending future is done", future.isDone());
        Assert.assertFalse("Pending future is valid", future.isCompletedExceptionally());
        return future.join();
    }

    private void pendingIsWaiting(CompletableFuture<Resource> future) {
        Assert.assertFalse("Pending future is waiting", future.isDone());
    }

    private void pendingIsCanceled(CompletableFuture<Resource> future) {
        Assert.assertTrue("Pending future is done", future.isDone());
        Assert.assertTrue("Pending future is canceled", future.isCancelled());
        Assert.assertTrue("Pending future is execptionally", future.isCompletedExceptionally());
    }

    private void pendingIsExeceptionally(CompletableFuture<Resource> future, String exceptionMessage) {
        Assert.assertTrue("Pending future is done", future.isDone());
        Assert.assertTrue("Pending future is rejected", future.isCompletedExceptionally());
        future.whenComplete((obj, tw) -> {
            Assert.assertNull("Future result is null", obj);
            Assert.assertNotNull("Future exception is not null", tw);
            Assert.assertEquals("Exception message", exceptionMessage, tw.getMessage());
        });
    }

    @Test
    public void simpleWithoutWaitingTest() {
        ResourceHandler rs = new ResourceHandler();
        // Queue with size = 1
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 1, 0);
        check(queue).queueSize(0).idleSize(0);
        check(rs).requestsCount(0).activeCount(0);

        // first acquire is pending 
        CompletableFuture<Resource> first = acquirePending(queue);
        check(queue).queueSize(1).idleSize(0);
        // second acquire is rejected
        acquireRejectedLimitExceeded(queue);

        check(rs).requestsCount(1).activeCount(0);
        rs.completeNext();
        check(rs).requestsCount(0).activeCount(1);
        check(queue).queueSize(1).idleSize(0);

        Resource r1 = pendingIsReady(first);
        // next acquire already will be rejected
        acquireRejectedLimitExceeded(queue);

        queue.release(r1);
        check(queue).queueSize(1).idleSize(1);

        // after release next acquire will be returned immediately
        Resource r2 = acquireReady(queue);
        check(queue).queueSize(1).idleSize(0);
        // but next acquire will be rejected
        acquireRejectedLimitExceeded(queue);

        Assert.assertEquals("Pool returned the same resource", r1, r2);

        check(rs).requestsCount(0).activeCount(1);
        check(queue).queueSize(1).idleSize(0);
        queue.delete(r2);
        check(rs).requestsCount(0).activeCount(0);

        // afrer deleting resource pool is empty
        CompletableFuture<Resource> second = acquirePending(queue);
        // next acquire is rejected
        acquireRejectedLimitExceeded(queue);

        check(queue).queueSize(1).idleSize(0);
        check(rs).requestsCount(1).activeCount(0);
        rs.completeNext();
        check(queue).queueSize(1).idleSize(0);
        check(rs).requestsCount(0).activeCount(1);

        Resource r3 = pendingIsReady(second);
        Assert.assertNotEquals("Pool returned different resource", r1, r3);

        queue.release(r3);
        check(queue).queueSize(1).idleSize(1);

        queue.close();

        check(queue).queueSize(0).idleSize(0);
        check(rs).requestsCount(0).activeCount(0);
    }

    @Test
    public void canceledPendingTest() {
        ResourceHandler rs = new ResourceHandler();
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 1, 0);

        // first acquire is pending 
        CompletableFuture<Resource> first = acquirePending(queue);

        check(queue).queueSize(1).idleSize(0);

        // After cancelation future resource will go to idle
        first.cancel(true);
        rs.completeNext();
        check(queue).queueSize(1).idleSize(1);
        
        // And resource can be acquired from idle immediately
        Resource r1 = acquireReady(queue);
        check(queue).queueSize(1).idleSize(0);

        queue.delete(r1);
        check(queue).queueSize(0).idleSize(0);

        queue.close();

        check(queue).queueSize(0).idleSize(0);
        check(rs).requestsCount(0).activeCount(0);
    }

    @Test
    public void doubleReleaseTest() {
        ResourceHandler rs = new ResourceHandler();
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 1, 0);

        // first acquire is pending 
        CompletableFuture<Resource> first = acquirePending(queue);
        rs.completeNext();
        Resource one = pendingIsReady(first);

        // Double release don't break queue
        queue.release(one);
        queue.release(one);

        Resource two = acquireReady(queue);
        Assert.assertEquals("Pool returned the same resource", one, two);

        queue.release(one);
        queue.close();

        check(queue).queueSize(0).idleSize(0);
        check(rs).requestsCount(0).activeCount(0);
    }

    @Test
    public void doubleDeleteTest() {
        ResourceHandler rs = new ResourceHandler();
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 1, 0);

        // first acquire is pending 
        CompletableFuture<Resource> first = acquirePending(queue);
        rs.completeNext();

        Resource one = pendingIsReady(first);

        // Double release don't break queue
        queue.delete(one);
        queue.delete(one);

        CompletableFuture<Resource> second = acquirePending(queue);
        rs.completeNext();

        Resource two = pendingIsReady(second);
        Assert.assertNotEquals("Pool returned the differents resources", one, two);

        queue.delete(two);
        queue.close();

        check(queue).queueSize(0).idleSize(0);
        check(rs).requestsCount(0).activeCount(0);
    }

    @Test
    public void deleteReleasedTest() {
        ResourceHandler rs = new ResourceHandler();
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 1, 0);

        // first acquire is pending 
        CompletableFuture<Resource> first = acquirePending(queue);
        rs.completeNext();

        Resource one = pendingIsReady(first);

        queue.release(one);
        // Delete after releasing will be ignored
        queue.delete(one);

        Resource two = acquireReady(queue);
        Assert.assertEquals("Pool returned the same resource", one, two);

        queue.release(two);
        queue.close();

        check(queue).queueSize(0).idleSize(0);
        check(rs).requestsCount(0).activeCount(0);
    }

    @Test
    public void releaseDeletedTest() {
        ResourceHandler rs = new ResourceHandler();
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 1, 0);

        // first acquire is pending 
        CompletableFuture<Resource> first = acquirePending(queue);
        rs.completeNext();

        Resource one = pendingIsReady(first);
        acquireRejectedLimitExceeded(queue);

        queue.delete(one);
        // Delete after releasing will be ignored
        queue.delete(one);

        CompletableFuture<Resource> second = acquirePending(queue);
        rs.completeNext();

        Resource two = pendingIsReady(second);
        Assert.assertNotEquals("Pool returned the differents resources", one, two);

        queue.delete(two);
        queue.close();

        check(queue).queueSize(0).idleSize(0);
        check(rs).requestsCount(0).activeCount(0);
    }

    @Test
    public void releaseAfterCloseTest() {
        ResourceHandler rs = new ResourceHandler();
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 1, 0);

        // first acquire is pending 
        CompletableFuture<Resource> first = acquirePending(queue);
        rs.completeNext();

        Resource one = pendingIsReady(first);
        acquireRejectedLimitExceeded(queue);

        check(queue).queueSize(1).idleSize(0);
        check(rs).requestsCount(0).activeCount(1);

        // Closing of queue doesn't affect to active objects
        queue.close();

        // But new acquires will be rejected
        acquireRejectedIllegalState(queue);
        check(queue).queueSize(1).idleSize(0);
        check(rs).requestsCount(0).activeCount(1);

        // After object releasing it will be cleaned
        queue.release(one);

        check(queue).queueSize(0).idleSize(0);
        check(rs).requestsCount(0).activeCount(0);
    }

    @Test
    public void deleteAfterCloseTest() {
        ResourceHandler rs = new ResourceHandler();
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 1, 0);

        // first acquire is pending 
        CompletableFuture<Resource> first = acquirePending(queue);
        check(rs).requestsCount(1).activeCount(0);
        rs.completeNext();
        check(rs).requestsCount(0).activeCount(1);

        Resource one = pendingIsReady(first);
        acquireRejectedLimitExceeded(queue);
        check(rs).requestsCount(0).activeCount(1);

        // Closing of queue doesn't affect to active objects
        queue.close();
        check(rs).requestsCount(0).activeCount(1);
        check(queue).queueSize(1).idleSize(0);

        // But new acquires will be rejected
        acquireRejectedIllegalState(queue);

        // After object deleting it will be cleaned
        queue.delete(one);

        check(queue).queueSize(0).idleSize(0);
        check(rs).requestsCount(0).activeCount(0);
    }

    @Test
    public void pendingAfterCloseTest() {
        ResourceHandler rs = new ResourceHandler();
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 1, 0);

        // first acquire is pending 
        CompletableFuture<Resource> first = acquirePending(queue);
        check(rs).requestsCount(1).activeCount(0);
        check(queue).queueSize(1).idleSize(0);

        // Closing of queue doesn't affect to active objects, but clean queue stats
        queue.close();
        check(rs).requestsCount(1).activeCount(0);
        check(queue).queueSize(0).idleSize(0);

        pendingIsWaiting(first);

        // After complete obejct to closed queue
        // object will be released and future will be canceled
        rs.completeNext();
        pendingIsCanceled(first);

        check(queue).queueSize(0).idleSize(0);
        check(rs).requestsCount(0).activeCount(0);
    }

    @Test
    public void exceptionallyResources() {
        ResourceHandler rs = new ResourceHandler();
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 5, 0);
        check(queue).queueSize(0).idleSize(0);
        check(rs).requestsCount(0).activeCount(0);
        
        CompletableFuture<Resource> f1 = acquirePending(queue);
        CompletableFuture<Resource> f2 = acquirePending(queue);
        CompletableFuture<Resource> f3 = acquirePending(queue);
        CompletableFuture<Resource> f4 = acquirePending(queue);
        CompletableFuture<Resource> f5 = acquirePending(queue);

        acquireRejectedLimitExceeded(queue);

        check(queue).queueSize(5).idleSize(0);
        check(rs).requestsCount(5).activeCount(0);
        
        rs.completeNextWithException(new Exception("trouble 1"));
        rs.completeNext();
        rs.completeNextWithException(new Exception("trouble 2"));
        rs.completeNext();
        rs.completeNextWithException(new Exception("trouble 3"));
        
        pendingIsExeceptionally(f1, "trouble 1");
        pendingIsExeceptionally(f3, "trouble 1");
        pendingIsExeceptionally(f5, "trouble 1");
        
        Resource r2 = pendingIsReady(f2);
        Resource r4 = pendingIsReady(f4);
        Assert.assertNotEquals("Get different resources", r2, r4);
        
        check(queue).queueSize(2).idleSize(0);
        check(rs).requestsCount(0).activeCount(2);
        
        queue.release(r2);
        queue.release(r4);

        check(queue).queueSize(2).idleSize(2);
        check(rs).requestsCount(0).activeCount(2);
        
        queue.close();
        check(queue).queueSize(0).idleSize(0);
        check(rs).requestsCount(0).activeCount(0);
    }

    @Test
    public void exceptionallyPendingAfterCloseTest() {
        ResourceHandler rs = new ResourceHandler();
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 1, 0);

        // first acquire is pending 
        CompletableFuture<Resource> first = acquirePending(queue);
        check(rs).requestsCount(1).activeCount(0);
        check(queue).queueSize(1).idleSize(0);

        // Closing of queue doesn't affect to active objects, but clean queue stats
        queue.close();
        check(rs).requestsCount(1).activeCount(0);
        check(queue).queueSize(0).idleSize(0);

        pendingIsWaiting(first);

        // After complete obejct to closed queue
        // object will be released and future will be canceled
        rs.completeNextWithException(new RuntimeException("big problem"));
        pendingIsExeceptionally(first, "big problem");
        
        check(queue).queueSize(0).idleSize(0);
        check(rs).requestsCount(0).activeCount(0);
    }

    @Test
    public void simpleWaitingRequests() {
        ResourceHandler rs = new ResourceHandler();
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 1);
        
        Assert.assertEquals("Validate queue limit", 1, queue.queueLimit());
        Assert.assertEquals("Validate waitings limit", WaitingQueue.WAITINGS_LIMIT_FACTOR, queue.waitingsLimit());

        check(rs).requestsCount(0).activeCount(0);
        check(queue).queueSize(0).idleSize(0).waitingsCount(0);

        // Make first request and get pending future
        CompletableFuture<Resource> first = acquirePending(queue);
        check(queue).queueSize(1).idleSize(0).waitingsCount(0);

        // Make few requests for fill waiting queue
        Deque<CompletableFuture<Resource>> waitings = new ArrayDeque<>(queue.waitingsLimit());
        for (int idx = 0; idx < queue.waitingsLimit(); idx += 1) {
            waitings.offer(acquirePending(queue));
        }

        check(rs).requestsCount(1).activeCount(0);
        check(queue).queueSize(1).idleSize(0).waitingsCount(WaitingQueue.WAITINGS_LIMIT_FACTOR);

        // When wainting queue is filled - next poll must be rejected
        acquireRejectedLimitExceeded(queue);

        // Complete first pending resource
        rs.completeNext();
        check(rs).requestsCount(0).activeCount(1);
        check(queue).queueSize(1).idleSize(0).waitingsCount(WaitingQueue.WAITINGS_LIMIT_FACTOR);

        Resource r1 = pendingIsReady(first);
        Resource next = r1;
        while (!waitings.isEmpty()) {
            // After release resource must be moved to next waiting future
            queue.release(next);
            CompletableFuture<Resource> waiting = waitings.poll();
            next = pendingIsReady(waiting);
            
            Assert.assertEquals("All waitings got single resource ", r1, next);
            check(rs).requestsCount(0).activeCount(1);
        }
        
        queue.delete(next);
        
        queue.close();

        check(queue).queueSize(0).idleSize(0).waitingsCount(0);
        check(rs).requestsCount(0).activeCount(0);
    }

    @Test
    public void canceledWaitingTest() {
        ResourceHandler rs = new ResourceHandler();
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 1, 3);

        CompletableFuture<Resource> f1 = acquirePending(queue);
        CompletableFuture<Resource> w1 = acquirePending(queue);
        CompletableFuture<Resource> w2 = acquirePending(queue);
        CompletableFuture<Resource> w3 = acquirePending(queue);

        check(queue).queueSize(1).idleSize(0).waitingsCount(3);
        rs.completeNext();
        
        Resource r1 = pendingIsReady(f1);
        queue.release(r1);

        check(queue).queueSize(1).idleSize(0).waitingsCount(2);
        
        Resource r2 = pendingIsReady(w1);
        Assert.assertEquals("Next waiting got same resource ", r1, r2);

        // Cancelation of waiting doesn't affect to queue
        w2.cancel(true);
        check(queue).queueSize(1).idleSize(0).waitingsCount(2);

        // Canceled waiting must be deleted when queue process next released resource
        queue.release(r2);

        check(queue).queueSize(1).idleSize(0).waitingsCount(0);
        Resource r3 = pendingIsReady(w3);
        Assert.assertEquals("Next waiting got same resource ", r1, r3);
        
        queue.delete(r3);
        queue.close();

        check(queue).queueSize(0).idleSize(0).waitingsCount(0);
        check(rs).requestsCount(0).activeCount(0);
    }

    @Test
    public void checkWaitingAfterDeleteTest() {
        ResourceHandler rs = new ResourceHandler();
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 1, 3);

        CompletableFuture<Resource> f1 = acquirePending(queue);
        CompletableFuture<Resource> w1 = acquirePending(queue);
        CompletableFuture<Resource> w2 = acquirePending(queue);
        CompletableFuture<Resource> w3 = acquirePending(queue);

        check(queue).queueSize(1).idleSize(0).waitingsCount(3);
        rs.completeNext();
        check(rs).requestsCount(0).activeCount(1);
        
        Resource r1 = pendingIsReady(f1);

        // After deleting current resource queue must create new pending to complete waiting
        queue.delete(r1);

        check(rs).requestsCount(1).activeCount(0);
        check(queue).queueSize(1).idleSize(0).waitingsCount(3);
        pendingIsWaiting(w1);
        pendingIsWaiting(w2);
        pendingIsWaiting(w3);
        
        rs.completeNext();
        check(rs).requestsCount(0).activeCount(1);
        check(queue).queueSize(1).idleSize(0).waitingsCount(2);
        
        Resource r2 = pendingIsReady(w1);
        pendingIsWaiting(w2);

        Assert.assertNotEquals("After deleting waiting got different resource ", r1, r2);
        
        queue.delete(r2);
        check(rs).requestsCount(1).activeCount(0);

        // If pending completed with exception - queue must repeat it
        rs.completeNextWithException(new RuntimeException("Trouble"));
        check(rs).requestsCount(1).activeCount(0);

        rs.completeNext();
        
        Resource r3 = pendingIsReady(w2);
        pendingIsWaiting(w3);

        Assert.assertNotEquals("After deleting waiting got different resource ", r1, r2);
        check(rs).requestsCount(0).activeCount(1);
        check(queue).queueSize(1).idleSize(0).waitingsCount(1);
        
        queue.delete(r3);

        // After canceling of pending waiting queue will move resource to idle
        check(rs).requestsCount(1).activeCount(0);
        w3.cancel(true);
        rs.completeNext();

        check(rs).requestsCount(0).activeCount(1);
        check(queue).queueSize(1).idleSize(1).waitingsCount(0);

        queue.close();
    }

    @Test
    public void cleanWaitingAfterClosingTest() {
        ResourceHandler rs = new ResourceHandler();
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 1, 2);

        CompletableFuture<Resource> f1 = acquirePending(queue);
        CompletableFuture<Resource> w1 = acquirePending(queue);

        rs.completeNext();
        Resource r1 = pendingIsReady(f1);

        // After closing all waitings will be canceled
        queue.close();
        
        check(rs).requestsCount(0).activeCount(1);
        
        pendingIsCanceled(w1);
        
        queue.release(r1);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void validateQueueMaxSize() {
        (new WaitingQueue<>(new ResourceHandler(), 0, 3)).getClass();
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateQueueHandler() {
        (new WaitingQueue<>(null, 1)).getClass();
    }
    
    @Test
    public void testColdIterator() {
        ResourceHandler rs = new ResourceHandler();
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 5);

        CompletableFuture<Resource> f1 = acquirePending(queue);
        CompletableFuture<Resource> f2 = acquirePending(queue);
        CompletableFuture<Resource> f3 = acquirePending(queue);
        
        rs.completeNext().completeNext().completeNext();
        
        Resource r1 = pendingIsReady(f1);
        Resource r2 = pendingIsReady(f2);
        Resource r3 = pendingIsReady(f3);
        
        Iterator<Resource> cold = queue.coldIterator();
        Assert.assertFalse("All of resources in use, nothing is cold", cold.hasNext());
        
        queue.release(r1);
        queue.release(r3);
        queue.release(r2);
        
        cold = queue.coldIterator();

        Assert.assertTrue("There is next cold resource", cold.hasNext());
        Assert.assertEquals("Next cold resource is r1", r1, cold.next());
        Assert.assertTrue("There is next cold resource", cold.hasNext());
        Assert.assertEquals("Next cold resource is r3", r3, cold.next());
        Assert.assertTrue("There is next cold resource", cold.hasNext());
        Assert.assertEquals("Next cold resource is r2", r2, cold.next());
        
        Assert.assertFalse("There isn't cold resource", cold.hasNext());

        queue.close();
    }

    @Test
    public void testRemoveColdResources() {
        ResourceHandler rs = new ResourceHandler();
        WaitingQueue<Resource> queue = new WaitingQueue<>(rs, 5);

        CompletableFuture<Resource> f1 = acquirePending(queue);
        CompletableFuture<Resource> f2 = acquirePending(queue);
        CompletableFuture<Resource> f3 = acquirePending(queue);
        
        rs.completeNext().completeNext().completeNext();
        check(rs).activeCount(3);
        
        check(queue).queueSize(3).idleSize(0);
        Resource r1 = pendingIsReady(f1);
        Resource r2 = pendingIsReady(f2);
        Resource r3 = pendingIsReady(f3);
        
        Iterator<Resource> cold = queue.coldIterator();
        Assert.assertFalse("All of resources in use, nothing is cold", cold.hasNext());
        // remove without next() do nothing
        cold.remove();
        
        queue.release(r1);
        
        check(queue).queueSize(3).idleSize(1);
        cold = queue.coldIterator();
        Assert.assertTrue("There is next cold resource", cold.hasNext());
        Assert.assertEquals("Next cold resource is r1", r1, cold.next());
        cold.remove();

        check(rs).activeCount(2);
        check(queue).queueSize(2).idleSize(0);
        Assert.assertFalse("There isn't cold resource", cold.hasNext());

        queue.release(r2);
        queue.release(r3);

        check(queue).queueSize(2).idleSize(2);
        cold = queue.coldIterator();

        Assert.assertTrue("There is next cold resource", cold.hasNext());
        Assert.assertEquals("Next cold resource is r2", r2, cold.next());
        Assert.assertTrue("There is next cold resource", cold.hasNext());
        Assert.assertEquals("Next cold resource is r3", r3, cold.next());
        
        check(queue).queueSize(2).idleSize(2);
        // acquire the hottest resource r3
        Resource r4 = acquireReady(queue);
        check(queue).queueSize(2).idleSize(1);
        
        // try to remove already used resource - nothing is changed
        cold.remove();
        check(rs).activeCount(2);
        check(queue).queueSize(2).idleSize(1);
        
        queue.delete(r4);
        
        queue.close();
    }
}
