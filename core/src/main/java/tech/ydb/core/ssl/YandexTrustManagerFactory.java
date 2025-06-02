package tech.ydb.core.ssl;

import java.security.KeyStore;
import java.security.Provider;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

public final class YandexTrustManagerFactory extends TrustManagerFactory {

    private static final Provider PROVIDER = new Provider("", 0.0D, "") {
        private static final long serialVersionUID = -2680540247105807895L;
    };

    public YandexTrustManagerFactory(String name) {
        super(new YandexTrustManagerFactorySpi(), PROVIDER, name);
    }

    private static final class YandexTrustManagerFactorySpi extends TrustManagerFactorySpi {
        private static final TrustManager[] TRUST_MANAGERS = initTrustManagers();

        YandexTrustManagerFactorySpi() { }

        @Override
        protected void engineInit(KeyStore keyStore) {
        }

        @Override
        protected void engineInit(ManagerFactoryParameters managerFactoryParameters) {
        }

        @Override
        protected TrustManager[] engineGetTrustManagers() {
            return TRUST_MANAGERS.clone();
        }
    }

    private static TrustManager[] initTrustManagers() {
        TrustManager[] managers = YandexTrustManagersProvider.getInstance().getTrustManagers();

        for (int i = 0; i < managers.length; ++i) {
            TrustManager tm = managers[i];
            if (tm instanceof X509TrustManager && !(tm instanceof X509ExtendedTrustManager)) {
                managers[i] = new X509TrustManagerWrapper((X509TrustManager) tm);
            }
        }

        return managers;
    }

}
