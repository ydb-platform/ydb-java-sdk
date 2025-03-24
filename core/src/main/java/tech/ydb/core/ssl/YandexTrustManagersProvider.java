package tech.ydb.core.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

final class YandexTrustManagersProvider {
    private static final String YANDEX_CA_STORE = "certificates/YandexAllCAs.pkcs";
    private static final String STORE_PASSWORD = "yandex";

    private final TrustManager[] trustManagers;

    private YandexTrustManagersProvider() {
        try {
            List<TrustManager> customTrustManagers = getCustomTrustManagers();
            List<TrustManager> defaultTrustManagers = getDefaultTrustManagers();

            List<X509TrustManager> x509TrustManagers = Stream
                    .concat(customTrustManagers.stream(), defaultTrustManagers.stream())
                    .filter(X509TrustManager.class::isInstance)
                    .map(X509TrustManager.class::cast)
                    .collect(Collectors.toList());
            List<TrustManager> allTrustManagers = Stream
                    .concat(customTrustManagers.stream(), defaultTrustManagers.stream())
                    .filter(x -> !(x instanceof X509TrustManager))
                    .collect(Collectors.toCollection(ArrayList::new));
            X509TrustManager composite = new MultiX509TrustManager(x509TrustManagers);
            allTrustManagers.add(composite);
            trustManagers = allTrustManagers.toArray(new TrustManager[0]);
        } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException e) {
            String msg = "Can't init yandex root CA setting";
            throw new RuntimeException(msg, e);
        }
    }

    private List<TrustManager> getDefaultTrustManagers() throws NoSuchAlgorithmException, KeyStoreException {
        return getTrustManagersFromKeyStore(null);
    }

    private List<TrustManager> getCustomTrustManagers()
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = YandexTrustManagersProvider.class.getClassLoader().getResourceAsStream(YANDEX_CA_STORE)) {
            keyStore.load(is, STORE_PASSWORD.toCharArray());
        }
        return getTrustManagersFromKeyStore(keyStore);
    }

    private List<TrustManager> getTrustManagersFromKeyStore(KeyStore keyStore)
            throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        return Arrays.asList(trustManagerFactory.getTrustManagers());
    }

    private static final class LazyHolder {
        private static final YandexTrustManagersProvider INSTANCE = new YandexTrustManagersProvider();
    }

    public static YandexTrustManagersProvider getInstance() {
        return LazyHolder.INSTANCE;
    }

    public TrustManager[] getTrustManagers() {
        return trustManagers.clone();
    }

}
