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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.qameta.allure.Allure.addAttachment;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@Tag("report")
class AllureReportMojoTest {

    @Test
    void shouldRestoreCachedAllure2HistoryIntoSeparateInputDirectory() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-report-history-restore");
        try {
            final Path resultsDirectory = workspace.resolve(Paths.get("build", "allure-results"));
            final Path cachedHistoryDirectory = workspace.resolve(Paths.get("install", "history", "report", "history"));
            step("Prepare results directory and cached Allure 2 history", () -> {
                Files.createDirectories(resultsDirectory);
                Files.createDirectories(cachedHistoryDirectory);
                Files.writeString(
                        cachedHistoryDirectory.resolve("history-trend.json"),
                        "{\"cached\":true}", StandardCharsets.UTF_8
                );
                attachValues(
                        "History restore workspace",
                        Map.of(
                                "workspace", workspace, "resultsDirectory", resultsDirectory,
                                "cachedHistoryDirectory", cachedHistoryDirectory
                        )
                );
            });

            final TestReportMojo mojo = step("Create report mojo with history cache enabled", () -> {
                final TestReportMojo reportMojo = new TestReportMojo();
                reportMojo.buildDirectory = workspace.resolve("build").toString();
                reportMojo.installDirectory = workspace.resolve("install").toString();
                reportMojo.historyEnabled = true;
                return reportMojo;
            });

            final List<Path> preparedInputDirectories = step(
                    "Prepare input directories for Allure 2 generation",
                    () -> mojo.prepareInputDirectoriesForGeneratePublic(
                            java.util.Collections.singletonList(resultsDirectory),
                            AllureVersion.resolve("2.30.0")
                    )
            );

            final Path historyInputDirectory = workspace
                    .resolve(Paths.get("build", "allure-maven", "history-input", "allure-results"));
            step("Verify cached history is restored into a dedicated input directory", () -> {
                addAttachment(
                        "Prepared input directories", String.join(
                                System.lineSeparator(),
                                preparedInputDirectories.stream().map(Path::toString).toList()
                        )
                );
                assertThat(preparedInputDirectories).hasSize(2);
                assertThat(preparedInputDirectories.get(0)).isEqualTo(resultsDirectory);
                assertThat(preparedInputDirectories.get(1)).isEqualTo(historyInputDirectory);
                assertThat(
                        historyInputDirectory.resolve(Paths.get("history", "history-trend.json"))
                )
                        .exists();
                assertThat(resultsDirectory.resolve("history")).doesNotExist();
            });
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    void shouldSkipHistoryRestoreWhenDisabled() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-report-history-disabled");
        try {
            final Path resultsDirectory = workspace.resolve(Paths.get("build", "allure-results"));
            final Path cachedHistoryDirectory = workspace.resolve(Paths.get("install", "history", "report", "history"));
            step("Prepare results directory and cached history with restore disabled", () -> {
                Files.createDirectories(resultsDirectory);
                Files.createDirectories(cachedHistoryDirectory);
                Files.writeString(
                        cachedHistoryDirectory.resolve("history-trend.json"),
                        "{\"cached\":true}", StandardCharsets.UTF_8
                );
                attachValues(
                        "History disabled workspace",
                        Map.of(
                                "workspace", workspace, "resultsDirectory", resultsDirectory,
                                "cachedHistoryDirectory", cachedHistoryDirectory
                        )
                );
            });

            final TestReportMojo mojo = step("Create report mojo with history cache disabled", () -> {
                final TestReportMojo reportMojo = new TestReportMojo();
                reportMojo.buildDirectory = workspace.resolve("build").toString();
                reportMojo.installDirectory = workspace.resolve("install").toString();
                reportMojo.historyEnabled = false;
                return reportMojo;
            });

            final List<Path> preparedInputDirectories = step(
                    "Prepare input directories without history restore",
                    () -> mojo.prepareInputDirectoriesForGeneratePublic(
                            java.util.Collections.singletonList(resultsDirectory),
                            AllureVersion.resolve("2.30.0")
                    )
            );

            step("Verify history input directory is not created", () -> {
                addAttachment(
                        "Prepared input directories", String.join(
                                System.lineSeparator(),
                                preparedInputDirectories.stream().map(Path::toString).toList()
                        )
                );
                assertThat(preparedInputDirectories).hasSize(1);
                assertThat(preparedInputDirectories.get(0)).isEqualTo(resultsDirectory);
                assertThat(workspace.resolve(Paths.get("build", "allure-maven", "history-input")))
                        .doesNotExist();
            });
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    void shouldRefreshCachedAllure2HistoryFromGeneratedReport() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-report-history-refresh");
        try {
            final Path reportHistoryDirectory = workspace.resolve(Paths.get("report", "history"));
            final Path cacheRoot = workspace.resolve(Paths.get("install", "history", "report"));
            final Path cachedHistoryDirectory = cacheRoot.resolve("history");
            step("Prepare generated report history and stale cached history", () -> {
                Files.createDirectories(reportHistoryDirectory);
                Files.createDirectories(cachedHistoryDirectory);
                Files.writeString(
                        cachedHistoryDirectory.resolve("obsolete.json"), "obsolete",
                        StandardCharsets.UTF_8
                );
                Files.writeString(
                        reportHistoryDirectory.resolve("history-trend.json"),
                        "{\"generated\":true}", StandardCharsets.UTF_8
                );
                attachValues(
                        "History refresh workspace",
                        Map.of(
                                "workspace", workspace, "reportHistoryDirectory",
                                reportHistoryDirectory, "cachedHistoryDirectory",
                                cachedHistoryDirectory
                        )
                );
            });

            final TestReportMojo mojo = step("Create report mojo with history refresh enabled", () -> {
                final TestReportMojo reportMojo = new TestReportMojo();
                reportMojo.installDirectory = workspace.resolve("install").toString();
                reportMojo.reportDirectory = workspace.resolve("report").toString();
                reportMojo.historyEnabled = true;
                return reportMojo;
            });

            step(
                    "Refresh cached history after report generation",
                    () -> mojo.afterGenerateReportPublic(
                            java.util.Collections.<Path>emptyList(),
                            AllureVersion.resolve("2.30.0")
                    )
            );

            step("Verify stale cache is replaced with generated history", () -> {
                addAttachment(
                        "Refreshed cached history",
                        Files.readString(
                                cachedHistoryDirectory.resolve("history-trend.json"),
                                StandardCharsets.UTF_8
                        )
                );
                assertThat(cachedHistoryDirectory.resolve("obsolete.json")).doesNotExist();
                assertThat(
                        Files.readString(
                                cachedHistoryDirectory.resolve("history-trend.json"),
                                StandardCharsets.UTF_8
                        )
                ).isEqualTo("{\"generated\":true}");
            });
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    void shouldProvideAllure3HistoryDefaultsWhenEnabled() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-report-history-allure3");
        try {
            final RecordingLog log = new RecordingLog();
            final TestReportMojo mojo = step("Create Allure 3 report mojo with history enabled", () -> {
                final TestReportMojo reportMojo = new TestReportMojo();
                reportMojo.installDirectory = workspace.resolve("install").toString();
                reportMojo.historyEnabled = true;
                reportMojo.setLog(log);
                return reportMojo;
            });

            final Map<String, Object> defaults = step("Resolve Allure 3 history defaults", mojo::getAllure3ConfigDefaultsPublic);
            final Path historyFile = workspace.resolve(Paths.get("install", "history", "report", "history.jsonl"));

            step("Verify Allure 3 history defaults and log message", () -> {
                attachValues("Allure 3 history defaults", defaults);
                addAttachment(
                        "History log messages",
                        String.join(System.lineSeparator(), log.infoMessages)
                );
                assertThat(defaults.get("historyPath"))
                        .isEqualTo(historyFile.toAbsolutePath().toString());
                assertThat(defaults.get("appendHistory")).isEqualTo(Boolean.TRUE);
                assertThat(historyFile.getParent()).isDirectory();
                assertThat(log.infoMessages.get(0)).contains("Using Allure 3 history file");
            });
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    private static void attachValues(final String name, final Map<String, ?> values) {
        addAttachment(
                name, String.join(
                        System.lineSeparator(), values.entrySet().stream()
                                .map(entry -> entry.getKey() + "=" + entry.getValue()).toList()
                )
        );
    }

    private static final class TestReportMojo extends AllureReportMojo {

        private List<Path> prepareInputDirectoriesForGeneratePublic(
                                                                    final List<Path> inputDirectories, final AllureVersion allureVersion)
                throws IOException {
            return super.prepareInputDirectoriesForGenerate(inputDirectories, allureVersion);
        }

        private void afterGenerateReportPublic(final List<Path> inputDirectories,
                                               final AllureVersion allureVersion)
                throws IOException {
            super.afterGenerateReport(inputDirectories, allureVersion);
        }

        private Map<String, Object> getAllure3ConfigDefaultsPublic() throws IOException {
            return super.getAllure3ConfigDefaults();
        }
    }

    private static final class RecordingLog implements Log {

        private final List<String> infoMessages = new ArrayList<String>();

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void debug(final CharSequence content) {
        }

        @Override
        public void debug(final CharSequence content, final Throwable error) {
        }

        @Override
        public void debug(final Throwable error) {
        }

        @Override
        public boolean isInfoEnabled() {
            return true;
        }

        @Override
        public void info(final CharSequence content) {
            infoMessages.add(content.toString());
        }

        @Override
        public void info(final CharSequence content, final Throwable error) {
            info(content);
        }

        @Override
        public void info(final Throwable error) {
            info(error.getMessage());
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
