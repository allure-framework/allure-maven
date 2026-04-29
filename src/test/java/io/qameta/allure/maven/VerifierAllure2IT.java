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

import io.qameta.allure.Description;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("verifier")
@Tag("allure2")
class VerifierAllure2IT extends VerifierTestSupport {

    private static final String ALLURE2_VERSION = "2.30.0";

    /**
     * Project structure: a single-module Maven project with Allure 2 JSON results in
     * {@code target/allure-results}.
     * <p>
     * Verifies that the {@code site} lifecycle runs the report goal, invokes the fake Allure 2
     * commandline, and creates a report with one test case.
     */
    @Test
    @Description
    void shouldGenerateReportFromAllure2JsonResults() throws Exception {
        final Path projectDirectory = prepareAllure2Project("allure-2-results");
        installAllure2Commandline(projectDirectory, "allure2-report-args.txt",
                Allure2SetupHelper.Mode.FULL, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
    }

    /**
     * Project structure: a single-module Maven project with standard Allure 2 XML results in
     * {@code target/allure-results} and an explicit {@code report.version}.
     * <p>
     * Verifies that the configured Allure 2 version is used to generate a report during the
     * {@code site} lifecycle.
     */
    @Test
    @Description
    void shouldGenerateReportWhenAllure2VersionIsConfigured() throws Exception {
        final Path projectDirectory =
                prepareAllure2Project("allure2-feature-without-version-property");
        installAllure2Commandline(projectDirectory, "allure2-version-args.txt",
                Allure2SetupHelper.Mode.FULL, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
    }

    /**
     * Project structure: a single-module Maven project with Allure 2 results and plugin
     * configuration for additional Allure commandline plugins.
     * <p>
     * Verifies that plugin configuration does not prevent report generation and that the generated
     * report contains one test case.
     */
    @Test
    @Description
    void shouldSupportConfiguredAllurePluginsForAllure2Report() throws Exception {
        final Path projectDirectory = prepareAllure2Project("feature-plugins-support");
        installAllure2Commandline(projectDirectory, "allure2-plugins-args.txt",
                Allure2SetupHelper.Mode.FULL, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
    }

    /**
     * Project structure: a single-module Maven project with two configured input directories,
     * {@code first} and {@code second}, each containing one Allure 2 result file.
     * <p>
     * Verifies that multiple input directories are passed to Allure and merged into a single report
     * with two test cases.
     */
    @Test
    @Description
    void shouldMergeMultipleInputDirectoriesIntoOneReport() throws Exception {
        final Path projectDirectory = prepareAllure2Project("input-directory-sample");
        installAllure2Commandline(projectDirectory, "allure2-input-directory-args.txt",
                Allure2SetupHelper.Mode.FULL, 2);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 2);
    }

    /**
     * Project structure: a single-module Maven project with Allure 2 results and a custom
     * {@code reportDirectory} pointing to {@code target/allure}.
     * <p>
     * Verifies that the report goal respects the configured output directory instead of writing to
     * the default Maven site location.
     */
    @Test
    @Description
    void shouldRespectConfiguredReportDirectory() throws Exception {
        final Path projectDirectory = prepareAllure2Project("report-change-report-directory");
        installAllure2Commandline(projectDirectory, "allure2-report-dir-args.txt",
                Allure2SetupHelper.Mode.FULL, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(projectDirectory.resolve(Path.of("target", "allure")), 1);
    }

    /**
     * Project structure: a single-module Maven project with Allure 2 results stored under a custom
     * {@code target/my-results} directory.
     * <p>
     * Verifies that the report goal reads the configured results directory and still produces the
     * default plugin report.
     */
    @Test
    @Description
    void shouldRespectConfiguredResultsDirectory() throws Exception {
        final Path projectDirectory = prepareAllure2Project("report-change-results-directory");
        installAllure2Commandline(projectDirectory, "allure2-results-dir-args.txt",
                Allure2SetupHelper.Mode.FULL, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
    }

    /**
     * Project structure: a single-module Maven project created in a directory whose path contains
     * spaces, with results under {@code target/my results}.
     * <p>
     * Verifies that paths with spaces are preserved when Allure 2 report generation reads custom
     * results and writes to a custom report directory.
     */
    @Test
    @Description
    void shouldGenerateReportWhenProjectPathContainsSpaces() throws Exception {
        final Path projectDirectory = prepareAllure2Project("report-paths-with-spaces",
                "allure2 report paths with spaces");
        installAllure2Commandline(projectDirectory, "allure2-paths-with-spaces-args.txt",
                Allure2SetupHelper.Mode.FULL, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(
                projectDirectory.resolve(Path.of("target", "site", "allure report")), 1);
    }

    /**
     * Project structure: a single-module Maven project where the plugin is configured both as a
     * build plugin and as a reporting plugin.
     * <p>
     * Verifies that both configured executions can generate reports without suppressing the normal
     * Maven site report.
     */
    @Test
    @Description
    void shouldGenerateReportFromBuildPluginAndReportingPlugin() throws Exception {
        final Path projectDirectory = prepareAllure2Project("report-as-build-plugin");
        installAllure2Commandline(projectDirectory, "allure2-build-plugin-args.txt",
                Allure2SetupHelper.Mode.FULL, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(
                projectDirectory.resolve(Path.of("target", "site", "allure")), 1);
        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
    }

    /**
     * Project structure: a single-module Maven project with Allure 2 results and a custom
     * commandline download URL replaced with a local fake archive.
     * <p>
     * Verifies that report generation can install Allure 2 from a hermetic custom URL and produce
     * the expected report.
     */
    @Test
    @Description
    void shouldGenerateReportFromHermeticCustomDownloadUrl() throws Exception {
        final Path projectDirectory = prepareProject("custom-url-report",
                "custom-url-report project",
                downloadUrlReplacement("custom-url-report", "allure2-custom-url-args.txt", 1),
                rootPom("custom-url-report"));

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
    }

    /**
     * Project structure: a single-module Maven project using the bundled report configuration
     * alongside the normal reporting plugin output.
     * <p>
     * Verifies that the bundled report and the Maven site report are both produced when an Allure 2
     * version is configured.
     */
    @Test
    @Description
    void shouldGenerateBundledAndReportingReportsTogether() throws Exception {
        final Path projectDirectory = prepareAllure2Project("report-with-bundled-version");
        installAllure2Commandline(projectDirectory, "allure2-bundled-version-args.txt",
                Allure2SetupHelper.Mode.FULL, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertThat(
                projectDirectory.resolve(Path.of("target", "site", "allure", "allure-maven.html")))
                .exists();
    }

    /**
     * Project structure: a single-module Maven project with Allure 2 results and single-file report
     * mode enabled.
     * <p>
     * Verifies that Allure 2 single-file generation writes only the report entry point and does not
     * create the normal report data directory.
     */
    @Test
    @Description
    void shouldGenerateSingleFileReportForAllure2() throws Exception {
        final Path projectDirectory = prepareAllure2Project("allure2-report-single-file");
        installAllure2Commandline(projectDirectory, "allure2-single-file-args.txt",
                Allure2SetupHelper.Mode.SINGLE_FILE, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkSingleFile(outputDirectory(projectDirectory));
    }

    /**
     * Project structure: a single-module Maven project with Allure 2 results and a configured
     * report directory used by an explicit report-then-serve invocation.
     * <p>
     * Verifies that running {@code allure:report} followed by {@code allure:serve} preserves the
     * configured report directory and passes the original results directory to serve.
     */
    @Test
    @Description
    void shouldPreserveConfiguredReportDirectoryWhenServingAfterReport() throws Exception {
        final Path projectDirectory =
                prepareAllure2Project("allure2-report-then-serve-preserves-report-directory");
        final Path captureFile = installAllure2Commandline(projectDirectory,
                "captured commands.txt", Allure2SetupHelper.Mode.COMMANDS, 1);

        runGoals(projectDirectory, List.of(pluginGoal("report"), pluginGoal("serve")));

        final Path resultsDirectory =
                canonical(projectDirectory.resolve(Path.of("target", "allure-results")));
        final Path reportDirectory =
                canonical(projectDirectory.resolve(Path.of("target", "site", "preserved-report")));
        assertCommandLines(readLines(captureFile),
                List.of("command=generate", "arg=generate", "arg=--clean",
                        "arg=" + resultsDirectory, "arg=-o", "arg=" + reportDirectory, "---",
                        "command=serve", "arg=serve", "arg=" + resultsDirectory, "---"));
        assertThat(reportDirectory.resolve("index.html")).exists();
    }

    /**
     * Project structure: a single-module Maven project created in a path with spaces and configured
     * with a results directory named {@code my results}.
     * <p>
     * Verifies that the Allure 2 serve goal receives the intended results directory with spaces
     * intact, regardless of platform-specific long or short path spelling.
     */
    @Test
    @Description
    void shouldPassResultsDirectoryWithSpacesToServe() throws Exception {
        final Path projectDirectory = prepareAllure2Project("allure2-serve-paths-with-spaces",
                "allure2 serve paths with spaces");
        final Path captureFile = installAllure2Commandline(projectDirectory, "captured args.txt",
                Allure2SetupHelper.Mode.ARGS, 1);

        runGoals(projectDirectory, List.of("site"));

        final List<String> args = readLines(captureFile);
        assertThat(args).hasSize(2);
        assertThat(args.get(0)).isEqualTo("serve");
        assertThat(args.get(1)).contains("my results");

        assertSamePath(args.get(1), projectDirectory.resolve(Path.of("target", "my results")));
    }

    /**
     * Project structure: a single-module Maven project with an empty {@code target/allure-results}
     * directory.
     * <p>
     * Verifies that an empty result set is skipped gracefully and the Maven build still succeeds.
     */
    @Test
    @Description
    void shouldSkipAllureReportGracefullyWhenResultsAreEmpty() throws Exception {
        final Path projectDirectory = prepareAllure2Project("feature-should-fail-if-empty-report");

        runGoals(projectDirectory, List.of("site"));

        assertThat(Files.readString(projectDirectory.resolve("build.log"), StandardCharsets.UTF_8))
                .contains("BUILD SUCCESS");
    }

    private Path prepareAllure2Project(final String scenario) throws Exception {
        return prepareProject(scenario, rootPom(scenario));
    }

    private Path prepareAllure2Project(final String scenario, final String directoryName)
            throws Exception {
        return prepareProject(scenario, directoryName, rootPom(scenario));
    }

    private Path installAllure2Commandline(final Path projectDirectory,
            final String captureFileName, final Allure2SetupHelper.Mode mode, final int testCases)
            throws Exception {
        final Path captureFile = projectDirectory.resolve(Path.of("target", captureFileName));
        Allure2SetupHelper.installFakeCommandlineArtifact(absoluteLocalRepository(),
                ALLURE2_VERSION, captureFile, null, mode, testCases);
        return captureFile;
    }

    private Path outputDirectory(final Path projectDirectory) {
        return projectDirectory.resolve(Path.of("target", "site", "allure-maven-plugin"));
    }

    private java.util.Map<String, String> downloadUrlReplacement(final String scenario,
            final String captureFileName, final int testCases) throws Exception {
        final Path downloadDirectory = tempDir.resolve("downloads").resolve(scenario);
        final Path captureFile =
                tempDir.resolve("downloads").resolve("captures").resolve(captureFileName);
        final Path archive = Allure2SetupHelper.createFakeCommandlineArchive(downloadDirectory,
                ALLURE2_VERSION, captureFile, null, Allure2SetupHelper.Mode.FULL, testCases);
        return java.util.Map.of("@allureDownloadUrl@", archive.toUri().toString());
    }
}
