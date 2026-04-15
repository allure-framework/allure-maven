/*
 *  Copyright 2016-2024 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.maven;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Proxy;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Utility methods for downloading the Allure command line archive while honoring Maven proxy and
 * Wagon SSL-related system properties.
 */
@SuppressWarnings("ClassDataAbstractionCoupling")
final class AllureDownloadUtils {

    private static final String WAGON_HTTP_SSL_INSECURE = "maven.wagon.http.ssl.insecure";

    private static final String WAGON_HTTP_SSL_ALLOW_ALL = "maven.wagon.http.ssl.allowall";

    private static final String WAGON_HTTP_SSL_IGNORE_VALIDITY_DATES =
            "maven.wagon.http.ssl.ignore.validity.dates";

    private AllureDownloadUtils() {
        throw new IllegalStateException("Do not instance");
    }

    static void copy(final URL url, final Path destination, final Proxy mavenProxy,
            final Properties downloadProperties) throws IOException {
        final AuthenticatorState authenticatorState = AuthenticatorState.capture();
        boolean proxyAuthenticatorConfigured = false;

        try {
            java.net.Proxy proxy = null;
            if (mavenProxy != null) {
                final InetSocketAddress proxyAddress =
                        new InetSocketAddress(mavenProxy.getHost(), mavenProxy.getPort());
                proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, proxyAddress);
                if (StringUtils.isNotBlank(mavenProxy.getUsername())
                        && StringUtils.isNotBlank(mavenProxy.getPassword())) {
                    final String proxyUser = mavenProxy.getUsername();
                    final String proxyPassword = mavenProxy.getPassword();
                    Authenticator.setDefault(new Authenticator() {
                        @Override
                        public PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(proxyUser,
                                    proxyPassword.toCharArray());
                        }
                    });
                    proxyAuthenticatorConfigured = true;
                }
            }

            final WagonSslProperties sslProperties = WagonSslProperties.from(downloadProperties);
            final URLConnection connection = openConnection(url, proxy, sslProperties);
            try (InputStream inputStream = connection.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (proxyAuthenticatorConfigured) {
                authenticatorState.restore();
            }
        }
    }

    static Properties getDownloadProperties(final MavenSession session) {
        final Properties properties = new Properties();
        properties.putAll(System.getProperties());
        if (session != null) {
            properties.putAll(session.getSystemProperties());
            properties.putAll(session.getUserProperties());
        }
        return properties;
    }

    @SuppressWarnings("WhitespaceAfter")
    private static URLConnection openConnection(final URL url, final java.net.Proxy proxy,
            final WagonSslProperties sslProperties) throws IOException {
        final URLConnection connection =
                proxy == null ? url.openConnection() : url.openConnection(proxy);
        if (!(connection instanceof HttpsURLConnection) || !sslProperties.isInsecure()) {
            return connection;
        }

        try {
            final HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null,
                    new TrustManager[] {
                            new RelaxedX509TrustManager(sslProperties.isIgnoreValidityDates()),},
                    new SecureRandom());
            httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
            if (sslProperties.isAllowAll()) {
                httpsConnection.setHostnameVerifier((hostname, session) -> true);
            }
            return httpsConnection;
        } catch (GeneralSecurityException e) {
            throw new IOException("Unable to configure SSL for Allure download.", e);
        }
    }

    /**
     * Parsed view of the Wagon SSL properties relevant to direct HTTPS downloads.
     */
    static final class WagonSslProperties {

        private final boolean insecure;

        private final boolean allowAll;

        private final boolean ignoreValidityDates;

        private WagonSslProperties(final boolean insecure, final boolean allowAll,
                final boolean ignoreValidityDates) {
            this.insecure = insecure;
            this.allowAll = allowAll;
            this.ignoreValidityDates = ignoreValidityDates;
        }

        static WagonSslProperties from(final Properties properties) {
            return new WagonSslProperties(getBooleanProperty(properties, WAGON_HTTP_SSL_INSECURE),
                    getBooleanProperty(properties, WAGON_HTTP_SSL_ALLOW_ALL),
                    getBooleanProperty(properties, WAGON_HTTP_SSL_IGNORE_VALIDITY_DATES));
        }

        private static boolean getBooleanProperty(final Properties properties,
                final String propertyName) {
            return Boolean.parseBoolean(properties.getProperty(propertyName));
        }

        boolean isInsecure() {
            return insecure;
        }

        boolean isAllowAll() {
            return insecure && allowAll;
        }

        boolean isIgnoreValidityDates() {
            return insecure && ignoreValidityDates;
        }
    }

    /**
     * Trust manager that optionally skips certificate validity date checks when Maven's Wagon
     * compatibility flags request relaxed SSL handling.
     */
    static final class RelaxedX509TrustManager implements X509TrustManager {

        private final boolean ignoreValidityDates;

        RelaxedX509TrustManager(final boolean ignoreValidityDates) {
            this.ignoreValidityDates = ignoreValidityDates;
        }

        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException {
            checkValidity(chain);
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException {
            checkValidity(chain);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        private void checkValidity(final X509Certificate... chain) throws CertificateException {
            if (ignoreValidityDates || chain == null) {
                return;
            }
            for (X509Certificate certificate : chain) {
                certificate.checkValidity();
            }
        }
    }

    /**
     * Captures the current JVM authenticator so download flows can restore it after temporary proxy
     * credentials are installed.
     */
    static final class AuthenticatorState {

        private final Authenticator authenticator;

        private AuthenticatorState(final Authenticator authenticator) {
            this.authenticator = authenticator;
        }

        static AuthenticatorState capture() {
            return new AuthenticatorState(Authenticator.getDefault());
        }

        void restore() {
            Authenticator.setDefault(authenticator);
        }
    }
}
