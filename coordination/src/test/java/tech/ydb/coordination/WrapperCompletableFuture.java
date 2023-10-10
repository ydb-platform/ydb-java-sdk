//package tech.ydb.coordination;
//
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.atomic.AtomicReference;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * @author Kirill Kurdyukov
// */
//public class WrapperCompletableFuture<T> {
//
//    private static final Logger logger = LoggerFactory.getLogger(WrapperCompletableFuture.class);
//    public final AtomicReference<CompletableFuture<T>> futureAtomicReference;
//
//    public WrapperCompletableFuture() {
//        futureAtomicReference = new AtomicReference<>(new CompletableFuture<>());
//    }
//
//    public void complete(T endpoint) {
//        logger.info("Completing endpoint: {}", endpoint);
//
//        futureAtomicReference.get().complete(endpoint);
//    }
//
//    public T join() {
//        return futureAtomicReference.get().join();
//    }
//
//    public void clear() {
//        futureAtomicReference.set(new CompletableFuture<>());
//    }
//}
