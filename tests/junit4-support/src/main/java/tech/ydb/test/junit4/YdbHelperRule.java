package tech.ydb.test.junit4;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.test.integration.YdbHelper;
import tech.ydb.test.integration.YdbHelperFactory;
import tech.ydb.test.integration.utils.ProxyYdbHelper;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class YdbHelperRule extends ProxyYdbHelper implements TestRule {
    private static final Logger logger = LoggerFactory.getLogger(YdbHelperRule.class);

    private final AtomicReference<YdbHelper> proxy = new AtomicReference<>();

    @Override
    public Statement apply(Statement base, Description description) {
        YdbHelperFactory factory = YdbHelperFactory.getInstance();

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (!factory.isEnabled()) {
                    logger.info("Test {} skipped because ydb helper is not available", description.getDisplayName());
                    Assume.assumeFalse("YDB Helper is not available", true);
                    return;
                }

                String path = description.getDisplayName();
                logger.debug("create ydb helper for test {}", path);
                try (YdbHelper helper = factory.createHelper()) {
                    proxy.set(helper);
                    base.evaluate();
                    proxy.set(null);
                }
            }
        };
    }

    @Override
    protected YdbHelper origin() {
        return proxy.get();
    }

    @Override
    public String getStdErr() {
        return proxy.get().getStdErr();
    }
}
