package tech.ydb.test.junit5;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
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
public class GrpcTransportExtention extends ProxyGrpcTransport implements InvocationInterceptor,
        AfterAllCallback, AfterEachCallback, BeforeAllCallback, BeforeEachCallback {
    private static final Logger logger = LoggerFactory.getLogger(GrpcTransportExtention.class);

    private final Holder holder = new Holder();

    @Override
    protected GrpcTransport origin() {
        return holder.transport();
    }

    @Override
    public void interceptTestMethod(
            InvocationInterceptor.Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (holder.transport() == null) {
            logger.info("Test {} skipped because ydb helper is not available", extensionContext.getDisplayName());
            invocation.skip();
        } else {
            invocation.proceed();
        }
    }

    @Override
    public void beforeAll(ExtensionContext ec) throws Exception {
        holder.before(ec);
    }

    @Override
    public void afterAll(ExtensionContext ec) throws Exception {
        holder.after(ec);
    }

    @Override
    public void beforeEach(ExtensionContext ec) throws Exception {
        holder.before(ec);
    }

    @Override
    public void afterEach(ExtensionContext ec) throws Exception {
        holder.after(ec);
    }

    private class Holder {
        private YdbHelper helper = null;
        private GrpcTransport transport = null;
        private ExtensionContext context = null;

        public synchronized void before(ExtensionContext ec) {
            if (helper != null) {
                return;
            }

            YdbHelperFactory factory = YdbHelperFactory.getInstance();
            logger.debug("create ydb helper for test {}", ec.getDisplayName());
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

                transport = helper.createTransport(path);
            }
        }

        public synchronized void after(ExtensionContext ec) {
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
        }

        public synchronized GrpcTransport transport() {
            return transport;
        }
    }
}
