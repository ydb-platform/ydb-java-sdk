package tech.ydb.test.junit4;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
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
public class GrpcTransportRule extends ProxyGrpcTransport implements TestRule {
    private static final Logger logger = LoggerFactory.getLogger(GrpcTransportRule.class);

    private final AtomicReference<GrpcTransport> proxy;
    private final boolean skipOnUnavailable;

    private GrpcTransportRule(AtomicReference<GrpcTransport> proxy, boolean skipOnUnavailable) {
        this.proxy = proxy;
        this.skipOnUnavailable = skipOnUnavailable;
    }

    public GrpcTransportRule() {
        this(new AtomicReference<>(), true);
    }

    public GrpcTransportRule failIfUnavailable() {
        return new GrpcTransportRule(proxy, false);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        YdbHelperFactory factory = YdbHelperFactory.getInstance();

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                String path = description.getDisplayName();
                if (description.getMethodName() != null) {
                    path += "/" + description.getMethodName();
                }

                if (!factory.isEnabled()) {
                    if (!skipOnUnavailable) {
                        throw new AssertionError("Ydb helper is not available " + path);
                    }
                    logger.info("Test {} skipped because ydb helper is not available", path);
                    Assume.assumeFalse("YDB Helper is not available", true);
                    return;
                }

                logger.debug("create ydb helper for test {}", path);
                try (YdbHelper helper = factory.createHelper()) {
                    try (GrpcTransport transport = helper.createTransport()) {
                        proxy.set(transport);
                        base.evaluate();
                        proxy.set(null);
                    }
                }
            }
        };
    }

    @Override
    protected GrpcTransport origin() {
        return proxy.get();
    }
}
