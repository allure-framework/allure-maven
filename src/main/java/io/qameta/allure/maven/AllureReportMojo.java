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
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * @author Dmitry Baev dmitry.baev@qameta.io Date: 30.07.15
 */
@Mojo(
        name = "report",
        defaultPhase = LifecyclePhase.SITE
)
public class AllureReportMojo extends AllureGenerateMojo {

    private static final String FOUND_DIRECTORY = "Found results directory %s";
    private static final String DIRECTORY_NOT_FOUND = "Directory %s not found";
    private static final String HISTORY_DIRECTORY_NAME = "history";
    private static final String REPORT_DIRECTORY_NAME = "report";
    private static final String TO_PATH_SEPARATOR = " to ";

    /**
     * The comma-separated list of additional input directories. As long as unix path can contains
     * commas it is bad way to specify few input directories. The main usage of this parameter is
     * some scripts to generate aggregated report. This parameter will be used only in "bulk" mojo.
     */
    @Parameter(property = "allure.results.inputDirectories")
    protected String inputDirectories;

    @Parameter(
            property = "allure.history.enabled",
            defaultValue = "true"
    )
    protected boolean historyEnabled;

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Path> getInputDirectories() {

        if (StringUtils.isNotBlank(inputDirectories)) {
            return fromInputDirectories();
        }

        final Path path = getInputDirectoryAbsolutePath();
        if (isDirectoryExists(path)) {
            getLog().info(format(FOUND_DIRECTORY, path));
            return Collections.singletonList(path);
        }
        getLog().error(format(DIRECTORY_NOT_FOUND, path));
        return Collections.emptyList();
    }

    private List<Path> fromInputDirectories() {
        final List<Path> results = new ArrayList<>();
        for (String dir : inputDirectories.split(",")) {
            final Path path = Paths.get(dir).toAbsolutePath();
            if (isDirectoryExists(path)) {
                results.add(path);
                getLog().info(format(FOUND_DIRECTORY, path));
            } else {
                getLog().error(format(DIRECTORY_NOT_FOUND, path));
            }
        }
        return results;
    }

    @Override
    protected String getMojoName() {
        return REPORT_DIRECTORY_NAME;
    }

    @Override
    protected List<Path> prepareInputDirectoriesForGenerate(final List<Path> inputDirectories,
                                                            final AllureVersion allureVersion)
            throws IOException {
        if (!historyEnabled || allureVersion.isAllure3()) {
            return inputDirectories;
        }

        final Path cachedHistoryDirectory = getCachedAllure2HistoryDirectory();
        if (!isDirectoryExists(cachedHistoryDirectory)) {
            getLog().info("No cached Allure history found at " + cachedHistoryDirectory);
            return inputDirectories;
        }

        final Path historyInputDirectory = restoreCachedHistoryInput(cachedHistoryDirectory);
        final List<Path> generationInputDirectories = new ArrayList<>(inputDirectories);
        generationInputDirectories.add(historyInputDirectory);
        getLog().info(
                "Restored cached Allure history from " + cachedHistoryDirectory
                        + TO_PATH_SEPARATOR + historyInputDirectory.resolve(HISTORY_DIRECTORY_NAME)
        );
        return generationInputDirectories;
    }

    @Override
    protected void afterGenerateReport(final List<Path> inputDirectories,
                                       final AllureVersion allureVersion)
            throws IOException {
        if (!historyEnabled || allureVersion.isAllure3()) {
            return;
        }

        final Path generatedHistoryDirectory = Paths.get(getReportDirectory()).resolve(HISTORY_DIRECTORY_NAME);
        if (!isDirectoryExists(generatedHistoryDirectory)) {
            return;
        }

        final Path cacheRoot = getReportHistoryCacheDirectory();
        final Path cachedHistoryDirectory = cacheRoot.resolve(HISTORY_DIRECTORY_NAME);
        FileUtils.deleteQuietly(cacheRoot.toFile());
        Files.createDirectories(cacheRoot);
        FileUtils.copyDirectory(
                generatedHistoryDirectory.toFile(),
                cachedHistoryDirectory.toFile()
        );
        getLog().info(
                "Refreshed cached Allure history from " + generatedHistoryDirectory
                        + TO_PATH_SEPARATOR + cachedHistoryDirectory
        );
    }

    @Override
    protected Map<String, Object> getAllure3ConfigDefaults() throws IOException {
        if (!historyEnabled) {
            return Collections.emptyMap();
        }

        final Path historyFile = getReportHistoryCacheDirectory().resolve("history.jsonl");
        Files.createDirectories(historyFile.getParent());
        getLog().info("Using Allure 3 history file " + historyFile.toAbsolutePath());

        final Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("historyPath", historyFile.toAbsolutePath().toString());
        defaults.put("appendHistory", true);
        return defaults;
    }

    private Path getInputDirectoryAbsolutePath() {
        final Path path = Paths.get(resultsDirectory);
        return path.isAbsolute() ? path : Paths.get(buildDirectory).resolve(path);
    }

    private Path restoreCachedHistoryInput(final Path cachedHistoryDirectory) throws IOException {
        final Path historyInputRoot = Paths.get(buildDirectory).resolve(Paths.get("allure-maven", "history-input"));
        final Path historyInputDirectory = historyInputRoot.resolve("allure-results");
        final Path restoredHistoryDirectory = historyInputDirectory.resolve(HISTORY_DIRECTORY_NAME);
        FileUtils.deleteQuietly(historyInputRoot.toFile());
        Files.createDirectories(historyInputDirectory);
        FileUtils.copyDirectory(cachedHistoryDirectory.toFile(), restoredHistoryDirectory.toFile());
        return historyInputDirectory;
    }

    private Path getReportHistoryCacheDirectory() {
        return Paths.get(getInstallDirectory())
                .resolve(Paths.get(HISTORY_DIRECTORY_NAME, REPORT_DIRECTORY_NAME));
    }

    private Path getCachedAllure2HistoryDirectory() {
        return getReportHistoryCacheDirectory().resolve(HISTORY_DIRECTORY_NAME);
    }
}
