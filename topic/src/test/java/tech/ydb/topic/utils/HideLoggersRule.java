package tech.ydb.topic.utils;


import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class HideLoggersRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        HideLoggers annotation = description.getAnnotation(HideLoggers.class);
        if (annotation == null) {
            return base;
        }

        Class<?>[] hiddenLoggers = annotation.value();
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Map<ExtendedLogger, Level> before = new HashMap<>();
                for (Class<?> clazz: hiddenLoggers) {
                    ExtendedLogger logger = LogManager.getContext(true).getLogger(clazz);
                    before.put(logger, logger.getLevel());
                    // hide logger
                    Configurator.setLevel(logger, Level.OFF);
                }

                try {
                    base.evaluate();
                } finally {
                    // recover all loggers
                    before.forEach((logger, level) -> Configurator.setLevel(logger, level));
                }
            }
        };
    }
}
