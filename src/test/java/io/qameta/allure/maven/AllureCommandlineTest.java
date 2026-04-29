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
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static io.qameta.allure.Allure.addAttachment;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("unit")
@Tag("allure2")
@Tag("commandline")
/**
 * Tests for the direct download path in {@link AllureCommandline}.
 */
class AllureCommandlineTest {

    @Test
    void shouldApplyMavenWagonSslOverridesForHttpsDownloads() throws Exception {
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
            final Path launcher =
                    installDirectory.resolve("allure-" + version).resolve("bin").resolve("allure");

            step("Prepare fake HTTPS archive and Maven wagon SSL overrides", () -> {
                addAttachment("HTTPS download inputs",
                        String.join(System.lineSeparator(), "url=" + url,
                                "installDirectory=" + installDirectory,
                                "maven.wagon.http.ssl.insecure=true",
                                "maven.wagon.http.ssl.allowall=true"));
            });

            final AllureCommandline commandline = new AllureCommandline(installDirectory, version);
            step("Download and unpack Allure commandline",
                    () -> commandline.download(url, null, downloadProperties));

            step("Verify SSL overrides and extracted launcher", () -> {
                addAttachment("Captured SSL overrides",
                        String.join(System.lineSeparator(),
                                "socketFactory=" + connection.getCustomSocketFactory(),
                                "hostnameVerifier=" + connection.getCustomHostnameVerifier(),
                                "launcher=" + launcher));
                assertThat(connection.getCustomSocketFactory()).isNotNull();
                assertThat(connection.getCustomHostnameVerifier()).isNotNull();
                assertThat(connection.getCustomHostnameVerifier().verify("example.test", null))
                        .isTrue();
                assertThat(launcher).exists();
            });
        } finally {
            FileUtils.deleteQuietly(installDirectory.toFile());
        }
    }

    @Test
    void shouldKeepCertificateValidityChecksWhenIgnoreDatesDisabled() throws CertificateException {
        final AllureDownloadUtils.RelaxedX509TrustManager trustManager =
                step("Create trust manager with certificate date checks enabled",
                        () -> new AllureDownloadUtils.RelaxedX509TrustManager(false));

        step("Reject expired certificate when date checks stay enabled", () -> {
            final CertificateExpiredException error = assertThrows(
                    CertificateExpiredException.class, () -> trustManager.checkServerTrusted(
                            new X509Certificate[] {new TestCertificate(true)}, "RSA"));
            addAttachment("Strict trust manager error", String.valueOf(error.getMessage()));
        });
    }

    @Test
    void shouldIgnoreCertificateValidityChecksWhenConfigured() throws CertificateException {
        final AllureDownloadUtils.RelaxedX509TrustManager trustManager =
                step("Create trust manager with certificate date checks disabled",
                        () -> new AllureDownloadUtils.RelaxedX509TrustManager(true));

        step("Accept expired certificate when date checks are disabled", () -> {
            trustManager.checkServerTrusted(new X509Certificate[] {new TestCertificate(true)},
                    "RSA");
            addAttachment("Relaxed trust manager result", "acceptedExpiredCertificate=true");
        });
    }

    @Test
    void shouldPassUnixPathsWithoutLiteralQuotesWhenGeneratingReport() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure-commandline");
        try {
            final String version = "2.30.0";
            final Path installDirectory = testDirectory.resolve("install");
            final Path resultsDirectory = testDirectory.resolve("results with space");
            final Path reportDirectory = testDirectory.resolve("report with space");
            final Path capturedArgs = testDirectory.resolve("args.txt");
            step("Prepare fake Unix runtime and results directory", () -> {
                Files.createDirectories(resultsDirectory);
                createUnixAllureExecutable(installDirectory, version, "#!/bin/sh",
                        "printf '%s\\n' \"$@\" > '" + capturedArgs + "'", "exit 0");
                addAttachment("Unix generate inputs", "resultsDirectory=" + resultsDirectory
                        + System.lineSeparator() + "reportDirectory=" + reportDirectory);
            });

            final AllureCommandline commandline = new AllureCommandline(installDirectory, version);
            step("Generate report",
                    () -> commandline.generateReport(Collections.singletonList(resultsDirectory),
                            reportDirectory, false));

            step("Verify generated command arguments", () -> {
                final List<String> args = Files.readAllLines(capturedArgs, StandardCharsets.UTF_8);
                addAttachment("Generate command arguments",
                        String.join(System.lineSeparator(), args));
                assertThat(args).isEqualTo(Arrays.asList("generate", "--clean",
                        resultsDirectory.toAbsolutePath().toString(), "-o",
                        reportDirectory.toAbsolutePath().toString()));
            });
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    void shouldPassVerboseFlagAndLogCommandWhenDebugGeneratingReport() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure-commandline");
        try {
            final String version = "2.30.0";
            final Path installDirectory = testDirectory.resolve("install");
            final Path resultsDirectory = testDirectory.resolve("results with space");
            final Path reportDirectory = testDirectory.resolve("report with space");
            final Path capturedArgs = testDirectory.resolve("args.txt");
            final Path executable =
                    installDirectory.resolve("allure-" + version).resolve("bin").resolve("allure");
            final RecordingLog log = new RecordingLog(true);
            step("Prepare fake Unix runtime and debug results directory", () -> {
                Files.createDirectories(resultsDirectory);
                createUnixAllureExecutable(installDirectory, version, "#!/bin/sh",
                        "printf '%s\\n' \"$@\" > '" + capturedArgs + "'", "exit 0");
                addAttachment("Debug generate inputs", "resultsDirectory=" + resultsDirectory
                        + System.lineSeparator() + "reportDirectory=" + reportDirectory);
            });

            final AllureCommandline commandline =
                    new AllureCommandline(installDirectory, version, 10, log);
            step("Generate report in debug mode",
                    () -> commandline.generateReport(Collections.singletonList(resultsDirectory),
                            reportDirectory, false));

            step("Verify debug command arguments and logged command", () -> {
                final List<String> args = Files.readAllLines(capturedArgs, StandardCharsets.UTF_8);
                addAttachment("Debug generate command arguments",
                        String.join(System.lineSeparator(), args));
                addAttachment("Debug generate log messages",
                        String.join(System.lineSeparator(), log.debugMessages));
                assertThat(args).isEqualTo(Arrays.asList("--verbose", "generate", "--clean",
                        resultsDirectory.toAbsolutePath().toString(), "-o",
                        reportDirectory.toAbsolutePath().toString()));
                assertThat(log.debugMessages)
                        .isEqualTo(Collections.singletonList("Executing Allure command: ["
                                + executable.toAbsolutePath() + ", --verbose, generate, --clean, "
                                + resultsDirectory.toAbsolutePath() + ", -o, "
                                + reportDirectory.toAbsolutePath() + "]"));
            });
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    void shouldPassWindowsPathsWithoutLosingSpacesWhenServingReport() throws Exception {
        assumeTrue(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure commandline");
        try {
            final String version = "2.30.0";
            final Path installDirectory = testDirectory.resolve("install with space");
            final Path resultsDirectory = testDirectory.resolve("results with space");
            final Path capturedArgs = testDirectory.resolve("captured args.txt");
            Files.createDirectories(resultsDirectory);

            final RecordingHttpsConnection connection = new RecordingHttpsConnection(
                    new URL("https://example.test/allure.zip"), createAllureArchive(version,
                            "echo allure", createWindowsArgumentCapturingLauncher(capturedArgs)));
            final URL url = new URL(null, "https://example.test/allure.zip",
                    new RecordingUrlStreamHandler(connection));

            final AllureCommandline commandline = new AllureCommandline(installDirectory, version);
            step("Prepare Windows runtime and results directory", () -> {
                addAttachment("Windows serve inputs",
                        String.join(System.lineSeparator(), "installDirectory=" + installDirectory,
                                "resultsDirectory=" + resultsDirectory,
                                "capturedArgs=" + capturedArgs, "downloadUrl=" + url));
            });
            step("Download commandline and serve report", () -> {
                commandline.download(url, null, new Properties());
                commandline.serve(Collections.singletonList(resultsDirectory), null, 0);
            });

            step("Verify captured serve arguments preserve spaces", () -> {
                final List<String> args = Files.readAllLines(capturedArgs, StandardCharsets.UTF_8);
                addAttachment("Windows serve command arguments",
                        String.join(System.lineSeparator(), args));
                assertThat(args).isEqualTo(
                        Arrays.asList("serve", resultsDirectory.toAbsolutePath().toString()));
            });
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    void shouldPassVerboseFlagAndLogCommandWhenDebugServingReport() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure-commandline");
        try {
            final String version = "2.30.0";
            final Path installDirectory = testDirectory.resolve("install");
            final Path resultsDirectory = testDirectory.resolve("results with space");
            final Path capturedArgs = testDirectory.resolve("args.txt");
            final Path executable =
                    installDirectory.resolve("allure-" + version).resolve("bin").resolve("allure");
            final RecordingLog log = new RecordingLog(true);
            step("Prepare fake Unix runtime and serve results directory", () -> {
                Files.createDirectories(resultsDirectory);
                createUnixAllureExecutable(installDirectory, version, "#!/bin/sh",
                        "printf '%s\\n' \"$@\" > '" + capturedArgs + "'", "exit 0");
                addAttachment("Debug serve inputs", "resultsDirectory=" + resultsDirectory);
            });

            final AllureCommandline commandline =
                    new AllureCommandline(installDirectory, version, 10, log);
            step("Serve report in debug mode",
                    () -> commandline.serve(Collections.singletonList(resultsDirectory), null, 0));

            step("Verify serve command arguments and logged command", () -> {
                final List<String> args = Files.readAllLines(capturedArgs, StandardCharsets.UTF_8);
                addAttachment("Serve command arguments", String.join(System.lineSeparator(), args));
                addAttachment("Debug serve log messages",
                        String.join(System.lineSeparator(), log.debugMessages));
                assertThat(args).isEqualTo(Arrays.asList("--verbose", "serve",
                        resultsDirectory.toAbsolutePath().toString()));
                assertThat(log.debugMessages)
                        .isEqualTo(Collections.singletonList("Executing Allure command: ["
                                + executable.toAbsolutePath() + ", --verbose, serve, "
                                + resultsDirectory.toAbsolutePath() + "]"));
            });
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

    private static final class RecordingLog implements Log {

        private final boolean debugEnabled;

        private final List<String> debugMessages;

        private RecordingLog(final boolean debugEnabled) {
            this.debugEnabled = debugEnabled;
            this.debugMessages = new java.util.ArrayList<String>();
        }

        @Override
        public boolean isDebugEnabled() {
            return debugEnabled;
        }

        @Override
        public void debug(final CharSequence content) {
            debugMessages.add(content.toString());
        }

        @Override
        public void debug(final CharSequence content, final Throwable error) {
            debug(content);
        }

        @Override
        public void debug(final Throwable error) {
            debug(error.getMessage());
        }

        @Override
        public boolean isInfoEnabled() {
            return false;
        }

        @Override
        public void info(final CharSequence content) {}

        @Override
        public void info(final CharSequence content, final Throwable error) {}

        @Override
        public void info(final Throwable error) {}

        @Override
        public boolean isWarnEnabled() {
            return false;
        }

        @Override
        public void warn(final CharSequence content) {}

        @Override
        public void warn(final CharSequence content, final Throwable error) {}

        @Override
        public void warn(final Throwable error) {}

        @Override
        public boolean isErrorEnabled() {
            return false;
        }

        @Override
        public void error(final CharSequence content) {}

        @Override
        public void error(final CharSequence content, final Throwable error) {}

        @Override
        public void error(final Throwable error) {}
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
