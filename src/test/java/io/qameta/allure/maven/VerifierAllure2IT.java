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

    @Test
    void shouldGenerateReportFromAllure2JsonResults() throws Exception {
        final Path projectDirectory = prepareAllure2Project("allure-2-results");
        installAllure2Commandline(projectDirectory, "allure2-report-args.txt",
                Allure2SetupHelper.Mode.FULL, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
    }

    @Test
    void shouldGenerateReportWhenAllure2VersionIsConfigured() throws Exception {
        final Path projectDirectory =
                prepareAllure2Project("allure2-feature-without-version-property");
        installAllure2Commandline(projectDirectory, "allure2-version-args.txt",
                Allure2SetupHelper.Mode.FULL, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
    }

    @Test
    void shouldSupportConfiguredAllurePluginsForAllure2Report() throws Exception {
        final Path projectDirectory = prepareAllure2Project("feature-plugins-support");
        installAllure2Commandline(projectDirectory, "allure2-plugins-args.txt",
                Allure2SetupHelper.Mode.FULL, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
    }

    @Test
    void shouldMergeMultipleInputDirectoriesIntoOneReport() throws Exception {
        final Path projectDirectory = prepareAllure2Project("input-directory-sample");
        installAllure2Commandline(projectDirectory, "allure2-input-directory-args.txt",
                Allure2SetupHelper.Mode.FULL, 2);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 2);
    }

    @Test
    void shouldRespectConfiguredReportDirectory() throws Exception {
        final Path projectDirectory = prepareAllure2Project("report-change-report-directory");
        installAllure2Commandline(projectDirectory, "allure2-report-dir-args.txt",
                Allure2SetupHelper.Mode.FULL, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(projectDirectory.resolve(Path.of("target", "allure")), 1);
    }

    @Test
    void shouldRespectConfiguredResultsDirectory() throws Exception {
        final Path projectDirectory = prepareAllure2Project("report-change-results-directory");
        installAllure2Commandline(projectDirectory, "allure2-results-dir-args.txt",
                Allure2SetupHelper.Mode.FULL, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
    }

    @Test
    void shouldGenerateReportWhenProjectPathContainsSpaces() throws Exception {
        final Path projectDirectory = prepareAllure2Project("report-paths-with-spaces",
                "allure2 report paths with spaces");
        installAllure2Commandline(projectDirectory, "allure2-paths-with-spaces-args.txt",
                Allure2SetupHelper.Mode.FULL, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(
                projectDirectory.resolve(Path.of("target", "site", "allure report")), 1);
    }

    @Test
    void shouldGenerateReportFromBuildPluginAndReportingPlugin() throws Exception {
        final Path projectDirectory = prepareAllure2Project("report-as-build-plugin");
        installAllure2Commandline(projectDirectory, "allure2-build-plugin-args.txt",
                Allure2SetupHelper.Mode.FULL, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(
                projectDirectory.resolve(Path.of("target", "site", "allure")), 1);
        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
    }

    @Test
    void shouldGenerateReportFromHermeticCustomDownloadUrl() throws Exception {
        final Path projectDirectory = prepareProject("custom-url-report",
                "custom-url-report project",
                downloadUrlReplacement("custom-url-report", "allure2-custom-url-args.txt", 1),
                rootPom("custom-url-report"));

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
    }

    @Test
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

    @Test
    void shouldGenerateSingleFileReportForAllure2() throws Exception {
        final Path projectDirectory = prepareAllure2Project("allure2-report-single-file");
        installAllure2Commandline(projectDirectory, "allure2-single-file-args.txt",
                Allure2SetupHelper.Mode.SINGLE_FILE, 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkSingleFile(outputDirectory(projectDirectory));
    }

    @Test
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
        assertThat(readLines(captureFile)).isEqualTo(List.of("command=generate", "arg=generate",
                "arg=--clean", "arg=" + resultsDirectory, "arg=-o", "arg=" + reportDirectory, "---",
                "command=serve", "arg=serve", "arg=" + resultsDirectory, "---"));
        assertThat(reportDirectory.resolve("index.html")).exists();
    }

    @Test
    void shouldPassResultsDirectoryWithSpacesToServe() throws Exception {
        final Path projectDirectory = prepareAllure2Project("allure2-serve-paths-with-spaces",
                "allure2 serve paths with spaces");
        final Path captureFile = installAllure2Commandline(projectDirectory, "captured args.txt",
                Allure2SetupHelper.Mode.ARGS, 1);

        runGoals(projectDirectory, List.of("site"));

        assertThat(readLines(captureFile)).isEqualTo(List.of("serve",
                canonical(projectDirectory.resolve(Path.of("target", "my results"))).toString()));
    }

    @Test
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
