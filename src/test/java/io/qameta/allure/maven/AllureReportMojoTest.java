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
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class AllureReportMojoTest {

    @Test
    public void shouldRestoreCachedAllure2HistoryIntoSeparateInputDirectory() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-report-history-restore");
        try {
            final Path resultsDirectory = workspace.resolve(Paths.get("build", "allure-results"));
            final Path cachedHistoryDirectory =
                    workspace.resolve(Paths.get("install", "history", "report", "history"));
            Files.createDirectories(resultsDirectory);
            Files.createDirectories(cachedHistoryDirectory);
            Files.writeString(cachedHistoryDirectory.resolve("history-trend.json"),
                    "{\"cached\":true}", StandardCharsets.UTF_8);

            final TestReportMojo mojo = new TestReportMojo();
            mojo.buildDirectory = workspace.resolve("build").toString();
            mojo.installDirectory = workspace.resolve("install").toString();
            mojo.historyEnabled = true;

            final List<Path> preparedInputDirectories =
                    mojo.prepareInputDirectoriesForGeneratePublic(
                            java.util.Collections.singletonList(resultsDirectory),
                            AllureVersion.resolve("2.30.0"));

            final Path historyInputDirectory = workspace
                    .resolve(Paths.get("build", "allure-maven", "history-input", "allure-results"));
            assertThat(preparedInputDirectories.size(), is(2));
            assertThat(preparedInputDirectories.get(0), is(resultsDirectory));
            assertThat(preparedInputDirectories.get(1), is(historyInputDirectory));
            assertThat(Files.exists(
                    historyInputDirectory.resolve(Paths.get("history", "history-trend.json"))),
                    is(true));
            assertThat(Files.exists(resultsDirectory.resolve("history")), is(false));
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    public void shouldSkipHistoryRestoreWhenDisabled() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-report-history-disabled");
        try {
            final Path resultsDirectory = workspace.resolve(Paths.get("build", "allure-results"));
            final Path cachedHistoryDirectory =
                    workspace.resolve(Paths.get("install", "history", "report", "history"));
            Files.createDirectories(resultsDirectory);
            Files.createDirectories(cachedHistoryDirectory);
            Files.writeString(cachedHistoryDirectory.resolve("history-trend.json"),
                    "{\"cached\":true}", StandardCharsets.UTF_8);

            final TestReportMojo mojo = new TestReportMojo();
            mojo.buildDirectory = workspace.resolve("build").toString();
            mojo.installDirectory = workspace.resolve("install").toString();
            mojo.historyEnabled = false;

            final List<Path> preparedInputDirectories =
                    mojo.prepareInputDirectoriesForGeneratePublic(
                            java.util.Collections.singletonList(resultsDirectory),
                            AllureVersion.resolve("2.30.0"));

            assertThat(preparedInputDirectories.size(), is(1));
            assertThat(preparedInputDirectories.get(0), is(resultsDirectory));
            assertThat(
                    Files.exists(
                            workspace.resolve(Paths.get("build", "allure-maven", "history-input"))),
                    is(false));
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    public void shouldRefreshCachedAllure2HistoryFromGeneratedReport() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-report-history-refresh");
        try {
            final Path reportHistoryDirectory = workspace.resolve(Paths.get("report", "history"));
            final Path cacheRoot = workspace.resolve(Paths.get("install", "history", "report"));
            final Path cachedHistoryDirectory = cacheRoot.resolve("history");
            Files.createDirectories(reportHistoryDirectory);
            Files.createDirectories(cachedHistoryDirectory);
            Files.writeString(cachedHistoryDirectory.resolve("obsolete.json"), "obsolete",
                    StandardCharsets.UTF_8);
            Files.writeString(reportHistoryDirectory.resolve("history-trend.json"),
                    "{\"generated\":true}", StandardCharsets.UTF_8);

            final TestReportMojo mojo = new TestReportMojo();
            mojo.installDirectory = workspace.resolve("install").toString();
            mojo.reportDirectory = workspace.resolve("report").toString();
            mojo.historyEnabled = true;

            mojo.afterGenerateReportPublic(java.util.Collections.<Path>emptyList(),
                    AllureVersion.resolve("2.30.0"));

            assertThat(Files.exists(cachedHistoryDirectory.resolve("obsolete.json")), is(false));
            assertThat(Files.readString(cachedHistoryDirectory.resolve("history-trend.json"),
                    StandardCharsets.UTF_8), is("{\"generated\":true}"));
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    public void shouldProvideAllure3HistoryDefaultsWhenEnabled() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-report-history-allure3");
        try {
            final RecordingLog log = new RecordingLog();
            final TestReportMojo mojo = new TestReportMojo();
            mojo.installDirectory = workspace.resolve("install").toString();
            mojo.historyEnabled = true;
            mojo.setLog(log);

            final Map<String, Object> defaults = mojo.getAllure3ConfigDefaultsPublic();
            final Path historyFile =
                    workspace.resolve(Paths.get("install", "history", "report", "history.jsonl"));

            assertThat(defaults.get("historyPath"), is(historyFile.toAbsolutePath().toString()));
            assertThat(defaults.get("appendHistory"), is((Object) Boolean.TRUE));
            assertThat(Files.isDirectory(historyFile.getParent()), is(true));
            assertThat(log.infoMessages.get(0), containsString("Using Allure 3 history file"));
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    private static final class TestReportMojo extends AllureReportMojo {

        private List<Path> prepareInputDirectoriesForGeneratePublic(
                final List<Path> inputDirectories, final AllureVersion allureVersion)
                throws IOException {
            return super.prepareInputDirectoriesForGenerate(inputDirectories, allureVersion);
        }

        private void afterGenerateReportPublic(final List<Path> inputDirectories,
                final AllureVersion allureVersion) throws IOException {
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
        public void debug(final CharSequence content) {}

        @Override
        public void debug(final CharSequence content, final Throwable error) {}

        @Override
        public void debug(final Throwable error) {}

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
}
