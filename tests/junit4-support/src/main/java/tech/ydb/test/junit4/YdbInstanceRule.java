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
public class YdbInstanceRule implements TestRule {
    private static final Logger logger = LoggerFactory.getLogger(YdbInstanceRule.class);

    private final AtomicReference<GrpcTransport> weakTransport = new AtomicReference<>();

    @Override
    public Statement apply(Statement base, Description description) {
        YdbHelperFactory factory = YdbHelperFactory.getInstance();

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                String path = description.getDisplayName();
                logger.debug("create ydb helper for test {}", path);

                YdbHelper helper = factory.createHelper(path);

                if (helper == null) {
                    logger.info("Test {} skipped because ydb helper is not available", description.getDisplayName());
                    Assume.assumeFalse("YDB Helper is not available", true);
                    return;
                }

                try (GrpcTransport transport = helper.createTransport()) {
                    weakTransport.set(transport);
                    base.evaluate();
                    weakTransport.set(null);
                }
            }
        };
    }

    public GrpcTransport transport() {
        return weakTransport.get();
    }
}
