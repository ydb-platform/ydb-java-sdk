package tech.ydb.core.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.X509TrustManager;

final class MultiX509TrustManager implements X509TrustManager {
    final List<X509TrustManager> trustManagers;

    MultiX509TrustManager(final List<X509TrustManager> trustManagers) {
        this.trustManagers = trustManagers;
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] x509Certificates, final String authType)
            throws CertificateException {
        for (X509TrustManager trustManager : trustManagers) {
            try {
                trustManager.checkClientTrusted(x509Certificates, authType);
                return;
            } catch (CertificateException ignored) {
            }
        }
        throw new CertificateException("No trust manager trusts this certificates");
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] x509Certificates, final String authType)
            throws CertificateException {
        for (X509TrustManager trustManager : trustManagers) {
            try {
                trustManager.checkServerTrusted(x509Certificates, authType);
                return;
            } catch (CertificateException ignored) {
            }
        }
        throw new CertificateException("No trust manager trusts this certificates");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        List<X509Certificate> acceptedIssuers = new ArrayList<>();
        for (X509TrustManager trustManager : trustManagers) {
            acceptedIssuers.addAll(Arrays.asList(trustManager.getAcceptedIssuers()));
        }
        return acceptedIssuers.toArray(new X509Certificate[0]);
    }
}
