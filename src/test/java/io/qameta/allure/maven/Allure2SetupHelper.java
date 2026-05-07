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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Shared setup helpers for hermetic Allure 2 verifier tests.
 */
final class Allure2SetupHelper {

    private static final String RUNNER_CLASS_NAME = FakeAllure2CliRunner.class.getName();
    private static final String RUNNER_CLASS_RESOURCE = "/" + RUNNER_CLASS_NAME.replace('.', '/') + ".class";

    enum Mode {
        FULL,
        SINGLE_FILE,
        COMMANDS,
        ARGS
    }

    private Allure2SetupHelper() {
    }

    static void installFakeCommandlineArtifact(final Path localRepository, final String version,
                                               final Path captureFile, final Path reportDirectory, final Mode mode,
                                               final int testCases)
            throws IOException {
        final Path artifactDirectory = localRepository
                .resolve(Path.of("io", "qameta", "allure", "allure-commandline", version));
        Files.createDirectories(artifactDirectory);

        final Path zipFile = artifactDirectory.resolve("allure-commandline-" + version + ".zip");
        writeArchive(zipFile, version, captureFile, reportDirectory, mode, testCases);
        writePom(artifactDirectory.resolve("allure-commandline-" + version + ".pom"), version);
    }

    static Path createFakeCommandlineArchive(final Path outputDirectory, final String version,
                                             final Path captureFile, final Path reportDirectory, final Mode mode,
                                             final int testCases)
            throws IOException {
        Files.createDirectories(outputDirectory);
        final Path zipFile = outputDirectory.resolve("allure-commandline-" + version + ".zip");
        writeArchive(zipFile, version, captureFile, reportDirectory, mode, testCases);
        return zipFile;
    }

    private static void writeArchive(final Path zipFile, final String version,
                                     final Path captureFile, final Path reportDirectory, final Mode mode,
                                     final int testCases)
            throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            addZipEntry(
                    zip, "allure-" + version + "/bin/allure",
                    createUnixLauncher(captureFile, reportDirectory, mode, testCases)
            );
            addZipEntry(
                    zip, "allure-" + version + "/bin/allure.bat",
                    createWindowsLauncher(captureFile, reportDirectory, mode, testCases)
            );
            addRunnerJar(zip, "allure-" + version + "/lib/fake-allure2-runner.jar");
        }
    }

    private static void writePom(final Path pomFile, final String version) throws IOException {
        final String pom = """
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>io.qameta.allure</groupId>
                    <artifactId>allure-commandline</artifactId>
                    <version>%s</version>
                    <packaging>zip</packaging>
                </project>
                """
                .formatted(version);
        Files.writeString(pomFile, pom, StandardCharsets.UTF_8);
    }

    private static String createUnixLauncher(final Path captureFile, final Path reportDirectory,
                                             final Mode mode, final int testCases) {
        return """
                #!/bin/sh
                set -eu
                BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
                exec java -cp "$BASE_DIR/lib/fake-allure2-runner.jar" %s '%s' '%s' '%s' '%s' "$@"
                """.formatted(
                RUNNER_CLASS_NAME, mode.name(), testCases,
                captureFile.toAbsolutePath(),
                reportDirectory == null ? "-" : reportDirectory.toAbsolutePath()
        );
    }

    private static String createWindowsLauncher(final Path captureFile, final Path reportDirectory,
                                                final Mode mode, final int testCases) {
        return """
                @echo off
                setlocal EnableExtensions DisableDelayedExpansion
                set "BASE_DIR=%%~dp0.."
                java -cp "%%BASE_DIR%%\\lib\\fake-allure2-runner.jar" %s "%s" "%s" "%s" "%s" %%*
                """.formatted(
                RUNNER_CLASS_NAME, mode.name(), testCases,
                captureFile.toAbsolutePath(),
                reportDirectory == null ? "-" : reportDirectory.toAbsolutePath()
        );
    }

    private static void addZipEntry(final ZipOutputStream zip, final String name,
                                    final String content)
            throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void addRunnerJar(final ZipOutputStream zip, final String name)
            throws IOException {
        final Path tempDirectory = Files.createTempDirectory("fake-allure2-runner");
        final Path root = tempDirectory.resolve("classes");
        final Path classFile = root.resolve(RUNNER_CLASS_NAME.replace('.', '/') + ".class");
        Files.createDirectories(classFile.getParent());

        try (InputStream stream = Allure2SetupHelper.class.getResourceAsStream(RUNNER_CLASS_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing runner class resource " + RUNNER_CLASS_RESOURCE
                );
            }
            Files.copy(stream, classFile);
        }

        final Path jarFile = tempDirectory.resolve("fake-allure2-runner.jar");
        try (ZipOutputStream jar = new ZipOutputStream(Files.newOutputStream(jarFile))) {
            addFileToZip(jar, root, classFile);
        }

        zip.putNextEntry(new ZipEntry(name));
        zip.write(Files.readAllBytes(jarFile));
        zip.closeEntry();
    }

    private static void addFileToZip(final ZipOutputStream zip, final Path root, final Path file)
            throws IOException {
        final String entryName = root.relativize(file).toString().replace('\\', '/');
        zip.putNextEntry(new ZipEntry(entryName));
        zip.write(Files.readAllBytes(file));
        zip.closeEntry();
    }
}
