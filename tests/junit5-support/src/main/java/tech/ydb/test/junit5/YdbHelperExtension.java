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

import tech.ydb.test.integration.YdbHelper;
import tech.ydb.test.integration.YdbHelperFactory;
import tech.ydb.test.integration.utils.ProxyYdbHelper;

/**
 * @author Aleksandr Gorshenin
 */
public class YdbHelperExtension extends ProxyYdbHelper implements BeforeAllCallback, AfterAllCallback,
        BeforeEachCallback, AfterEachCallback {
    private static final Logger logger = LoggerFactory.getLogger(GrpcTransportExtension.class);

    private final Holder holder = new Holder();

    @Override
    protected YdbHelper origin() {
        return holder.helper();
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
        private ExtensionContext context = null;

        public void before(ExtensionContext ec) {
            holderLock.lock();
            try {
                if (helper != null) {
                    return;
                }

                YdbHelperFactory factory = YdbHelperFactory.getInstance();
                logger.debug("create ydb helper for test {}", ec.getDisplayName());
                helper = factory.createHelper();

                if (helper != null) {
                    context = ec;
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

                if (helper != null) {
                    helper.close();
                    helper = null;
                }

                context = null;
            } finally {
                holderLock.unlock();
            }
        }

        public YdbHelper helper() {
            holderLock.lock();
            try {
                return helper;
            } finally {
                holderLock.unlock();
            }
        }
    }
}
