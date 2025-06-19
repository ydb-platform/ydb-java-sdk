package tech.ydb.test.junit5;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.test.integration.YdbHelper;
import tech.ydb.test.integration.YdbHelperFactory;
import tech.ydb.test.integration.utils.ProxyGrpcTransport;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcTransportExtension extends ProxyGrpcTransport implements AfterAllCallback, AfterEachCallback,
        BeforeAllCallback, BeforeEachCallback {
    private static final Logger logger = LoggerFactory.getLogger(GrpcTransportExtension.class);

    private final Holder holder = new Holder();

    @Override
    protected GrpcTransport origin() {
        return holder.transport();
    }

    private void ensureEnabled(String displayName) {
        Assumptions.assumeTrue(YdbHelperFactory.getInstance().isEnabled(), "Ydb helper is disabled " + displayName);
    }

    @Override
    public void beforeAll(ExtensionContext ec) throws Exception {
        ensureEnabled(ec.getDisplayName());
        holder.before(ec);
    }

    @Override
    public void afterAll(ExtensionContext ec) throws Exception {
        ensureEnabled(ec.getDisplayName());
        holder.after(ec);
    }

    @Override
    public void beforeEach(ExtensionContext ec) throws Exception {
        ensureEnabled(ec.getDisplayName());
        holder.before(ec);
    }

    @Override
    public void afterEach(ExtensionContext ec) throws Exception {
        ensureEnabled(ec.getDisplayName());
        holder.after(ec);
    }

    private static class Holder {
        private final Lock holderLock = new ReentrantLock();

        private YdbHelper helper = null;
        private GrpcTransport transport = null;
        private ExtensionContext context = null;

        public void before(ExtensionContext ec) {
            holderLock.lock();
            try {
                if (helper != null) {
                    return;
                }
                YdbHelperFactory factory = YdbHelperFactory.getInstance();
                helper = factory.createHelper();
                if (helper != null) {
                    context = ec;

                    String path = "";
                    if (ec.getTestClass().isPresent()) {
                        path += "/" + ec.getTestClass().get().getName();
                    }
                    if (ec.getTestMethod().isPresent()) {
                        path += "/" + ec.getTestMethod().get().getName();
                    }

                    logger.debug("create ydb helper for path {}", path);
                    transport = helper.createTransport();
                }
            } finally {
                holderLock.unlock();
            }
        }

        public void after(ExtensionContext ec) {
            holderLock.lock();

            try {
                if (context != ec) {
                    return;
                }

                if (transport != null) {
                    transport.close();
                    transport = null;
                }

                if (helper != null) {
                    helper.close();
                    helper = null;
                }

                context = null;
            } finally {
                holderLock.unlock();
            }
        }

        public GrpcTransport transport() {
            holderLock.lock();
            try {
                return transport;
            } finally {
                holderLock.unlock();
            }
        }
    }
}
