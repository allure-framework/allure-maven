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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("verifier")
@Tag("allure3")
class VerifierAllure3IT extends VerifierTestSupport {

    @Test
    void shouldInstallAllure3FromPackageArchiveUsingVerifier() throws Exception {
        final Path projectDirectory = prepareProject("allure3-package-path-install",
                rootPom("allure3-package-path-install"));
        final Path installDirectory = projectDirectory.resolve(".allure install");
        final Path captureFile =
                projectDirectory.resolve(Path.of("target", "allure3 package install args.txt"));
        final Path packageArchive =
                projectDirectory.resolve(Path.of("packages", "custom-allure.tgz"));

        Allure3SetupHelper.prepareFakeInstallRuntime(installDirectory, captureFile);
        Allure3SetupHelper.prepareFakePackageArchive(packageArchive);

        runGoals(projectDirectory, List.of("site"));

        final Path allureCli = allureCli(installDirectory);
        assertThat(allureCli).exists();

        final List<String> args = readLines(captureFile);
        assertThat(args).contains("arg=" + canonical(packageArchive));
        assertThat(args).doesNotContain("arg=--registry");
        assertThat(args).doesNotContain("arg=allure@" + AllureVersion.ALLURE3_DEFAULT_VERSION);
        assertThat(args.get(2)).isEqualTo("arg=" + canonical(
                installDirectory.resolve("allure-" + AllureVersion.ALLURE3_DEFAULT_VERSION)));
    }

    @Test
    void shouldInstallAllure3WithBundledPrivateNode() throws Exception {
        final Path projectDirectory = prepareProject("allure3-install-private-node",
                rootPom("allure3-install-private-node"));
        final Path installDirectory = projectDirectory.resolve(".allure install");
        final Path captureFile =
                projectDirectory.resolve(Path.of("target", "allure3 install args.txt"));

        Allure3SetupHelper.prepareFakeInstallRuntime(installDirectory, captureFile);

        runGoals(projectDirectory, List.of("site"));

        final Allure3Platform platform = Allure3Platform.detect();
        final Path nodeHome =
                platform.getNodeHome(installDirectory, Allure3Commandline.NODE_DEFAULT_VERSION);
        final Path npmCli =
                platform.getNpmCliPath(installDirectory, Allure3Commandline.NODE_DEFAULT_VERSION);
        final Path allureExecutable = platform.getAllureExecutable(installDirectory);

        assertThat(allureCli(installDirectory)).exists();
        assertThat(allureExecutable).exists();
        assertThat(readLines(captureFile)).isEqualTo(List.of("cli=" + canonical(npmCli),
                "arg=--prefix",
                "arg=" + canonical(installDirectory
                        .resolve("allure-" + AllureVersion.ALLURE3_DEFAULT_VERSION)),
                "arg=install", "arg=--no-package-lock", "arg=--no-save", "arg=--ignore-scripts",
                "arg=allure@" + AllureVersion.ALLURE3_DEFAULT_VERSION, "arg=--registry",
                "arg=https://registry.npmjs.org"));
        assertThat(nodeHome).exists();
    }

    @Test
    void shouldUsePluginConfiguredAllure3ConfigPath() throws Exception {
        final Path projectDirectory = prepareProject("allure3-config-path-plugin-property",
                rootPom("allure3-config-path-plugin-property"));
        prepareAllure3ReportRuntime(projectDirectory, ".allure", "allure3 args.txt",
                outputDirectory(projectDirectory), true);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkRegularReportMojoOnly(projectDirectory);
        final JsonNode config = readJson(configPath(projectDirectory));
        assertThat(config.path("plugins").path("plugin-config").path("options").path("enabled")
                .asBoolean()).isTrue();
        assertThat(config.path("plugins").has("root-config")).isFalse();
    }

    @Test
    void shouldUseSystemPropertyConfiguredAllure3ConfigPath() throws Exception {
        final Path projectDirectory = prepareProject("allure3-config-path-system-property",
                rootPom("allure3-config-path-system-property"));
        prepareAllure3ReportRuntime(projectDirectory, ".allure", "allure3 args.txt",
                outputDirectory(projectDirectory), true);

        runGoals(projectDirectory, List.of("site"),
                List.of("-Dallure.config.path=config/system-allure.yml"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        final JsonNode config = readJson(configPath(projectDirectory));
        assertThat(config.path("plugins").path("system-config").path("options").path("enabled")
                .asBoolean()).isTrue();
        assertThat(config.path("plugins").has("root-config")).isFalse();
    }

    @Test
    void shouldUseRootAllureYamlConfigForAllure3Report() throws Exception {
        final Path projectDirectory = prepareProject("feature-root-allure-yaml-config",
                rootPom("feature-root-allure-yaml-config"));
        final Path captureFile = prepareAllure3ReportRuntime(projectDirectory, ".allure",
                "allure3 args.txt", outputDirectory(projectDirectory), true);

        runGoals(projectDirectory, List.of("site"));

        final Path results = projectDirectory.resolve(Path.of("target", "allure-results"));
        final JsonNode config = readJson(configPath(projectDirectory));
        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertThat(readLines(captureFile)).isEqualTo(generateInvocation(
                projectDirectory.resolve(".allure"), results, configPath(projectDirectory)));
        assertThat(config.path("plugins").path("custom").path("enabled").asBoolean()).isTrue();
        assertThat(config.path("plugins").path("awesome").path("options").path("reportLanguage")
                .asText()).isEqualTo("en");
        assertThat(config.path("plugins").path("awesome").path("options").path("singleFile")
                .asBoolean()).isFalse();
    }

    @Test
    void shouldUseDefaultAllure3VersionWhenReportVersionIsOmitted() throws Exception {
        final Path projectDirectory = prepareProject("feature-without-version-property",
                rootPom("feature-without-version-property"));
        final Path captureFile = prepareAllure3ReportRuntime(projectDirectory, ".allure",
                "allure3 args.txt", outputDirectory(projectDirectory), true);

        runGoals(projectDirectory, List.of("site"));

        final Path results = projectDirectory.resolve(Path.of("target", "allure-results"));
        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertThat(readLines(captureFile)).isEqualTo(generateInvocation(
                projectDirectory.resolve(".allure"), results, configPath(projectDirectory)));
    }

    @Test
    void shouldWriteHistoryConfigForAllure3Reports() throws Exception {
        final Path projectDirectory =
                prepareProject("report-history-allure3", rootPom("report-history-allure3"));
        final Path captureFile = prepareAllure3ReportRuntime(projectDirectory, ".allure",
                "allure3 history args.txt", outputDirectory(projectDirectory), true);

        runGoals(projectDirectory, List.of("site"));

        final Path results = projectDirectory.resolve(Path.of("target", "allure-results"));
        final Path historyFile =
                projectDirectory.resolve(Path.of(".allure", "history", "report", "history.jsonl"));
        final JsonNode config = readJson(configPath(projectDirectory));
        TestHelper.checkRegularReportMojoOnly(projectDirectory);
        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertThat(readLines(captureFile)).isEqualTo(generateInvocation(
                projectDirectory.resolve(".allure"), results, configPath(projectDirectory)));
        assertThat(config.path("historyPath").asText()).isEqualTo(
                canonical(historyFile.getParent()).resolve(historyFile.getFileName()).toString());
        assertThat(config.path("appendHistory").asBoolean()).isTrue();
        assertThat(historyFile.getParent()).isDirectory();
    }

    @Test
    void shouldGenerateSingleFileAllure3Reports() throws Exception {
        final Path projectDirectory =
                prepareProject("report-single-file", rootPom("report-single-file"));
        final Path captureFile = prepareAllure3ReportRuntime(projectDirectory, ".allure",
                "allure3 args.txt", outputDirectory(projectDirectory), false);

        runGoals(projectDirectory, List.of("site"));

        final Path results = projectDirectory.resolve(Path.of("target", "allure-results"));
        final List<String> expectedInvocation = generateInvocation(
                projectDirectory.resolve(".allure"), results, configPath(projectDirectory));
        final JsonNode config = readJson(configPath(projectDirectory));
        TestHelper.checkSingleFile(outputDirectory(projectDirectory));
        assertThat(readLines(captureFile))
                .isEqualTo(concatenate(expectedInvocation, expectedInvocation));
        assertThat(config.path("plugins").path("awesome").path("options").path("singleFile")
                .asBoolean()).isTrue();
    }

    @Test
    void shouldServeAllure3ReportWhenPathsContainSpaces() throws Exception {
        final Path projectDirectory = prepareProject("allure3-serve-paths-with-spaces",
                "allure3 serve paths with spaces", rootPom("allure3-serve-paths-with-spaces"));
        final Path captureFile = prepareAllure3ReportRuntime(projectDirectory, ".allure install",
                "allure3 serve args.txt",
                projectDirectory.resolve(Path.of("target", "site", "allure serve report")), false);

        runGoals(projectDirectory, List.of("site"));

        final Path installDirectory = projectDirectory.resolve(".allure install");
        final Path results = projectDirectory.resolve(Path.of("target", "my results"));
        final Path reportDirectory =
                projectDirectory.resolve(Path.of("target", "site", "allure serve report"));
        final Path config = configPath(projectDirectory);

        assertThat(readLines(captureFile))
                .isEqualTo(concatenate(generateInvocation(installDirectory, results, config),
                        openInvocation(installDirectory, reportDirectory, config)));
    }

    private Path prepareAllure3ReportRuntime(final Path projectDirectory,
            final String installDirectoryName, final String captureFileName,
            final Path reportDirectory, final boolean createDataFiles) throws Exception {
        final Path installDirectory = projectDirectory.resolve(installDirectoryName);
        final Path captureFile = projectDirectory.resolve(Path.of("target", captureFileName));
        Allure3SetupHelper.prepareFakeReportRuntime(installDirectory, captureFile, reportDirectory,
                createDataFiles);
        return captureFile;
    }

    private Path outputDirectory(final Path projectDirectory) {
        return projectDirectory.resolve(Path.of("target", "site", "allure-maven-plugin"));
    }

    private Path configPath(final Path projectDirectory) {
        return projectDirectory
                .resolve(Path.of("target", "allure-maven", "allure3", "allurerc.json"));
    }

    private Path allureCli(final Path installDirectory) {
        return installDirectory.resolve(Path.of("allure-" + AllureVersion.ALLURE3_DEFAULT_VERSION,
                "node_modules", "allure", "cli.js"));
    }

    private List<String> generateInvocation(final Path installDirectory, final Path results,
            final Path config) throws Exception {
        return List.of("cli=" + canonical(allureCli(installDirectory)), "command=generate",
                "arg=generate", "arg=" + canonical(results), "arg=--config",
                "arg=" + canonical(config), "---");
    }

    private List<String> openInvocation(final Path installDirectory, final Path reportDirectory,
            final Path config) throws Exception {
        return List.of("cli=" + canonical(allureCli(installDirectory)), "command=open", "arg=open",
                "arg=" + canonical(reportDirectory), "arg=--config", "arg=" + canonical(config),
                "---");
    }

    private List<String> concatenate(final List<String> first, final List<String> second) {
        return java.util.stream.Stream.concat(first.stream(), second.stream()).toList();
    }
}
