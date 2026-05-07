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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static io.qameta.allure.Allure.addAttachment;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@Tag("unit")
@Tag("allure3")
@Tag("commandline")
class Allure3CommandlineTest {

    @Test
    void shouldInstallAllure3WithPrivateNodeAndNpmCli() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure3-commandline");
        try {
            final Path installDirectory = testDirectory.resolve("install with space");
            final Path capturedArgs = testDirectory.resolve("node-args.txt");
            final Allure3Commandline commandline = newCommandline(installDirectory, null, false, 10);

            step("Prepare fake install runtime", () -> {
                Allure3SetupHelper.prepareFakeInstallRuntime(installDirectory, capturedArgs);
                addAttachment(
                        "Install runtime setup", "installDirectory=" + installDirectory
                                + System.lineSeparator() + "capturedArgs=" + capturedArgs
                );
            });

            step("Install Allure 3 runtime", commandline::install);

            step("Verify npm arguments and installed binaries", () -> {
                final List<String> args = Files.readAllLines(capturedArgs, StandardCharsets.UTF_8);
                addAttachment("Captured npm arguments", String.join(System.lineSeparator(), args));
                assertThat(args).isEqualTo(
                        Arrays.asList(
                                "cli=" + commandline.getNpmCliPath().toAbsolutePath(), "arg=--prefix",
                                "arg=" + commandline.getAllureHome().toAbsolutePath(), "arg=install",
                                "arg=--no-package-lock", "arg=--no-save", "arg=--ignore-scripts",
                                "arg=allure@3.4.1", "arg=--registry", "arg=https://registry.npmjs.org"
                        )
                );
                assertThat(commandline.getAllureCliPath()).exists();
                assertThat(commandline.getAllureExecutablePath()).exists();
            });
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    void shouldInstallAllure3FromLocalPackageArchiveWithoutRegistry() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure3-commandline");
        try {
            final Path installDirectory = testDirectory.resolve("install");
            final Path capturedArgs = testDirectory.resolve("node-args.txt");
            final Path packageArchive = testDirectory.resolve("custom-allure.tgz");
            final Allure3Commandline commandline = newCommandline(installDirectory, packageArchive, false, 10);

            step("Prepare fake install runtime and local package archive", () -> {
                Allure3SetupHelper.prepareFakeInstallRuntime(installDirectory, capturedArgs);
                Allure3SetupHelper.prepareFakePackageArchive(packageArchive);
                addAttachment(
                        "Local package install setup",
                        "installDirectory=" + installDirectory + System.lineSeparator()
                                + "packageArchive=" + packageArchive + System.lineSeparator()
                                + "capturedArgs=" + capturedArgs
                );
            });

            step("Install Allure 3 from local package archive", commandline::install);

            step("Verify archive install arguments and installed binaries", () -> {
                final List<String> args = Files.readAllLines(capturedArgs, StandardCharsets.UTF_8);
                addAttachment(
                        "Captured archive install arguments",
                        String.join(System.lineSeparator(), args)
                );
                assertThat(args).contains("arg=" + packageArchive.toAbsolutePath());
                assertThat(args).doesNotContain("arg=--registry");
                assertThat(args).doesNotContain("arg=allure@3.4.1");
                assertThat(commandline.getAllureCliPath()).exists();
                assertThat(commandline.getAllureExecutablePath()).exists();
            });
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    void shouldGenerateReportWithDirectResultsAndAwesomeConfig() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure3-commandline");
        try {
            final Path installDirectory = testDirectory.resolve("install");
            final Path buildDirectory = testDirectory.resolve("build");
            final Path resultsDirectory = testDirectory.resolve("results with space");
            final Path secondResultsDirectory = testDirectory.resolve("second results with space");
            final Path reportDirectory = testDirectory.resolve("report with space");
            final Path capturedArgs = testDirectory.resolve("node-args.txt");
            final Allure3Commandline commandline = newCommandline(installDirectory, null, false, 10);

            step("Prepare fake results and report runtime", () -> {
                Files.createDirectories(resultsDirectory);
                Files.write(
                        resultsDirectory.resolve("sample.json"),
                        Collections.singletonList("{}"), StandardCharsets.UTF_8
                );
                Files.createDirectories(secondResultsDirectory);
                Files.write(
                        secondResultsDirectory.resolve("sample2.json"),
                        Collections.singletonList("{}"), StandardCharsets.UTF_8
                );
                Allure3SetupHelper.prepareFakeReportRuntime(
                        installDirectory, capturedArgs,
                        reportDirectory, true
                );
                addAttachment(
                        "Report generation inputs",
                        "resultsDirectory=" + resultsDirectory + System.lineSeparator()
                                + "secondResultsDirectory=" + secondResultsDirectory
                                + System.lineSeparator() + "reportDirectory=" + reportDirectory
                );
            });

            final Map<String, Object> defaultConfig = new LinkedHashMap<>();
            defaultConfig.put(
                    "historyPath", testDirectory
                            .resolve(Paths.get("history", "history.jsonl")).toAbsolutePath().toString()
            );
            defaultConfig.put("appendHistory", true);

            step(
                    "Generate report with generated Allure 3 config",
                    () -> commandline.generateReport(
                            Arrays.asList(resultsDirectory, secondResultsDirectory),
                            reportDirectory, true, buildDirectory, "Allure", null, defaultConfig
                    )
            );

            step("Verify generated config, report files, and CLI arguments", () -> {
                final Path config = buildDirectory.resolve("allure-maven").resolve("allure3")
                        .resolve("allurerc.json");
                final JsonNode configJson = new ObjectMapper().readTree(config.toFile());
                addAttachment("Generated Allure 3 config", Files.readString(config));
                assertThat(configJson.get("name").asText()).isEqualTo("Allure");
                assertThat(configJson.get("output").asText())
                        .isEqualTo(reportDirectory.toAbsolutePath().toString());
                assertThat(configJson.get("historyPath").asText())
                        .isEqualTo(defaultConfig.get("historyPath"));
                assertThat(configJson.get("appendHistory").asBoolean()).isTrue();
                assertThat(
                        configJson.get("plugins").get("awesome").get("options").get("singleFile")
                                .asBoolean()
                ).isTrue();
                assertThat(reportDirectory.resolve("index.html")).exists();
                assertThat(
                        reportDirectory.resolve("data").resolve("test-cases").resolve("case.json")
                )
                        .exists();
                final List<String> args = Files.readAllLines(capturedArgs, StandardCharsets.UTF_8);
                addAttachment(
                        "Generated report CLI arguments",
                        String.join(System.lineSeparator(), args)
                );
                assertThat(args).isEqualTo(
                        Arrays.asList(
                                "cli=" + commandline.getAllureCliPath().toAbsolutePath(),
                                "command=generate", "arg=generate",
                                "arg=" + resultsDirectory.toAbsolutePath(),
                                "arg=" + secondResultsDirectory.toAbsolutePath(), "arg=--config",
                                "arg=" + config.toAbsolutePath(), "---"
                        )
                );
            });
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    void shouldLogGenerateCommandInDebugModeWithoutVerboseFlag() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure3-commandline");
        try {
            final Path installDirectory = testDirectory.resolve("install");
            final Path buildDirectory = testDirectory.resolve("build");
            final Path resultsDirectory = testDirectory.resolve("results with space");
            final Path reportDirectory = testDirectory.resolve("report with space");
            final Path capturedArgs = testDirectory.resolve("node-args.txt");
            final RecordingLog log = new RecordingLog(true);
            final Allure3Commandline commandline = newCommandline(installDirectory, null, false, 10, log);

            step("Prepare fake results and debug report runtime", () -> {
                Files.createDirectories(resultsDirectory);
                Files.write(
                        resultsDirectory.resolve("sample.json"),
                        Collections.singletonList("{}"), StandardCharsets.UTF_8
                );
                Allure3SetupHelper.prepareFakeReportRuntime(
                        installDirectory, capturedArgs,
                        reportDirectory, false
                );
                addAttachment(
                        "Debug report inputs", "resultsDirectory=" + resultsDirectory
                                + System.lineSeparator() + "reportDirectory=" + reportDirectory
                );
            });

            step(
                    "Generate report in debug mode",
                    () -> commandline.generateReport(
                            Collections.singletonList(resultsDirectory),
                            reportDirectory, false, buildDirectory, "Allure", null
                    )
            );

            step("Verify generated debug command log", () -> {
                final Path config = buildDirectory.resolve("allure-maven").resolve("allure3")
                        .resolve("allurerc.json");
                addAttachment("Generated debug config", Files.readString(config));
                addAttachment(
                        "Debug log messages",
                        String.join(System.lineSeparator(), log.debugMessages)
                );
                assertThat(log.debugMessages)
                        .isEqualTo(
                                Collections.singletonList(
                                        "Executing Allure command: ["
                                                + commandline.getAllureExecutablePath().toAbsolutePath()
                                                + ", generate, " + resultsDirectory.toAbsolutePath()
                                                + ", --config, " + config.toAbsolutePath() + "]"
                                )
                        );
            });
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    void shouldMergeYamlConfigIntoGeneratedConfig() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure3-commandline");
        try {
            final Path installDirectory = testDirectory.resolve("install");
            final Path buildDirectory = testDirectory.resolve("build");
            final Path resultsDirectory = testDirectory.resolve("results");
            final Path reportDirectory = testDirectory.resolve("report");
            final Path userConfig = testDirectory.resolve("allurerc.yml");
            final Allure3Commandline commandline = newCommandline(installDirectory, null, false, 10);

            step("Prepare fake results, user YAML config, and report runtime", () -> {
                Files.createDirectories(resultsDirectory);
                Files.write(
                        resultsDirectory.resolve("sample.json"),
                        Collections.singletonList("{}"), StandardCharsets.UTF_8
                );
                Files.write(
                        userConfig,
                        Arrays.asList(
                                "plugins:", "  custom:", "    enabled: true", "  awesome:",
                                "    options:", "      collapseSuites: true",
                                "      reportLanguage: en"
                        ),
                        StandardCharsets.UTF_8
                );
                Allure3SetupHelper.prepareFakeReportRuntime(
                        installDirectory,
                        testDirectory.resolve("node-args.txt"), reportDirectory, false
                );
                addAttachment("User YAML config", Files.readString(userConfig));
            });

            final Map<String, Object> defaultConfig = new LinkedHashMap<>();
            defaultConfig.put(
                    "historyPath", testDirectory
                            .resolve(Paths.get("history", "history.jsonl")).toAbsolutePath().toString()
            );
            defaultConfig.put("appendHistory", true);

            step(
                    "Generate report with merged YAML config",
                    () -> commandline.generateReport(
                            Collections.singletonList(resultsDirectory),
                            reportDirectory, true, buildDirectory, "Allure", userConfig,
                            defaultConfig
                    )
            );

            step("Verify merged YAML config in generated report config", () -> {
                final Path config = buildDirectory.resolve("allure-maven").resolve("allure3")
                        .resolve("allurerc.json");
                final JsonNode configJson = new ObjectMapper().readTree(config.toFile());
                addAttachment("Merged Allure 3 config", Files.readString(config));
                assertThat(configJson.get("name").asText()).isEqualTo("Allure");
                assertThat(configJson.get("output").asText())
                        .isEqualTo(reportDirectory.toAbsolutePath().toString());
                assertThat(configJson.get("historyPath").asText())
                        .isEqualTo(defaultConfig.get("historyPath"));
                assertThat(configJson.get("appendHistory").asBoolean()).isTrue();
                assertThat(configJson.get("plugins").get("custom").get("enabled").asBoolean())
                        .isTrue();
                assertThat(
                        configJson.get("plugins").get("awesome").get("options")
                                .get("collapseSuites").asBoolean()
                ).isTrue();
                assertThat(
                        configJson.get("plugins").get("awesome").get("options")
                                .get("reportLanguage").asText()
                ).isEqualTo("en");
                assertThat(
                        configJson.get("plugins").get("awesome").get("options").get("singleFile")
                                .asBoolean()
                ).isTrue();
            });
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    void shouldWrapJavaScriptConfigIntoGeneratedModule() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure3-commandline");
        try {
            final Path installDirectory = testDirectory.resolve("install");
            final Path buildDirectory = testDirectory.resolve("build");
            final Path resultsDirectory = testDirectory.resolve("results");
            final Path reportDirectory = testDirectory.resolve("report");
            final Path userConfig = testDirectory.resolve("allurerc.mjs");
            final Allure3Commandline commandline = newCommandline(installDirectory, null, false, 10);

            step("Prepare fake results, user JavaScript config, and report runtime", () -> {
                Files.createDirectories(resultsDirectory);
                Files.write(
                        resultsDirectory.resolve("sample.json"),
                        Collections.singletonList("{}"), StandardCharsets.UTF_8
                );
                Files.write(
                        userConfig,
                        Arrays.asList(
                                "export default {", "  plugins: {", "    awesome: {",
                                "      options: {", "        reportLanguage: \"en\"", "      }",
                                "    }", "  }", "};"
                        ),
                        StandardCharsets.UTF_8
                );
                Allure3SetupHelper.prepareFakeReportRuntime(
                        installDirectory,
                        testDirectory.resolve("node-args.txt"), reportDirectory, false
                );
                addAttachment("User JavaScript config", Files.readString(userConfig));
            });

            final Map<String, Object> defaultConfig = new LinkedHashMap<>();
            defaultConfig.put(
                    "historyPath", testDirectory
                            .resolve(Paths.get("history", "history.jsonl")).toAbsolutePath().toString()
            );
            defaultConfig.put("appendHistory", true);

            step(
                    "Generate report with wrapped JavaScript config",
                    () -> commandline.generateReport(
                            Collections.singletonList(resultsDirectory),
                            reportDirectory, true, buildDirectory, "Allure", userConfig,
                            defaultConfig
                    )
            );

            step("Verify generated JavaScript wrapper config", () -> {
                final Path generatedConfig = buildDirectory.resolve("allure-maven")
                        .resolve("allure3").resolve("allurerc.mjs");
                final List<String> generatedConfigLines = Files.readAllLines(generatedConfig, StandardCharsets.UTF_8);
                addAttachment(
                        "Generated JavaScript wrapper config",
                        String.join(System.lineSeparator(), generatedConfigLines)
                );
                assertThat(generatedConfig).exists();
                assertThat(generatedConfigLines)
                        .contains(
                                "import userConfig from "
                                        + new ObjectMapper().writeValueAsString(
                                                userConfig.toAbsolutePath().toUri().toString()
                                        )
                                        + ";"
                        );
                assertThat(generatedConfigLines).contains("  name: \"Allure\",");
                assertThat(generatedConfigLines).contains("        singleFile: true,");
                assertThat(generatedConfigLines).contains(
                        "  historyPath: config.historyPath ?? "
                                + new ObjectMapper().writeValueAsString(defaultConfig.get("historyPath"))
                                + ","
                );
                assertThat(generatedConfigLines)
                        .contains("  appendHistory: config.appendHistory ?? true,");
            });
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    void shouldServeReportWithGenerateThenOpenAndPort() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure3-commandline");
        try {
            final Path installDirectory = testDirectory.resolve("install");
            final Path buildDirectory = testDirectory.resolve("build");
            final Path resultsDirectory = testDirectory.resolve("results with space");
            final Path reportDirectory = testDirectory.resolve("report with space");
            final Path capturedArgs = testDirectory.resolve("node-args.txt");
            final Allure3Commandline commandline = newCommandline(installDirectory, null, false, 10);

            step("Prepare fake results and report runtime for serve flow", () -> {
                Files.createDirectories(resultsDirectory);
                Files.write(
                        resultsDirectory.resolve("sample.json"),
                        Collections.singletonList("{}"), StandardCharsets.UTF_8
                );
                Allure3SetupHelper.prepareFakeReportRuntime(
                        installDirectory, capturedArgs,
                        reportDirectory, false
                );
                addAttachment(
                        "Serve flow inputs", "resultsDirectory=" + resultsDirectory
                                + System.lineSeparator() + "reportDirectory=" + reportDirectory
                );
            });

            step(
                    "Generate and open report through serve flow",
                    () -> commandline.serve(
                            Collections.singletonList(resultsDirectory),
                            reportDirectory, false, buildDirectory, "Allure", 5555, null
                    )
            );

            step("Verify generated report and open command arguments", () -> {
                final Path config = buildDirectory.resolve("allure-maven").resolve("allure3")
                        .resolve("allurerc.json");
                addAttachment("Serve flow config", Files.readString(config));
                assertThat(reportDirectory.resolve("index.html")).exists();
                final List<String> args = Files.readAllLines(capturedArgs, StandardCharsets.UTF_8);
                addAttachment(
                        "Serve flow CLI arguments",
                        String.join(System.lineSeparator(), args)
                );
                assertThat(args).isEqualTo(
                        Arrays.asList(
                                "cli=" + commandline.getAllureCliPath().toAbsolutePath(),
                                "command=generate", "arg=generate",
                                "arg=" + resultsDirectory.toAbsolutePath(), "arg=--config",
                                "arg=" + config.toAbsolutePath(), "---",
                                "cli=" + commandline.getAllureCliPath().toAbsolutePath(), "command=open",
                                "arg=open", "arg=" + reportDirectory.toAbsolutePath(), "arg=--config",
                                "arg=" + config.toAbsolutePath(), "arg=--port", "arg=5555", "---"
                        )
                );
            });
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    void shouldLogGenerateAndOpenCommandsInDebugModeWithoutVerboseFlag() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure3-commandline");
        try {
            final Path installDirectory = testDirectory.resolve("install");
            final Path buildDirectory = testDirectory.resolve("build");
            final Path resultsDirectory = testDirectory.resolve("results with space");
            final Path reportDirectory = testDirectory.resolve("report with space");
            final Path capturedArgs = testDirectory.resolve("node-args.txt");
            final RecordingLog log = new RecordingLog(true);
            final Allure3Commandline commandline = newCommandline(installDirectory, null, false, 10, log);

            step("Prepare fake results and debug report runtime for serve flow", () -> {
                Files.createDirectories(resultsDirectory);
                Files.write(
                        resultsDirectory.resolve("sample.json"),
                        Collections.singletonList("{}"), StandardCharsets.UTF_8
                );
                Allure3SetupHelper.prepareFakeReportRuntime(
                        installDirectory, capturedArgs,
                        reportDirectory, false
                );
                addAttachment(
                        "Debug serve flow inputs", "resultsDirectory=" + resultsDirectory
                                + System.lineSeparator() + "reportDirectory=" + reportDirectory
                );
            });

            step(
                    "Generate and open report in debug mode",
                    () -> commandline.serve(
                            Collections.singletonList(resultsDirectory),
                            reportDirectory, false, buildDirectory, "Allure", 5555, null
                    )
            );

            step("Verify logged generate and open commands", () -> {
                final Path config = buildDirectory.resolve("allure-maven").resolve("allure3")
                        .resolve("allurerc.json");
                addAttachment("Debug serve flow config", Files.readString(config));
                addAttachment(
                        "Generate and open debug log messages",
                        String.join(System.lineSeparator(), log.debugMessages)
                );
                assertThat(log.debugMessages)
                        .isEqualTo(
                                Arrays.asList(
                                        "Executing Allure command: ["
                                                + commandline.getAllureExecutablePath()
                                                        .toAbsolutePath()
                                                + ", generate, " + resultsDirectory.toAbsolutePath()
                                                + ", --config, " + config.toAbsolutePath() + "]",
                                        "Executing Allure command: ["
                                                + commandline.getAllureExecutablePath()
                                                        .toAbsolutePath()
                                                + ", open, " + reportDirectory.toAbsolutePath()
                                                + ", --config, " + config.toAbsolutePath()
                                                + ", --port, 5555]"
                                )
                        );
            });
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    void shouldFailOfflineWhenPrivateNodeIsMissing() throws Exception {
        final Path testDirectory = Files.createTempDirectory("allure3-commandline");
        try {
            assertThrows(
                    IOException.class,
                    () -> newCommandline(testDirectory.resolve("install"), null, true, 10)
                            .install()
            );
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    private static Allure3Commandline newCommandline(final Path installDirectory,
                                                     final Path packageArchive, final boolean offline, final int timeout) {
        return newCommandline(installDirectory, packageArchive, offline, timeout, null);
    }

    private static Allure3Commandline newCommandline(final Path installDirectory,
                                                     final Path packageArchive, final boolean offline, final int timeout, final Log log) {
        return new Allure3Commandline(
                installDirectory, "3.4.1",
                Allure3Commandline.NODE_DEFAULT_VERSION,
                Allure3Commandline.NODE_DEFAULT_DOWNLOAD_URL,
                Allure3Commandline.NPM_DEFAULT_REGISTRY, packageArchive, null, new Properties(),
                offline, timeout, log
        );
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
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
        public void info(final CharSequence content) {
        }

        @Override
        public void info(final CharSequence content, final Throwable error) {
        }

        @Override
        public void info(final Throwable error) {
        }

        @Override
        public boolean isWarnEnabled() {
            return false;
        }

        @Override
        public void warn(final CharSequence content) {
        }

        @Override
        public void warn(final CharSequence content, final Throwable error) {
        }

        @Override
        public void warn(final Throwable error) {
        }

        @Override
        public boolean isErrorEnabled() {
            return false;
        }

        @Override
        public void error(final CharSequence content) {
        }

        @Override
        public void error(final CharSequence content, final Throwable error) {
        }

        @Override
        public void error(final Throwable error) {
        }
    }
}
