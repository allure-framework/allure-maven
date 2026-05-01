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
import java.nio.file.Paths;
import java.util.Map;

/**
 * Shared setup helpers for Allure 3 integration tests.
 */
@SuppressWarnings({"MultipleStringLiterals", "PMD.AvoidDuplicateLiterals"})
final class Allure3SetupHelper {

    private static final String WINDOWS_REPORT_NODE_TEMPLATE =
            "/verifier-data/allure3-report-node-windows.cmd";

    private static final String WINDOWS_REPORT_NODE_DATA_TEMPLATE =
            "/verifier-data/allure3-report-node-data-windows.cmd";

    private static final String WINDOWS_INSTALL_NODE_TEMPLATE =
            "/verifier-data/allure3-install-node-windows.cmd";

    private Allure3SetupHelper() {}

    static void prepareFakeReportRuntime(final Path installDirectory, final Path captureFile,
            final Path reportDirectory, final boolean createDataFiles) throws IOException {
        final Allure3Platform platform = Allure3Platform.detect();
        final Path nodeExecutable = platform.getNodeExecutable(installDirectory,
                Allure3Commandline.NODE_DEFAULT_VERSION);
        final Path allureCli =
                installDirectory.resolve("allure-" + AllureVersion.ALLURE3_DEFAULT_VERSION)
                        .resolve(Paths.get("node_modules", "allure", "cli.js"));

        Files.createDirectories(nodeExecutable.getParent());
        Files.createDirectories(allureCli.getParent());
        Files.write(allureCli, "console.log(\"fake allure\");\n".getBytes(StandardCharsets.UTF_8));

        if (platform.isWindows()) {
            Files.write(nodeExecutable, "fake node marker\r\n".getBytes(StandardCharsets.UTF_8));
            Files.write(nodeExecutable.resolveSibling("node.cmd"),
                    createWindowsReportNode(captureFile, reportDirectory, createDataFiles)
                            .getBytes(StandardCharsets.UTF_8));
            return;
        }

        Files.write(nodeExecutable,
                createUnixReportNode(captureFile, reportDirectory, createDataFiles)
                        .getBytes(StandardCharsets.UTF_8));
        nodeExecutable.toFile().setExecutable(true);
    }

    static void prepareFakeInstallRuntime(final Path installDirectory, final Path captureFile)
            throws IOException {
        final Allure3Platform platform = Allure3Platform.detect();
        final Path nodeExecutable = platform.getNodeExecutable(installDirectory,
                Allure3Commandline.NODE_DEFAULT_VERSION);
        final Path npmCli =
                platform.getNpmCliPath(installDirectory, Allure3Commandline.NODE_DEFAULT_VERSION);

        Files.createDirectories(nodeExecutable.getParent());
        Files.createDirectories(npmCli.getParent());
        Files.write(npmCli, "console.log(\"fake npm\");\n".getBytes(StandardCharsets.UTF_8));

        if (platform.isWindows()) {
            Files.write(nodeExecutable, "fake node marker\r\n".getBytes(StandardCharsets.UTF_8));
            Files.write(nodeExecutable.resolveSibling("node.cmd"),
                    createWindowsInstallNode(captureFile).getBytes(StandardCharsets.UTF_8));
            return;
        }

        Files.write(nodeExecutable,
                createUnixInstallNode(captureFile).getBytes(StandardCharsets.UTF_8));
        nodeExecutable.toFile().setExecutable(true);
    }

    static void prepareFakePackageArchive(final Path packageArchive) throws IOException {
        if (packageArchive.getParent() != null) {
            Files.createDirectories(packageArchive.getParent());
        }
        Files.write(packageArchive, "fake package\n".getBytes(StandardCharsets.UTF_8));
    }

    private static String createUnixReportNode(final Path captureFile, final Path reportDirectory,
            final boolean createDataFiles) {
        final StringBuilder builder = new StringBuilder().append("#!/bin/sh\n").append("set -eu\n")
                .append("mkdir -p '").append(captureFile.getParent()).append("'\n")
                .append("cli=\"$1\"\n").append("shift\n").append("command=\"${1-}\"\n")
                .append("{\n").append("  printf 'cli=%s\\n' \"$cli\"\n")
                .append("  printf 'command=%s\\n' \"$command\"\n")
                .append("  for arg in \"$@\"; do\n").append("    printf 'arg=%s\\n' \"$arg\"\n")
                .append("  done\n").append("  printf '%s\\n' '---'\n").append("} >> '")
                .append(captureFile).append("'\n")
                .append("if [ \"$command\" = \"generate\" ]; then\n");

        appendUnixReportFiles(builder, reportDirectory, createDataFiles);
        builder.append("fi\n").append("exit 0\n");
        return builder.toString();
    }

    private static void appendUnixReportFiles(final StringBuilder builder,
            final Path reportDirectory, final boolean createDataFiles) {
        if (createDataFiles) {
            final Path dataDirectory = reportDirectory.resolve("data");
            builder.append("  mkdir -p '").append(dataDirectory.resolve("test-cases")).append("'\n")
                    .append("  touch '").append(reportDirectory.resolve("index.html")).append("'\n")
                    .append("  touch '").append(dataDirectory.resolve("behaviors.json"))
                    .append("'\n").append("  touch '")
                    .append(dataDirectory.resolve("categories.json")).append("'\n")
                    .append("  touch '").append(dataDirectory.resolve("packages.json"))
                    .append("'\n").append("  touch '")
                    .append(dataDirectory.resolve("timeline.json")).append("'\n")
                    .append("  touch '").append(dataDirectory.resolve("suites.json")).append("'\n")
                    .append("  printf '%s\\n' '{}' > '")
                    .append(dataDirectory.resolve(Paths.get("test-cases", "case.json")))
                    .append("'\n");
            return;
        }

        builder.append("  mkdir -p '").append(reportDirectory).append("'\n").append("  touch '")
                .append(reportDirectory.resolve("index.html")).append("'\n");
    }

    private static String createWindowsReportNode(final Path captureFile,
            final Path reportDirectory, final boolean createDataFiles) throws IOException {
        final String template =
                createDataFiles ? WINDOWS_REPORT_NODE_DATA_TEMPLATE : WINDOWS_REPORT_NODE_TEMPLATE;
        return renderWindowsTemplate(template,
                Map.of("@capture.parent@", windowsPath(captureFile.getParent()), "@capture.file@",
                        windowsPath(captureFile), "@report.directory@",
                        windowsPath(reportDirectory), "@index.file@",
                        windowsPath(reportDirectory.resolve("index.html")), "@data.directory@",
                        windowsPath(reportDirectory.resolve("data")), "@cases.directory@",
                        windowsPath(reportDirectory.resolve(Paths.get("data", "test-cases")))));
    }

    private static String createUnixInstallNode(final Path captureFile) {
        return new StringBuilder().append("#!/bin/sh\n").append("set -eu\n").append("mkdir -p '")
                .append(captureFile.getParent()).append("'\n").append("cli=\"$1\"\n")
                .append("shift\n").append("prefix=''\n").append("prev=''\n").append("{\n")
                .append("  printf 'cli=%s\\n' \"$cli\"\n").append("  for arg in \"$@\"; do\n")
                .append("    printf 'arg=%s\\n' \"$arg\"\n")
                .append("    if [ \"$prev\" = '--prefix' ]; then\n")
                .append("      prefix=\"$arg\"\n").append("    fi\n").append("    prev=\"$arg\"\n")
                .append("  done\n").append("} > '").append(captureFile).append("'\n")
                .append("mkdir -p \"$prefix/node_modules/allure\"\n")
                .append("printf '%s\\n' 'console.log(\"fake allure\")' > ")
                .append("\"$prefix/node_modules/allure/cli.js\"\n").append("exit 0\n").toString();
    }

    private static String createWindowsInstallNode(final Path captureFile) throws IOException {
        return renderWindowsTemplate(WINDOWS_INSTALL_NODE_TEMPLATE, Map.of("@capture.parent@",
                windowsPath(captureFile.getParent()), "@capture.file@", windowsPath(captureFile)));
    }

    private static String renderWindowsTemplate(final String resourcePath,
            final Map<String, String> placeholders) throws IOException {
        String template = readResource(resourcePath);
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            template = template.replace(placeholder.getKey(), placeholder.getValue());
        }
        return template.replace("\r\n", "\n").replace("\n", "\r\n");
    }

    private static String readResource(final String resourcePath) throws IOException {
        try (InputStream stream = Allure3SetupHelper.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String windowsPath(final Path path) {
        return path.toAbsolutePath().toString();
    }
}
