package tech.ydb.core.ssl;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;


public final class X509TrustManagerWrapper extends X509ExtendedTrustManager {
    private final X509TrustManager delegate;

    X509TrustManagerWrapper(X509TrustManager delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String s) throws CertificateException {
        this.delegate.checkClientTrusted(chain, s);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String s, Socket socket) throws CertificateException {
        this.delegate.checkClientTrusted(chain, s);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String s, SSLEngine sslEngine) throws CertificateException {
        this.delegate.checkClientTrusted(chain, s);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String s) throws CertificateException {
        this.delegate.checkServerTrusted(chain, s);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String s, Socket socket) throws CertificateException {
        this.delegate.checkServerTrusted(chain, s);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String s, SSLEngine sslEngine) throws CertificateException {
        this.delegate.checkServerTrusted(chain, s);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return this.delegate.getAcceptedIssuers();
    }
}
