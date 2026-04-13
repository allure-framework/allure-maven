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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Shared setup helpers for Allure 3 integration tests.
 */
@SuppressWarnings({"MultipleStringLiterals", "PMD.AvoidDuplicateLiterals"})
public final class Allure3SetupHelper {

    private Allure3SetupHelper() {}

    public static void prepareFakeReportRuntime(final Path installDirectory, final Path captureFile,
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
            Files.write(nodeExecutable,
                    createWindowsReportNode(captureFile, reportDirectory, createDataFiles)
                            .getBytes(StandardCharsets.UTF_8));
            return;
        }

        Files.write(nodeExecutable,
                createUnixReportNode(captureFile, reportDirectory, createDataFiles)
                        .getBytes(StandardCharsets.UTF_8));
        nodeExecutable.toFile().setExecutable(true);
    }

    public static void prepareFakeInstallRuntime(final Path installDirectory,
            final Path captureFile) throws IOException {
        final Allure3Platform platform = Allure3Platform.detect();
        final Path nodeExecutable = platform.getNodeExecutable(installDirectory,
                Allure3Commandline.NODE_DEFAULT_VERSION);
        final Path npmCli =
                platform.getNpmCliPath(installDirectory, Allure3Commandline.NODE_DEFAULT_VERSION);

        Files.createDirectories(nodeExecutable.getParent());
        Files.createDirectories(npmCli.getParent());
        Files.write(npmCli, "console.log(\"fake npm\");\n".getBytes(StandardCharsets.UTF_8));

        if (platform.isWindows()) {
            Files.write(nodeExecutable,
                    createWindowsInstallNode(captureFile).getBytes(StandardCharsets.UTF_8));
            return;
        }

        Files.write(nodeExecutable,
                createUnixInstallNode(captureFile).getBytes(StandardCharsets.UTF_8));
        nodeExecutable.toFile().setExecutable(true);
    }

    public static void prepareFakePackageArchive(final Path packageArchive) throws IOException {
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
            final Path reportDirectory, final boolean createDataFiles) {
        final StringBuilder builder = new StringBuilder().append("@echo off\r\n")
                .append("setlocal EnableExtensions DisableDelayedExpansion\r\n")
                .append("if not exist \"").append(captureFile.getParent()).append("\" mkdir \"")
                .append(captureFile.getParent()).append("\"\r\n").append(">> \"")
                .append(captureFile).append("\" echo(cli=%~1\r\n").append("set \"COMMAND=%~2\"\r\n")
                .append(">> \"").append(captureFile).append("\" echo(command=%COMMAND%\r\n")
                .append(":loop\r\n").append("if \"%~1\"==\"\" goto afterCapture\r\n")
                .append(">> \"").append(captureFile).append("\" echo(arg=%~1\r\n")
                .append("shift\r\n").append("goto loop\r\n").append(":afterCapture\r\n")
                .append(">> \"").append(captureFile).append("\" echo(---\r\n")
                .append("if /I not \"%COMMAND%\"==\"generate\" exit /b 0\r\n");

        appendWindowsReportFiles(builder, reportDirectory, createDataFiles);
        builder.append("exit /b 0\r\n");
        return builder.toString();
    }

    private static void appendWindowsReportFiles(final StringBuilder builder,
            final Path reportDirectory, final boolean createDataFiles) {
        if (createDataFiles) {
            final String dataDirectory =
                    reportDirectory.resolve("data").toAbsolutePath().toString();
            final String casesDirectory = reportDirectory.resolve(Paths.get("data", "test-cases"))
                    .toAbsolutePath().toString();
            builder.append("if not exist \"").append(casesDirectory).append("\" mkdir \"")
                    .append(casesDirectory).append("\"\r\n").append("type nul > \"")
                    .append(reportDirectory.resolve("index.html")).append("\"\r\n")
                    .append("type nul > \"").append(dataDirectory).append("\\behaviors.json\"\r\n")
                    .append("type nul > \"").append(dataDirectory).append("\\categories.json\"\r\n")
                    .append("type nul > \"").append(dataDirectory).append("\\packages.json\"\r\n")
                    .append("type nul > \"").append(dataDirectory).append("\\timeline.json\"\r\n")
                    .append("type nul > \"").append(dataDirectory).append("\\suites.json\"\r\n")
                    .append("echo {} > \"").append(casesDirectory).append("\\case.json\"\r\n");
            return;
        }

        builder.append("if not exist \"").append(reportDirectory).append("\" mkdir \"")
                .append(reportDirectory).append("\"\r\n").append("type nul > \"")
                .append(reportDirectory.resolve("index.html")).append("\"\r\n");
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

    private static String createWindowsInstallNode(final Path captureFile) {
        return new StringBuilder().append("@echo off\r\n")
                .append("setlocal EnableExtensions DisableDelayedExpansion\r\n")
                .append("if not exist \"").append(captureFile.getParent()).append("\" mkdir \"")
                .append(captureFile.getParent()).append("\"\r\n").append("type nul > \"")
                .append(captureFile).append("\"\r\n").append(">> \"").append(captureFile)
                .append("\" echo(cli=%~1\r\n").append("set \"PREFIX=\"\r\n").append(":loop\r\n")
                .append("shift\r\n").append("if \"%~1\"==\"\" goto done\r\n").append(">> \"")
                .append(captureFile).append("\" echo(arg=%~1\r\n")
                .append("if \"%~1\"==\"--prefix\" (\r\n").append("  shift\r\n")
                .append("  set \"PREFIX=%~1\"\r\n").append("  >> \"").append(captureFile)
                .append("\" echo(arg=%~1\r\n").append(")\r\n").append("goto loop\r\n")
                .append(":done\r\n").append("if not defined PREFIX exit /b 1\r\n")
                .append("if not exist \"%PREFIX%\\node_modules\\allure\" mkdir ")
                .append("\"%PREFIX%\\node_modules\\allure\"\r\n")
                .append("echo console.log(\"fake allure\") > ")
                .append("\"%PREFIX%\\node_modules\\allure\\cli.js\"\r\n").append("exit /b 0\r\n")
                .toString();
    }
}
