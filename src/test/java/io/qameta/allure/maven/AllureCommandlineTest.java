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

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.qatools.matchers.nio.PathMatchers.exists;

/**
 * Tests for the direct download path in {@link AllureCommandline}.
 */
public class AllureCommandlineTest {

    @Test
    public void shouldApplyMavenWagonSslOverridesForHttpsDownloads() throws Exception {
        final Path installDirectory = Files.createTempDirectory("allure-install");
        try {
            final String version = "2.30.0";
            final RecordingHttpsConnection connection = new RecordingHttpsConnection(
                    new URL("https://example.test/allure.zip"), createAllureArchive(version));
            final URL url = new URL(null, "https://example.test/allure.zip",
                    new RecordingUrlStreamHandler(connection));

            final Properties downloadProperties = new Properties();
            downloadProperties.setProperty("maven.wagon.http.ssl.insecure", "true");
            downloadProperties.setProperty("maven.wagon.http.ssl.allowall", "true");

            final AllureCommandline commandline = new AllureCommandline(installDirectory, version);
            commandline.download(url, null, downloadProperties);

            assertThat(connection.getCustomSocketFactory(), notNullValue());
            assertThat(connection.getCustomHostnameVerifier(), notNullValue());
            assertThat(connection.getCustomHostnameVerifier().verify("example.test", null),
                    is(true));
            assertThat(
                    installDirectory.resolve("allure-" + version).resolve("bin").resolve("allure"),
                    exists());
        } finally {
            FileUtils.deleteQuietly(installDirectory.toFile());
        }
    }

    @Test(expected = CertificateExpiredException.class)
    public void shouldKeepCertificateValidityChecksWhenIgnoreDatesDisabled()
            throws CertificateException {
        new AllureDownloadUtils.RelaxedX509TrustManager(false)
                .checkServerTrusted(new X509Certificate[] {new TestCertificate(true)}, "RSA");
    }

    @Test
    public void shouldIgnoreCertificateValidityChecksWhenConfigured() throws CertificateException {
        new AllureDownloadUtils.RelaxedX509TrustManager(true)
                .checkServerTrusted(new X509Certificate[] {new TestCertificate(true)}, "RSA");
    }

    @Test
    public void shouldPassUnixPathsWithoutLiteralQuotesWhenGeneratingReport() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure-commandline");
        try {
            final String version = "2.30.0";
            final Path installDirectory = testDirectory.resolve("install");
            final Path resultsDirectory = testDirectory.resolve("results with space");
            final Path reportDirectory = testDirectory.resolve("report with space");
            final Path capturedArgs = testDirectory.resolve("args.txt");
            Files.createDirectories(resultsDirectory);

            createUnixAllureExecutable(installDirectory, version, "#!/bin/sh",
                    "printf '%s\\n' \"$@\" > '" + capturedArgs + "'", "exit 0");

            final AllureCommandline commandline = new AllureCommandline(installDirectory, version);
            commandline.generateReport(Collections.singletonList(resultsDirectory), reportDirectory,
                    false);

            assertThat(Files.readAllLines(capturedArgs, StandardCharsets.UTF_8),
                    is(Arrays.asList("generate", "--clean",
                            resultsDirectory.toAbsolutePath().toString(), "-o",
                            reportDirectory.toAbsolutePath().toString())));
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    public void shouldPassWindowsPathsWithoutLosingSpacesWhenServingReport() throws Exception {
        assumeTrue(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure commandline");
        try {
            final String version = "2.30.0";
            final Path installDirectory = testDirectory.resolve("install with space");
            final Path resultsDirectory = testDirectory.resolve("results with space");
            final Path reportDirectory = testDirectory.resolve("report with space");
            final Path capturedArgs = testDirectory.resolve("captured args.txt");
            Files.createDirectories(resultsDirectory);

            final RecordingHttpsConnection connection = new RecordingHttpsConnection(
                    new URL("https://example.test/allure.zip"), createAllureArchive(version,
                            "echo allure", createWindowsArgumentCapturingLauncher(capturedArgs)));
            final URL url = new URL(null, "https://example.test/allure.zip",
                    new RecordingUrlStreamHandler(connection));

            final AllureCommandline commandline = new AllureCommandline(installDirectory, version);
            commandline.download(url, null, new Properties());
            commandline.serve(Collections.singletonList(resultsDirectory), reportDirectory, null,
                    0);

            assertThat(Files.readAllLines(capturedArgs, StandardCharsets.UTF_8),
                    is(Arrays.asList("serve", resultsDirectory.toAbsolutePath().toString())));
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    private static void createUnixAllureExecutable(final Path installDirectory,
            final String version, final String... lines) throws IOException {
        final Path executable =
                installDirectory.resolve("allure-" + version).resolve("bin").resolve("allure");
        Files.createDirectories(executable.getParent());
        Files.write(executable, Arrays.asList(lines), StandardCharsets.UTF_8);
        executable.toFile().setExecutable(true);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static byte[] createAllureArchive(final String version) throws IOException {
        return createAllureArchive(version, "echo allure", "@echo off\r\necho allure\r\n");
    }

    private static byte[] createAllureArchive(final String version, final String unixLauncher,
            final String windowsLauncher) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            final Map<String, String> entries = new LinkedHashMap<>();
            entries.put("allure-" + version + "/bin/allure", unixLauncher);
            entries.put("allure-" + version + "/bin/allure.bat", windowsLauncher);
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                addZipEntry(zip, entry.getKey(), entry.getValue());
            }
        }
        return output.toByteArray();
    }

    private static void addZipEntry(final ZipOutputStream zip, final String name,
            final String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String createWindowsArgumentCapturingLauncher(final Path capturedArgs) {
        final String captureDirectory = capturedArgs.getParent().toAbsolutePath().toString();
        final String captureFile = capturedArgs.toAbsolutePath().toString();
        return String.join("\r\n", "@echo off", "setlocal EnableExtensions DisableDelayedExpansion",
                "if not exist \"" + captureDirectory + "\" mkdir \"" + captureDirectory + "\"",
                "type nul > \"" + captureFile + "\"", ":loop", "if \"%~1\"==\"\" goto done",
                ">> \"" + captureFile + "\" echo(%~1", "shift", "goto loop", ":done", "exit /b 0",
                "");
    }

    /**
     * URL stream handler that always returns the preconfigured test connection.
     */
    private static final class RecordingUrlStreamHandler extends URLStreamHandler {

        private final RecordingHttpsConnection connection;

        private RecordingUrlStreamHandler(final RecordingHttpsConnection connection) {
            this.connection = connection;
        }

        @Override
        protected URLConnection openConnection(final URL url) {
            return connection;
        }

        @Override
        protected URLConnection openConnection(final URL url, final Proxy proxy) {
            connection.setProxyUsed(proxy != null);
            return connection;
        }
    }

    /**
     * Minimal HTTPS connection used to capture SSL customizations applied by the downloader.
     */
    private static final class RecordingHttpsConnection extends HttpsURLConnection {

        private final byte[] response;

        private SSLSocketFactory customSocketFactory;

        private HostnameVerifier customHostnameVerifier;

        private final AtomicBoolean proxyUsed = new AtomicBoolean();

        private RecordingHttpsConnection(final URL url, final byte[] response) {
            super(url);
            this.response = response;
        }

        @Override
        public void disconnect() {
            // do nothing
        }

        @Override
        public boolean usingProxy() {
            return proxyUsed.get();
        }

        @Override
        public void connect() {
            // do nothing
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(response);
        }

        @Override
        public void setSSLSocketFactory(final SSLSocketFactory sf) {
            this.customSocketFactory = sf;
        }

        @Override
        public void setHostnameVerifier(final HostnameVerifier hostnameVerifier) {
            this.customHostnameVerifier = hostnameVerifier;
        }

        @Override
        public String getCipherSuite() {
            return null;
        }

        @Override
        public java.security.cert.Certificate[] getLocalCertificates() {
            return new java.security.cert.Certificate[0];
        }

        @Override
        public java.security.cert.Certificate[] getServerCertificates() {
            return new java.security.cert.Certificate[0];
        }

        @Override
        public Principal getPeerPrincipal() {
            return null;
        }

        @Override
        public Principal getLocalPrincipal() {
            return null;
        }

        private SSLSocketFactory getCustomSocketFactory() {
            return customSocketFactory;
        }

        private HostnameVerifier getCustomHostnameVerifier() {
            return customHostnameVerifier;
        }

        private void setProxyUsed(final boolean used) {
            proxyUsed.set(used);
        }
    }

    /**
     * Stub certificate that can simulate expired validity checks for trust manager tests.
     */
    private static final class TestCertificate extends X509Certificate {

        private final boolean expired;

        private TestCertificate(final boolean expired) {
            this.expired = expired;
        }

        @Override
        public void checkValidity() throws CertificateExpiredException {
            if (expired) {
                throw new CertificateExpiredException("expired");
            }
        }

        @Override
        public void checkValidity(final Date date)
                throws CertificateExpiredException, CertificateNotYetValidException {
            checkValidity();
        }

        @Override
        public int getVersion() {
            return 3;
        }

        @Override
        public BigInteger getSerialNumber() {
            return BigInteger.ONE;
        }

        @Override
        public Principal getIssuerDN() {
            return () -> "CN=issuer";
        }

        @Override
        public Principal getSubjectDN() {
            return () -> "CN=subject";
        }

        @Override
        public Date getNotBefore() {
            return new Date(0L);
        }

        @Override
        public Date getNotAfter() {
            return new Date(0L);
        }

        @Override
        public byte[] getTBSCertificate() {
            return new byte[0];
        }

        @Override
        public byte[] getSignature() {
            return new byte[0];
        }

        @Override
        public String getSigAlgName() {
            return "none";
        }

        @Override
        public String getSigAlgOID() {
            return "0.0";
        }

        @Override
        public byte[] getSigAlgParams() {
            return new byte[0];
        }

        @Override
        public boolean[] getIssuerUniqueID() {
            return null;
        }

        @Override
        public boolean[] getSubjectUniqueID() {
            return null;
        }

        @Override
        public boolean[] getKeyUsage() {
            return null;
        }

        @Override
        public int getBasicConstraints() {
            return -1;
        }

        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            return new byte[0];
        }

        @Override
        public void verify(final PublicKey key) {
            // do nothing
        }

        @Override
        public void verify(final PublicKey key, final String sigProvider) {
            // do nothing
        }

        @Override
        public String toString() {
            return "TestCertificate";
        }

        @Override
        public PublicKey getPublicKey() {
            return null;
        }

        @Override
        public boolean hasUnsupportedCriticalExtension() {
            return false;
        }

        @Override
        public Set<String> getCriticalExtensionOIDs() {
            return Collections.emptySet();
        }

        @Override
        public Set<String> getNonCriticalExtensionOIDs() {
            return Collections.emptySet();
        }

        @Override
        public byte[] getExtensionValue(final String oid) {
            return new byte[0];
        }
    }
}
