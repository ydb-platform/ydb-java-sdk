package tech.ydb.core.impl.pool;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class ChannelFactoryLoader {
    private static final Logger logger = LoggerFactory.getLogger(ChannelFactoryLoader.class);

    private ChannelFactoryLoader() { }

    public static ManagedChannelFactory.Builder load() {
        return FactoryLoader.factory;
    }

    private static class FactoryLoader {
        private static final String SHADED_DEPS = "io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder";
        private static final String NETTY_DEPS = "io.grpc.netty.NettyChannelBuilder";

        private static ManagedChannelFactory.Builder factory;

        static {
            boolean ok = tryLoad(SHADED_DEPS, ShadedNettyChannelFactory.build())
                    || tryLoad(NETTY_DEPS, NettyChannelFactory.build());
            if (!ok) {
                throw new IllegalStateException("Cannot load any ManagedChannelFactory!! "
                        + "Classpath must contain grpc-netty or grpc-netty-shaded");
            }
        }

        private static boolean tryLoad(String name, ManagedChannelFactory.Builder f) {
            try {
                Class.forName(name);
                logger.info("class {} is found, use {}", name, f);
                factory = f;
                return true;
            } catch (ClassNotFoundException ex) {
                logger.info("class {} is not found", name);
                return false;
            }
        }
    }
}
