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

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("verifier")
@Tag("aggregate")
class VerifierAggregateIT extends VerifierTestSupport {

    private static final String ALLURE2_VERSION = "2.30.0";

    /**
     * Project structure: a parent Maven project with {@code first} and {@code second} child
     * modules, each containing Allure 2 results under {@code target/allure-results}.
     * <p>
     * Verifies that the site lifecycle runs only the aggregate mojo and produces a combined report
     * with both module test cases.
     */
    @Test
    @Description
    void shouldAggregateMultiModuleAllure2Results() throws Exception {
        final Path projectDirectory = prepareAllure2MultiModuleProject("aggregate-multi-module");
        installAllure2Commandline(projectDirectory, "aggregate-allure2-args.txt", 2);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkAggregateMojoOnly(projectDirectory);
        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 2);
    }

    /**
     * Project structure: a parent Maven project with two child modules containing Allure 2 result
     * files.
     * <p>
     * Verifies that a direct {@code allure:aggregate} invocation, with the report version supplied
     * on the command line, produces a two-case aggregate report.
     */
    @Test
    @Description
    void shouldAggregateMultiModuleAllure2ResultsFromDirectGoal() throws Exception {
        final Path projectDirectory =
                prepareAllure2MultiModuleProject("aggregate-multi-module-cli");
        installAllure2Commandline(projectDirectory, "aggregate-cli-allure2-args.txt", 2);

        runGoals(projectDirectory, List.of(pluginGoal("aggregate")),
                List.of("-Dreport.version=" + ALLURE2_VERSION));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 2);
    }

    /**
     * Project structure: a parent Maven project with two child modules and aggregate report
     * configuration that excludes regular per-module report generation.
     * <p>
     * Verifies that the aggregate report is generated while regular report mojo output remains
     * suppressed.
     */
    @Test
    @Description
    void shouldSkipRegularReportWhenAggregateReportIsConfigured() throws Exception {
        final Path projectDirectory =
                prepareAllure2MultiModuleProject("aggregate-multi-module-exclude-report");
        installAllure2Commandline(projectDirectory, "aggregate-exclude-report-args.txt", 2);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkAggregateMojoOnly(projectDirectory);
        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 2);
    }

    /**
     * Project structure: a parent Maven project with two child modules, each contributing Allure 2
     * results that can receive executor metadata.
     * <p>
     * Verifies that aggregate generation preserves each child module's executor file while
     * producing the combined report.
     */
    @Test
    @Description
    void shouldPreserveChildExecutorFilesWhenAggregating() throws Exception {
        final Path projectDirectory =
                prepareAllure2MultiModuleProject("aggregate-multi-module-preserve-child-executor");
        installAllure2Commandline(projectDirectory, "aggregate-preserve-executor-args.txt", 2);

        runGoals(projectDirectory, List.of("site"));

        final Path firstExecutor = projectDirectory
                .resolve(Path.of("first", "target", "allure-results", "executor.json"));
        final Path secondExecutor = projectDirectory
                .resolve(Path.of("second", "target", "allure-results", "executor.json"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 2);
        assertThat(firstExecutor).exists();
        assertThat(secondExecutor).exists();
        assertThat(readJson(firstExecutor).path("buildName").asText())
                .isEqualTo("Allure Report Test First Child");
        assertThat(readJson(secondExecutor).path("buildName").asText())
                .isEqualTo("Allure Report Test Second Child");
    }

    /**
     * Project structure: a single-module Maven project configured for aggregate report generation
     * with one Allure 2 result file.
     * <p>
     * Verifies that aggregate mode works for a non-reactor project and does not run the regular
     * report mojo.
     */
    @Test
    @Description
    void shouldAggregateSingleModuleAllure2Results() throws Exception {
        final Path projectDirectory =
                prepareProject("aggregate-sample", rootPom("aggregate-sample"));
        installAllure2Commandline(projectDirectory, "aggregate-sample-allure2-args.txt", 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkAggregateMojoOnly(projectDirectory);
        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
    }

    /**
     * Project structure: a single-module Maven project configured for Allure 3 aggregate generation
     * with results under {@code target/allure-results}.
     * <p>
     * Verifies that the Allure 3 runtime is invoked with the generated config and produces an
     * aggregate report.
     */
    @Test
    @Description
    void shouldAggregateSingleModuleAllure3Results() throws Exception {
        final Path projectDirectory =
                prepareProject("allure3-aggregate-sample", rootPom("allure3-aggregate-sample"));
        final Path captureFile =
                prepareAllure3ReportRuntime(projectDirectory, "allure3 aggregate args.txt");

        runGoals(projectDirectory, List.of("site"));

        final Path results = projectDirectory.resolve(Path.of("target", "allure-results"));
        TestHelper.checkAggregateMojoOnly(projectDirectory);
        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertThat(readLines(captureFile)).isEqualTo(
                List.of("cli=" + canonical(allureCli(projectDirectory.resolve(".allure"))),
                        "command=generate", "arg=generate", "arg=" + canonical(results),
                        "arg=--config", "arg=" + canonical(configPath(projectDirectory)), "---"));
    }

    /**
     * Project structure: a parent Maven project with two Allure 3 child modules; one module uses
     * {@code target/module-results}, and the other uses {@code target/override-results}.
     * <p>
     * Verifies that aggregate generation reads each configured module results directory and passes
     * both paths to Allure 3.
     */
    @Test
    @Description
    void shouldAggregateAllure3ResultsFromConfiguredModuleDirectories() throws Exception {
        final Path projectDirectory =
                prepareProject("allure3-aggregate-multi-module-results-directory",
                        rootPom("allure3-aggregate-multi-module-results-directory"),
                        modulePom("allure3-aggregate-multi-module-results-directory", "first"),
                        modulePom("allure3-aggregate-multi-module-results-directory", "second"));
        final Path captureFile =
                prepareAllure3ReportRuntime(projectDirectory, "allure3 aggregate results args.txt");

        runGoals(projectDirectory, List.of("site"));

        final Path firstResults =
                projectDirectory.resolve(Path.of("first", "target", "module-results"));
        final Path secondResults =
                projectDirectory.resolve(Path.of("second", "target", "override-results"));
        TestHelper.checkAggregateMojoOnly(projectDirectory);
        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertThat(readLines(captureFile)).isEqualTo(
                List.of("cli=" + canonical(allureCli(projectDirectory.resolve(".allure"))),
                        "command=generate", "arg=generate", "arg=" + canonical(firstResults),
                        "arg=" + canonical(secondResults), "arg=--config",
                        "arg=" + canonical(configPath(projectDirectory)), "---"));
    }

    /**
     * Project structure: a parent Maven project with {@code first} and {@code second} child
     * modules, each running regular Allure 2 report generation.
     * <p>
     * Verifies that normal multi-module site generation writes separate reports for each child
     * module instead of aggregating them.
     */
    @Test
    @Description
    void shouldGeneratePerModuleReportsForRegularMultiModuleBuilds() throws Exception {
        final Path projectDirectory = prepareProject("report-multi-module",
                rootPom("report-multi-module"), modulePom("report-multi-module", "first"),
                modulePom("report-multi-module", "second"));
        installAllure2Commandline(projectDirectory, "report-multi-module-allure2-args.txt", 1);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(
                projectDirectory.resolve(Path.of("first", "target", "site", "allure-maven-plugin")),
                1);
        TestHelper.checkReportDirectory(projectDirectory
                .resolve(Path.of("second", "target", "site", "allure-maven-plugin")), 1);
    }

    private Path prepareAllure2MultiModuleProject(final String scenario) throws Exception {
        return prepareProject(scenario, rootPom(scenario), modulePom(scenario, "first"),
                modulePom(scenario, "second"));
    }

    private void installAllure2Commandline(final Path projectDirectory,
            final String captureFileName, final int testCases) throws Exception {
        final Path captureFile = projectDirectory.resolve(Path.of("target", captureFileName));
        Allure2SetupHelper.installFakeCommandlineArtifact(absoluteLocalRepository(),
                ALLURE2_VERSION, captureFile, null, Allure2SetupHelper.Mode.FULL, testCases);
    }

    private Path prepareAllure3ReportRuntime(final Path projectDirectory,
            final String captureFileName) throws Exception {
        final Path captureFile = projectDirectory.resolve(Path.of("target", captureFileName));
        Allure3SetupHelper.prepareFakeReportRuntime(projectDirectory.resolve(".allure"),
                captureFile, outputDirectory(projectDirectory), true);
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
}
