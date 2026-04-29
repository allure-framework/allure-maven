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
import io.qameta.allure.Description;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("verifier")
@Tag("allure3")
class VerifierAllure3IT extends VerifierTestSupport {

    /**
     * Project structure: a single-module Maven project with an Allure 3 package archive under
     * {@code packages/custom-allure.tgz} and a private install directory containing a fake npm
     * runtime.
     * <p>
     * Verifies that package-path installation uses the local archive, does not hit the npm
     * registry, and installs the expected Allure CLI files.
     */
    @Test
    @Description
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
        assertCommandLinesContain(args, "arg=" + canonical(packageArchive));
        assertThat(args).doesNotContain("arg=--registry");
        assertThat(args).doesNotContain("arg=allure@" + AllureVersion.ALLURE3_DEFAULT_VERSION);
        final Path expectedInstallDirectory =
                installDirectory.resolve("allure-" + AllureVersion.ALLURE3_DEFAULT_VERSION);
        assertCommandLines(List.of(args.get(2)),
                List.of("arg=" + canonical(expectedInstallDirectory)));
    }

    /**
     * Project structure: a single-module Maven project using the default Allure 3 version and a
     * private Node installation under {@code .allure install}.
     * <p>
     * Verifies that the plugin provisions the bundled Node/npm runtime and invokes npm with the
     * expected prefix, registry, and package arguments.
     */
    @Test
    @Description
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
        assertCommandLines(readLines(captureFile),
                List.of("cli=" + canonical(npmCli), "arg=--prefix",
                        "arg=" + canonical(installDirectory
                                .resolve("allure-" + AllureVersion.ALLURE3_DEFAULT_VERSION)),
                        "arg=install", "arg=--no-package-lock", "arg=--no-save",
                        "arg=--ignore-scripts",
                        "arg=allure@" + AllureVersion.ALLURE3_DEFAULT_VERSION, "arg=--registry",
                        "arg=https://registry.npmjs.org"));
        assertThat(nodeHome).exists();
    }

    /**
     * Project structure: a single-module Maven project with Allure 3 results, default root config,
     * and plugin configuration pointing to {@code config/plugin-allure.yml}.
     * <p>
     * Verifies that the plugin-level config path overrides root discovery and is merged into the
     * generated Allure 3 config.
     */
    @Test
    @Description
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

    /**
     * Project structure: a single-module Maven project with Allure 3 results, default root config,
     * and an alternate config file at {@code config/system-allure.yml}.
     * <p>
     * Verifies that the {@code allure.config.path} system property overrides root config discovery
     * and plugin configuration.
     */
    @Test
    @Description
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

    /**
     * Project structure: a single-module Maven project with Allure 3 results and a root
     * {@code allurerc.yml}.
     * <p>
     * Verifies that root Allure config discovery feeds the generated config and that report
     * generation receives the expected results and config paths.
     */
    @Test
    @Description
    void shouldUseRootAllureYamlConfigForAllure3Report() throws Exception {
        final Path projectDirectory = prepareProject("feature-root-allure-yaml-config",
                rootPom("feature-root-allure-yaml-config"));
        final Path captureFile = prepareAllure3ReportRuntime(projectDirectory, ".allure",
                "allure3 args.txt", outputDirectory(projectDirectory), true);

        runGoals(projectDirectory, List.of("site"));

        final Path results = projectDirectory.resolve(Path.of("target", "allure-results"));
        final JsonNode config = readJson(configPath(projectDirectory));
        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertCommandLines(readLines(captureFile), generateInvocation(
                projectDirectory.resolve(".allure"), results, configPath(projectDirectory)));
        assertThat(config.path("plugins").path("custom").path("enabled").asBoolean()).isTrue();
        assertThat(config.path("plugins").path("awesome").path("options").path("reportLanguage")
                .asText()).isEqualTo("en");
        assertThat(config.path("plugins").path("awesome").path("options").path("singleFile")
                .asBoolean()).isFalse();
    }

    /**
     * Project structure: a single-module Maven project with Allure 3 results and no configured
     * report version.
     * <p>
     * Verifies that the default Allure 3 version is used and that report generation is invoked with
     * the generated config.
     */
    @Test
    @Description
    void shouldUseDefaultAllure3VersionWhenReportVersionIsOmitted() throws Exception {
        final Path projectDirectory = prepareProject("feature-without-version-property",
                rootPom("feature-without-version-property"));
        final Path captureFile = prepareAllure3ReportRuntime(projectDirectory, ".allure",
                "allure3 args.txt", outputDirectory(projectDirectory), true);

        runGoals(projectDirectory, List.of("site"));

        final Path results = projectDirectory.resolve(Path.of("target", "allure-results"));
        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertCommandLines(readLines(captureFile), generateInvocation(
                projectDirectory.resolve(".allure"), results, configPath(projectDirectory)));
    }

    /**
     * Project structure: a single-module Maven project with Allure 3 results and report history
     * enabled.
     * <p>
     * Verifies that the generated config contains append-history settings and that the history
     * cache directory is prepared for the report.
     */
    @Test
    @Description
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
        assertCommandLines(readLines(captureFile), generateInvocation(
                projectDirectory.resolve(".allure"), results, configPath(projectDirectory)));
        final Path historyPath = Path.of(config.path("historyPath").asText());
        assertThat(historyPath.getFileName()).isEqualTo(historyFile.getFileName());
        assertSamePath(historyPath.getParent().toString(), historyFile.getParent());
        assertThat(config.path("appendHistory").asBoolean()).isTrue();
        assertThat(historyFile.getParent()).isDirectory();
    }

    /**
     * Project structure: a single-module Maven project with Allure 3 results and single-file report
     * mode enabled.
     * <p>
     * Verifies that single-file mode is written into the generated config and that both generation
     * calls target the same report output.
     */
    @Test
    @Description
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
        assertCommandLines(readLines(captureFile),
                concatenate(expectedInvocation, expectedInvocation));
        assertThat(config.path("plugins").path("awesome").path("options").path("singleFile")
                .asBoolean()).isTrue();
    }

    /**
     * Project structure: a single-module Maven project created in a path with spaces, with results
     * under {@code target/my results} and report output under
     * {@code target/site/allure serve report}.
     * <p>
     * Verifies that Allure 3 serve first generates the report and then opens the configured report
     * directory while preserving paths with spaces.
     */
    @Test
    @Description
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

        assertCommandLines(readLines(captureFile),
                concatenate(generateInvocation(installDirectory, results, config),
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
