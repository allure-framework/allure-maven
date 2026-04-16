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
import org.junit.Test;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeFalse;
import static ru.yandex.qatools.matchers.nio.PathMatchers.exists;

public class Allure3CommandlineTest {

    @Test
    public void shouldInstallAllure3WithPrivateNodeAndNpmCli() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure3-commandline");
        try {
            final Path installDirectory = testDirectory.resolve("install with space");
            final Path capturedArgs = testDirectory.resolve("node-args.txt");
            final Allure3Commandline commandline =
                    newCommandline(installDirectory, null, false, 10);

            Allure3SetupHelper.prepareFakeInstallRuntime(installDirectory, capturedArgs);

            commandline.install();

            final List<String> args = Files.readAllLines(capturedArgs, StandardCharsets.UTF_8);
            assertThat(args, is(Arrays.asList("cli=" + commandline.getNpmCliPath().toAbsolutePath(),
                    "arg=--prefix", "arg=" + commandline.getAllureHome().toAbsolutePath(),
                    "arg=install", "arg=--no-package-lock", "arg=--no-save", "arg=--ignore-scripts",
                    "arg=allure@3.4.1", "arg=--registry", "arg=https://registry.npmjs.org")));
            assertThat(commandline.getAllureCliPath(), exists());
            assertThat(commandline.getAllureExecutablePath(), exists());
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    public void shouldInstallAllure3FromLocalPackageArchiveWithoutRegistry() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure3-commandline");
        try {
            final Path installDirectory = testDirectory.resolve("install");
            final Path capturedArgs = testDirectory.resolve("node-args.txt");
            final Path packageArchive = testDirectory.resolve("custom-allure.tgz");
            final Allure3Commandline commandline =
                    newCommandline(installDirectory, packageArchive, false, 10);

            Allure3SetupHelper.prepareFakeInstallRuntime(installDirectory, capturedArgs);
            Allure3SetupHelper.prepareFakePackageArchive(packageArchive);

            commandline.install();

            final List<String> args = Files.readAllLines(capturedArgs, StandardCharsets.UTF_8);
            assertThat(args, hasItem("arg=" + packageArchive.toAbsolutePath()));
            assertThat(args, not(hasItem("arg=--registry")));
            assertThat(args, not(hasItem("arg=allure@3.4.1")));
            assertThat(commandline.getAllureCliPath(), exists());
            assertThat(commandline.getAllureExecutablePath(), exists());
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    public void shouldGenerateReportWithDirectResultsAndAwesomeConfig() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure3-commandline");
        try {
            final Path installDirectory = testDirectory.resolve("install");
            final Path buildDirectory = testDirectory.resolve("build");
            final Path resultsDirectory = testDirectory.resolve("results with space");
            final Path secondResultsDirectory = testDirectory.resolve("second results with space");
            final Path reportDirectory = testDirectory.resolve("report with space");
            final Path capturedArgs = testDirectory.resolve("node-args.txt");
            final Allure3Commandline commandline =
                    newCommandline(installDirectory, null, false, 10);

            Files.createDirectories(resultsDirectory);
            Files.write(resultsDirectory.resolve("sample.json"), Collections.singletonList("{}"),
                    StandardCharsets.UTF_8);
            Files.createDirectories(secondResultsDirectory);
            Files.write(secondResultsDirectory.resolve("sample2.json"),
                    Collections.singletonList("{}"), StandardCharsets.UTF_8);
            Allure3SetupHelper.prepareFakeReportRuntime(installDirectory, capturedArgs,
                    reportDirectory, true);

            final Map<String, Object> defaultConfig = new LinkedHashMap<>();
            defaultConfig.put("historyPath", testDirectory
                    .resolve(Paths.get("history", "history.jsonl")).toAbsolutePath().toString());
            defaultConfig.put("appendHistory", true);

            commandline.generateReport(Arrays.asList(resultsDirectory, secondResultsDirectory),
                    reportDirectory, true, buildDirectory, "Allure", null, defaultConfig);

            final Path config = buildDirectory.resolve("allure-maven").resolve("allure3")
                    .resolve("allurerc.json");
            final JsonNode configJson = new ObjectMapper().readTree(config.toFile());
            assertThat(configJson.get("name").asText(), is("Allure"));
            assertThat(configJson.get("output").asText(),
                    is(reportDirectory.toAbsolutePath().toString()));
            assertThat(configJson.get("historyPath").asText(),
                    is(defaultConfig.get("historyPath")));
            assertThat(configJson.get("appendHistory").asBoolean(), is(true));
            assertThat(configJson.get("plugins").get("awesome").get("options").get("singleFile")
                    .asBoolean(), is(true));
            assertThat(reportDirectory.resolve("index.html"), exists());
            assertThat(reportDirectory.resolve("data").resolve("test-cases").resolve("case.json"),
                    exists());
            assertThat(Files.readAllLines(capturedArgs, StandardCharsets.UTF_8),
                    is(Arrays.asList("cli=" + commandline.getAllureCliPath().toAbsolutePath(),
                            "command=generate", "arg=generate",
                            "arg=" + resultsDirectory.toAbsolutePath(),
                            "arg=" + secondResultsDirectory.toAbsolutePath(), "arg=--config",
                            "arg=" + config.toAbsolutePath(), "---")));
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    public void shouldMergeYamlConfigIntoGeneratedConfig() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure3-commandline");
        try {
            final Path installDirectory = testDirectory.resolve("install");
            final Path buildDirectory = testDirectory.resolve("build");
            final Path resultsDirectory = testDirectory.resolve("results");
            final Path reportDirectory = testDirectory.resolve("report");
            final Path userConfig = testDirectory.resolve("allurerc.yml");
            final Allure3Commandline commandline =
                    newCommandline(installDirectory, null, false, 10);

            Files.createDirectories(resultsDirectory);
            Files.write(resultsDirectory.resolve("sample.json"), Collections.singletonList("{}"),
                    StandardCharsets.UTF_8);
            Files.write(userConfig,
                    Arrays.asList("plugins:", "  custom:", "    enabled: true", "  awesome:",
                            "    options:", "      collapseSuites: true",
                            "      reportLanguage: en"),
                    StandardCharsets.UTF_8);
            Allure3SetupHelper.prepareFakeReportRuntime(installDirectory,
                    testDirectory.resolve("node-args.txt"), reportDirectory, false);

            final Map<String, Object> defaultConfig = new LinkedHashMap<>();
            defaultConfig.put("historyPath", testDirectory
                    .resolve(Paths.get("history", "history.jsonl")).toAbsolutePath().toString());
            defaultConfig.put("appendHistory", true);

            commandline.generateReport(Collections.singletonList(resultsDirectory), reportDirectory,
                    true, buildDirectory, "Allure", userConfig, defaultConfig);

            final Path config = buildDirectory.resolve("allure-maven").resolve("allure3")
                    .resolve("allurerc.json");
            final JsonNode configJson = new ObjectMapper().readTree(config.toFile());
            assertThat(configJson.get("name").asText(), is("Allure"));
            assertThat(configJson.get("output").asText(),
                    is(reportDirectory.toAbsolutePath().toString()));
            assertThat(configJson.get("historyPath").asText(),
                    is(defaultConfig.get("historyPath")));
            assertThat(configJson.get("appendHistory").asBoolean(), is(true));
            assertThat(configJson.get("plugins").get("custom").get("enabled").asBoolean(),
                    is(true));
            assertThat(configJson.get("plugins").get("awesome").get("options").get("collapseSuites")
                    .asBoolean(), is(true));
            assertThat(configJson.get("plugins").get("awesome").get("options").get("reportLanguage")
                    .asText(), is("en"));
            assertThat(configJson.get("plugins").get("awesome").get("options").get("singleFile")
                    .asBoolean(), is(true));
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    public void shouldWrapJavaScriptConfigIntoGeneratedModule() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure3-commandline");
        try {
            final Path installDirectory = testDirectory.resolve("install");
            final Path buildDirectory = testDirectory.resolve("build");
            final Path resultsDirectory = testDirectory.resolve("results");
            final Path reportDirectory = testDirectory.resolve("report");
            final Path userConfig = testDirectory.resolve("allurerc.mjs");
            final Allure3Commandline commandline =
                    newCommandline(installDirectory, null, false, 10);

            Files.createDirectories(resultsDirectory);
            Files.write(resultsDirectory.resolve("sample.json"), Collections.singletonList("{}"),
                    StandardCharsets.UTF_8);
            Files.write(userConfig,
                    Arrays.asList("export default {", "  plugins: {", "    awesome: {",
                            "      options: {", "        reportLanguage: \"en\"", "      }",
                            "    }", "  }", "};"),
                    StandardCharsets.UTF_8);
            Allure3SetupHelper.prepareFakeReportRuntime(installDirectory,
                    testDirectory.resolve("node-args.txt"), reportDirectory, false);

            final Map<String, Object> defaultConfig = new LinkedHashMap<>();
            defaultConfig.put("historyPath", testDirectory
                    .resolve(Paths.get("history", "history.jsonl")).toAbsolutePath().toString());
            defaultConfig.put("appendHistory", true);

            commandline.generateReport(Collections.singletonList(resultsDirectory), reportDirectory,
                    true, buildDirectory, "Allure", userConfig, defaultConfig);

            final Path generatedConfig = buildDirectory.resolve("allure-maven").resolve("allure3")
                    .resolve("allurerc.mjs");
            final List<String> generatedConfigLines =
                    Files.readAllLines(generatedConfig, StandardCharsets.UTF_8);
            assertThat(generatedConfig, exists());
            assertThat(generatedConfigLines, hasItem("import userConfig from " + new ObjectMapper()
                    .writeValueAsString(userConfig.toAbsolutePath().toUri().toString()) + ";"));
            assertThat(generatedConfigLines, hasItem("  name: \"Allure\","));
            assertThat(generatedConfigLines, hasItem("        singleFile: true,"));
            assertThat(generatedConfigLines, hasItem("  historyPath: config.historyPath ?? "
                    + new ObjectMapper().writeValueAsString(defaultConfig.get("historyPath"))
                    + ","));
            assertThat(generatedConfigLines,
                    hasItem("  appendHistory: config.appendHistory ?? true,"));
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test
    public void shouldServeReportWithGenerateThenOpenAndPort() throws Exception {
        assumeFalse(isWindows());

        final Path testDirectory = Files.createTempDirectory("allure3-commandline");
        try {
            final Path installDirectory = testDirectory.resolve("install");
            final Path buildDirectory = testDirectory.resolve("build");
            final Path resultsDirectory = testDirectory.resolve("results with space");
            final Path reportDirectory = testDirectory.resolve("report with space");
            final Path capturedArgs = testDirectory.resolve("node-args.txt");
            final Allure3Commandline commandline =
                    newCommandline(installDirectory, null, false, 10);

            Files.createDirectories(resultsDirectory);
            Files.write(resultsDirectory.resolve("sample.json"), Collections.singletonList("{}"),
                    StandardCharsets.UTF_8);
            Allure3SetupHelper.prepareFakeReportRuntime(installDirectory, capturedArgs,
                    reportDirectory, false);

            commandline.serve(Collections.singletonList(resultsDirectory), reportDirectory, false,
                    buildDirectory, "Allure", 5555, null);

            final Path config = buildDirectory.resolve("allure-maven").resolve("allure3")
                    .resolve("allurerc.json");
            assertThat(reportDirectory.resolve("index.html"), exists());
            assertThat(Files.readAllLines(capturedArgs, StandardCharsets.UTF_8),
                    is(Arrays.asList("cli=" + commandline.getAllureCliPath().toAbsolutePath(),
                            "command=generate", "arg=generate",
                            "arg=" + resultsDirectory.toAbsolutePath(), "arg=--config",
                            "arg=" + config.toAbsolutePath(), "---",
                            "cli=" + commandline.getAllureCliPath().toAbsolutePath(),
                            "command=open", "arg=open", "arg=" + reportDirectory.toAbsolutePath(),
                            "arg=--config", "arg=" + config.toAbsolutePath(), "arg=--port",
                            "arg=5555", "---")));
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    @Test(expected = IOException.class)
    public void shouldFailOfflineWhenPrivateNodeIsMissing() throws Exception {
        final Path testDirectory = Files.createTempDirectory("allure3-commandline");
        try {
            newCommandline(testDirectory.resolve("install"), null, true, 10).install();
        } finally {
            FileUtils.deleteQuietly(testDirectory.toFile());
        }
    }

    private static Allure3Commandline newCommandline(final Path installDirectory,
            final Path packageArchive, final boolean offline, final int timeout) {
        return new Allure3Commandline(installDirectory, "3.4.1",
                Allure3Commandline.NODE_DEFAULT_VERSION,
                Allure3Commandline.NODE_DEFAULT_DOWNLOAD_URL,
                Allure3Commandline.NPM_DEFAULT_REGISTRY, packageArchive, null, new Properties(),
                offline, timeout);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
