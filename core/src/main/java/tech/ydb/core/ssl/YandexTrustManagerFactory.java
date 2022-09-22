package tech.ydb.core.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

import io.netty.util.internal.PlatformDependent;

public final class YandexTrustManagerFactory extends TrustManagerFactory {

    @SuppressWarnings("deprecation")
    private static final Provider PROVIDER = new Provider("", 0.0D, "") {
        private static final long serialVersionUID = -2680540247105807895L;
    };

    private static final ThreadLocal<YandexTrustManagerFactorySpi> CURRENT_SPI =
            new ThreadLocal<YandexTrustManagerFactorySpi>() {
        @Override
        protected YandexTrustManagerFactorySpi initialValue() {
            return new YandexTrustManagerFactorySpi();
        }
    };

    public YandexTrustManagerFactory(String name) {
        super(CURRENT_SPI.get(), PROVIDER, name);
        TrustManager[] trustManagers = YandexTrustManagersProvider.getInstance().getTrustManagers();
        CURRENT_SPI.get().init(trustManagers);
        CURRENT_SPI.remove();
    }

    private static final class YandexTrustManagerFactorySpi extends TrustManagerFactorySpi {
        private volatile TrustManager[] trustManagers;

        YandexTrustManagerFactorySpi() {
        }

        void init(TrustManager[] managers) {
            if (PlatformDependent.javaVersion() >= 7) {
                for (int i = 0; i < managers.length; ++i) {
                    TrustManager tm = managers[i];
                    if (tm instanceof X509TrustManager && !(tm instanceof X509ExtendedTrustManager)) {
                        managers[i] = new X509TrustManagerWrapper((X509TrustManager) tm);
                    }
                }
            }

            this.trustManagers = managers;
        }

        @Override
        protected void engineInit(KeyStore keyStore) throws KeyStoreException {
        }

        @Override
        protected void engineInit(ManagerFactoryParameters managerFactoryParameters)
                throws InvalidAlgorithmParameterException {
        }

        @Override
        protected TrustManager[] engineGetTrustManagers() {
            return (TrustManager[]) trustManagers.clone();
        }
    }

}
