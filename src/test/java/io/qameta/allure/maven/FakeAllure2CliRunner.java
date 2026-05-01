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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Small helper embedded into fake Allure 2 archives for hermetic verifier ITs.
 */
public final class FakeAllure2CliRunner {

    private static final String MODE_ARGS = "ARGS";
    private static final String MODE_COMMANDS = "COMMANDS";
    private static final String MODE_SINGLE_FILE = "SINGLE_FILE";

    private FakeAllure2CliRunner() {}

    public static void main(final String[] args) throws Exception {
        if (args.length < 4) {
            throw new IllegalArgumentException("Expected mode, testCases, captureFile, reportDir");
        }

        final String mode = args[0];
        final int testCases = Integer.parseInt(args[1]);
        final Path captureFile = Path.of(args[2]);
        final Path reportDirectory = "-".equals(args[3]) ? null : Path.of(args[3]);
        final List<String> commandArgs = Arrays.asList(Arrays.copyOfRange(args, 4, args.length));

        if (MODE_ARGS.equals(mode)) {
            writeLines(captureFile, commandArgs);
            return;
        }

        final String command = commandArgs.isEmpty() ? "" : commandArgs.get(0);
        final List<String> captureLines = new ArrayList<>();
        captureLines.add("command=" + command);
        for (String arg : commandArgs) {
            captureLines.add("arg=" + arg);
        }
        captureLines.add("---");
        appendLines(captureFile, captureLines);

        if (!"generate".equals(command)) {
            return;
        }

        final Path outputDirectory = findOutputDirectory(commandArgs, reportDirectory);
        if (outputDirectory == null) {
            return;
        }

        Files.createDirectories(outputDirectory);
        Files.writeString(outputDirectory.resolve("index.html"), "", StandardCharsets.UTF_8);

        if (MODE_SINGLE_FILE.equals(mode) || MODE_COMMANDS.equals(mode)) {
            return;
        }

        final List<Path> inputDirectories = findInputDirectories(commandArgs);
        final Path projectDirectory = findProjectDirectory(captureFile);
        final boolean hasProperties = inputDirectories.stream()
                .anyMatch(thisDirectory -> Files.exists(thisDirectory.resolve("allure.properties"))
                        || Files.exists(thisDirectory.resolve("report.properties")))
                || projectHasProperties(projectDirectory);
        final boolean hasCategories = inputDirectories.stream()
                .anyMatch(directory -> Files.exists(directory.resolve("categories.json")))
                || projectHasCategories(projectDirectory);

        final Path dataDirectory = outputDirectory.resolve("data");
        final Path testCasesDirectory = dataDirectory.resolve("test-cases");
        Files.createDirectories(testCasesDirectory);

        Files.writeString(dataDirectory.resolve("behaviors.json"), "[]", StandardCharsets.UTF_8);
        Files.writeString(dataDirectory.resolve("packages.json"), "[]", StandardCharsets.UTF_8);
        Files.writeString(dataDirectory.resolve("timeline.json"), "[]", StandardCharsets.UTF_8);
        Files.writeString(dataDirectory.resolve("suites.json"), "[]", StandardCharsets.UTF_8);

        final String categories = hasCategories ? "[{\"children\":[{}]}]" : "[]";
        Files.writeString(dataDirectory.resolve("categories.json"), categories,
                StandardCharsets.UTF_8);

        for (int index = 1; index <= testCases; index++) {
            final String json = hasProperties && index == 1
                    ? "{\"links\":[{\"name\":\"issue-123\",\"url\":\"http://example.com/issue-123\"}]}"
                    : "{}";
            Files.writeString(testCasesDirectory.resolve("case-" + index + ".json"), json,
                    StandardCharsets.UTF_8);
        }
    }

    private static Path findProjectDirectory(final Path captureFile) {
        final Path parent = captureFile.getParent();
        if (parent == null) {
            return captureFile.toAbsolutePath().getParent();
        }
        if ("target".equals(parent.getFileName().toString())) {
            return parent.getParent();
        }
        return parent.getParent();
    }

    private static boolean projectHasProperties(final Path projectDirectory) throws IOException {
        if (projectDirectory == null) {
            return false;
        }
        if (Files.exists(projectDirectory.resolve("allure.properties"))
                || Files.exists(projectDirectory.resolve("report.properties"))
                || Files.exists(projectDirectory.resolve("target/classes/allure.properties"))
                || Files.exists(
                        projectDirectory.resolve("target/test-classes/report.properties"))) {
            return true;
        }
        final Path pom = projectDirectory.resolve("pom.xml");
        return Files.exists(pom) && Files.readString(pom, StandardCharsets.UTF_8)
                .contains("allure.issues.tracker.pattern");
    }

    private static boolean projectHasCategories(final Path projectDirectory) {
        return projectDirectory != null
                && Files.exists(projectDirectory.resolve("target/test-classes/categories.json"));
    }

    private static List<Path> findInputDirectories(final List<String> commandArgs) {
        final List<Path> inputDirectories = new ArrayList<>();
        boolean skipNext = true;
        for (String arg : commandArgs) {
            if (skipNext) {
                skipNext = false;
                continue;
            }
            if ("-o".equals(arg) || "--output".equals(arg) || "-c".equals(arg)
                    || "--config".equals(arg)) {
                skipNext = true;
                continue;
            }
            if (!arg.startsWith("-")) {
                final Path candidate = Path.of(arg);
                if (Files.isDirectory(candidate)) {
                    inputDirectories.add(candidate);
                }
            }
        }
        return inputDirectories;
    }

    private static Path findOutputDirectory(final List<String> commandArgs,
            final Path defaultReportDirectory) {
        for (int index = 1; index < commandArgs.size() - 1; index++) {
            final String arg = commandArgs.get(index);
            if ("-o".equals(arg) || "--output".equals(arg)) {
                return Path.of(commandArgs.get(index + 1));
            }
        }
        return defaultReportDirectory;
    }

    private static void writeLines(final Path file, final List<String> lines) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    private static void appendLines(final Path file, final List<String> lines) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.write(file, lines, StandardCharsets.UTF_8,
                Files.exists(file) ? java.nio.file.StandardOpenOption.APPEND
                        : java.nio.file.StandardOpenOption.CREATE);
    }
}
