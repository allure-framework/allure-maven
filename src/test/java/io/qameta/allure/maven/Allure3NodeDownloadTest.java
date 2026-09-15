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

import com.sun.net.httpserver.HttpServer;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.maven.settings.Proxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static io.qameta.allure.Allure.addAttachment;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
@Tag("allure3")
@Tag("commandline")
class Allure3NodeDownloadTest {

    private static final String NODE_MIRROR = "http://node-mirror.invalid/repository/nodejs/";

    private static final String NODE_VERSION = Allure3Commandline.NODE_DEFAULT_VERSION;

    private final Allure3Platform platform = Allure3Platform.detect();

    private final Map<String, byte[]> responses = new ConcurrentHashMap<>();

    private final List<String> requests = new CopyOnWriteArrayList<>();

    @TempDir
    private Path testDirectory;

    private HttpServer mirror;

    @BeforeEach
    void prepareMirror() throws Exception {
        mirror = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mirror.createContext("/", exchange -> {
            try {
                final String url = exchange.getRequestURI().toString();
                requests.add(exchange.getRequestMethod() + " " + url);
                final byte[] response = responses.get(url);
                if (response == null) {
                    exchange.sendResponseHeaders(404, -1);
                } else {
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                }
            } finally {
                exchange.close();
            }
        });
        mirror.start();

        final byte[] archive = createNodeArchive();
        responses.put(archiveUrl(), archive);
        publishChecksum(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(archive)));
    }

    @AfterEach
    void stopMirror() {
        if (mirror != null) {
            mirror.stop(0);
        }
        addAttachment("Node mirror requests", String.join(System.lineSeparator(), requests));
    }

    @Test
    @Description("A Node.js URL template must fetch the archive and its checksum from the configured mirror.")
    void shouldInstallNodeFromMirrorTemplate() throws Exception {
        final Allure3Commandline commandline = newCommandline(NODE_MIRROR + "v%s/node-v%s-%s.%s");

        step("Install Allure using the mirrored Node.js URL template", commandline::install);

        assertThat(commandline.allureExists()).isTrue();
        assertThat(commandline.getNodeExecutable()).hasBinaryContent(
                Files.readAllBytes(
                        platform.getNodeExecutable(testDirectory.resolve("staging"), NODE_VERSION)
                )
        );
        assertThat(requests).containsExactly("GET " + checksumUrl(), "GET " + archiveUrl());
    }

    @Test
    @Description("A complete Node.js archive URL must resolve SHASUMS256.txt from the same directory.")
    void shouldInstallNodeFromMirrorArchiveUrl() throws Exception {
        final Allure3Commandline commandline = newCommandline(archiveUrl());

        step("Install Allure using the complete mirrored Node.js archive URL", commandline::install);

        assertThat(commandline.allureExists()).isTrue();
        assertThat(requests).containsExactly("GET " + checksumUrl(), "GET " + archiveUrl());
    }

    @Test
    @Description("A mirrored Node.js archive must be rejected before extraction when its SHA-256 differs.")
    void shouldRejectMirrorArchiveWithIncorrectChecksum() {
        publishChecksum("0".repeat(64));
        final Allure3Commandline commandline = newCommandline(archiveUrl());

        final IOException error = step(
                "Attempt installation with an incorrect checksum",
                () -> assertThrows(IOException.class, commandline::install)
        );

        assertThat(error).hasMessageContaining("Checksum mismatch for " + platform.getArchiveFileName(NODE_VERSION));
        assertThat(commandline.getNodeExecutable()).doesNotExist();
        assertThat(requests).containsExactly("GET " + checksumUrl(), "GET " + archiveUrl());
    }

    @Test
    @Description("A missing mirror checksum must stop installation without downloading the archive or falling back.")
    void shouldFailWhenMirrorChecksumIsMissing() {
        responses.remove(checksumUrl());
        final Allure3Commandline commandline = newCommandline(archiveUrl());

        final FileNotFoundException error = step(
                "Attempt installation without mirrored checksums",
                () -> assertThrows(FileNotFoundException.class, commandline::install)
        );

        assertThat(error).hasMessage(checksumUrl());
        assertThat(commandline.getNodeExecutable()).doesNotExist();
        assertThat(requests).containsExactly("GET " + checksumUrl());
    }

    @Step("Create a Node.js archive containing the fake installation runtime")
    private byte[] createNodeArchive() throws Exception {
        final Path staging = testDirectory.resolve("staging");
        Allure3SetupHelper.prepareFakeInstallRuntime(staging, testDirectory.resolve("npm-args.txt"));

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (OutputStream output = platform.isWindows() ? bytes : new GZIPOutputStream(bytes);
                ArchiveOutputStream<ArchiveEntry> archive = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream(
                        platform.isWindows() ? ArchiveStreamFactory.ZIP : ArchiveStreamFactory.TAR, output
                );
                Stream<Path> files = Files.walk(staging)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                final String name = staging.relativize(file).toString().replace('\\', '/');
                archive.putArchiveEntry(archive.createArchiveEntry(file, name));
                Files.copy(file, archive);
                archive.closeArchiveEntry();
            }
        }
        return bytes.toByteArray();
    }

    @Step("Publish the Node.js checksum in the mirror")
    private void publishChecksum(final String checksum) {
        final String content = checksum + "  " + platform.getArchiveFileName(NODE_VERSION) + "\n";
        responses.put(checksumUrl(), content.getBytes(StandardCharsets.UTF_8));
        addAttachment("SHASUMS256.txt", content);
    }

    private Allure3Commandline newCommandline(final String downloadUrl) {
        // Route every download through the fixture server so external hosts cannot be contacted.
        final Proxy proxy = new Proxy();
        proxy.setHost("127.0.0.1");
        proxy.setPort(mirror.getAddress().getPort());
        return new Allure3Commandline(
                testDirectory.resolve("install"), AllureVersion.ALLURE3_DEFAULT_VERSION,
                NODE_VERSION, downloadUrl, null, null, proxy, new Properties(), false, 10
        );
    }

    private String archiveUrl() {
        return NODE_MIRROR + "v" + NODE_VERSION + "/" + platform.getArchiveFileName(NODE_VERSION);
    }

    private String checksumUrl() {
        return NODE_MIRROR + "v" + NODE_VERSION + "/SHASUMS256.txt";
    }
}
