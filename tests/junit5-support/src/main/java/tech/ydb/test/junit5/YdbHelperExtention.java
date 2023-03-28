package tech.ydb.test.junit5;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.test.integration.YdbHelper;
import tech.ydb.test.integration.YdbHelperFactory;
import tech.ydb.test.integration.utils.ProxyYdbHelper;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class YdbHelperExtention extends ProxyYdbHelper implements ExecutionCondition,
        BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {
    private static final Logger logger = LoggerFactory.getLogger(GrpTransportExtension.class);

    private final Holder holder = new Holder();

    @Override
    protected YdbHelper origin() {
        return holder.helper();
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (!context.getTestInstance().isPresent()) {
            return ConditionEvaluationResult.enabled("OK");
        }

        if (!YdbHelperFactory.getInstance().isEnabled()) {
            return ConditionEvaluationResult.disabled("Ydb helper is disabled " + context.getDisplayName());
        }
        return ConditionEvaluationResult.enabled("OK");
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
            }
        }

        public synchronized void after(ExtensionContext ec) {
            if (context != ec) {
                return;
            }

            if (helper != null) {
                helper.close();
                helper = null;
            }

            context = null;
        }

        public synchronized YdbHelper helper() {
            return helper;
        }
    }
}
