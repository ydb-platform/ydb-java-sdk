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

/**
 *
 * @author Aleksandr Gorshenin
 */
public class YdbHelperRule implements TestRule, YdbHelper {
    private static final Logger logger = LoggerFactory.getLogger(YdbHelperRule.class);

    private final AtomicReference<YdbHelper> proxy = new AtomicReference<>();

    @Override
    public Statement apply(Statement base, Description description) {
        YdbHelperFactory factory = YdbHelperFactory.getInstance();

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                String path = description.getDisplayName();
                logger.debug("create ydb helper for test {}", path);

                YdbHelper helper = factory.createHelper();

                if (helper == null) {
                    logger.info("Test {} skipped because ydb helper is not available", description.getDisplayName());
                    Assume.assumeFalse("YDB Helper is not available", true);
                    return;
                }

                try {
                    proxy.set(helper);
                    base.evaluate();
                    proxy.set(null);
                } finally {
                    helper.close();
                }
            }
        };
    }

    @Override
    public GrpcTransport createTransport(String path) {
        return proxy.get().createTransport(path);
    }

    @Override
    public String endpoint() {
        return proxy.get().endpoint();
    }

    @Override
    public String database() {
        return proxy.get().database();
    }

    @Override
    public boolean useTls() {
        return proxy.get().useTls();
    }

    @Override
    public byte[] pemCert() {
        return proxy.get().pemCert();
    }

    @Override
    public String authToken() {
        return proxy.get().authToken();
    }

    @Override
    public void close() {
        // Nothing
    }
}
