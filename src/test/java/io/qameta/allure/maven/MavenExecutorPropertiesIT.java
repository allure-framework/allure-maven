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

@Tag("maven-executor")
@Tag("properties")
class MavenExecutorPropertiesIT extends MavenTestSupport {

    private static final String ALLURE2_VERSION = "2.30.0";

    /**
     * Project structure: a single-module Maven project with Allure 2 results in
     * {@code target/allure-results} and an {@code allure.properties} file in the project root.
     * <p>
     * Verifies that a configured properties file is loaded and turns the issue label in the result
     * into an issue link in the generated report.
     */
    @Test
    @Description
    void shouldLoadPropertiesFromConfiguredFile() throws Exception {
        final Path projectDirectory = prepareProject(
                "properties-file-support",
                resource("/maven-executor/properties-file-support-pom.xml", "pom.xml"),
                resource("/maven-executor-data/allure2-properties-result.xml", "target/allure-results/sample-testsuite.xml"),
                resource("/maven-executor-data/report.properties", "allure.properties")
        );
        installAllure2Commandline(
                projectDirectory, "properties-file-support-allure2-args.txt", 1
        );

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkRegularReportMojoOnly(projectDirectory);
        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertIssueLink(outputDirectory(projectDirectory));
    }

    /**
     * Project structure: a single-module Maven project with Allure 2 results and
     * {@code allure.properties} available on the compile classpath under {@code target/classes}.
     * <p>
     * Verifies that compile classpath properties are discovered and applied to issue link
     * generation.
     */
    @Test
    @Description
    void shouldLoadPropertiesFromCompileClasspath() throws Exception {
        final Path projectDirectory = prepareProject(
                "properties-file-support-compile-classpath",
                resource("/maven-executor/properties-file-support-compile-classpath-pom.xml", "pom.xml"),
                resource("/maven-executor-data/allure2-properties-result.xml", "target/allure-results/sample-testsuite.xml"),
                resource("/maven-executor-data/report.properties", "target/classes/allure.properties")
        );
        installAllure2Commandline(
                projectDirectory, "properties-file-support-compile-classpath-allure2-args.txt", 1
        );

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertIssueLink(outputDirectory(projectDirectory));
    }

    /**
     * Project structure: a single-module Maven project with Allure 2 results and issue-link
     * patterns provided directly in plugin configuration.
     * <p>
     * Verifies that plugin configuration properties are used when generating report links.
     */
    @Test
    @Description
    void shouldLoadPropertiesFromPluginConfiguration() throws Exception {
        final Path projectDirectory = prepareProject(
                "properties-file-support-configuration",
                resource("/maven-executor/properties-file-support-configuration-pom.xml", "pom.xml"),
                resource("/maven-executor-data/allure2-properties-result.xml", "target/allure-results/sample-testsuite.xml")
        );
        installAllure2Commandline(
                projectDirectory, "properties-file-support-configuration-allure2-args.txt", 1
        );

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertIssueLink(outputDirectory(projectDirectory));
    }

    /**
     * Project structure: a single-module Maven project with Allure 2 results and a default
     * {@code report.properties} file in the project root.
     * <p>
     * Verifies that the default report properties location is discovered and used for issue link
     * expansion.
     */
    @Test
    @Description
    void shouldLoadPropertiesFromDefaultReportPropertiesLocation() throws Exception {
        final Path projectDirectory = prepareProject(
                "properties-file-support-default-location",
                resource("/maven-executor/properties-file-support-default-location-pom.xml", "pom.xml"),
                resource("/maven-executor-data/allure2-properties-result.xml", "target/allure-results/sample-testsuite.xml"),
                resource("/maven-executor-data/report.properties", "report.properties")
        );
        installAllure2Commandline(
                projectDirectory, "properties-file-support-default-location-allure2-args.txt", 1
        );

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertIssueLink(outputDirectory(projectDirectory));
    }

    /**
     * Project structure: a single-module Maven project with Allure 2 results and an
     * {@code allure.properties} file containing Maven-style placeholders.
     * <p>
     * Verifies that placeholders in properties are resolved before issue links are generated.
     */
    @Test
    @Description
    void shouldResolvePlaceholdersInPropertiesFile() throws Exception {
        final Path projectDirectory = prepareProject(
                "properties-file-support-placeholder",
                resource("/maven-executor/properties-file-support-placeholder-pom.xml", "pom.xml"),
                resource("/maven-executor-data/allure2-properties-result.xml", "target/allure-results/sample-testsuite.xml"),
                resource("/maven-executor-data/report-placeholder.properties", "allure.properties")
        );
        installAllure2Commandline(
                projectDirectory, "properties-file-support-placeholder-allure2-args.txt", 1
        );

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertIssueLink(outputDirectory(projectDirectory));
    }

    /**
     * Project structure: a single-module Maven project with Allure 2 results and
     * {@code report.properties} available on the test classpath under {@code target/test-classes}.
     * <p>
     * Verifies that test classpath properties are discovered and used for issue link generation.
     */
    @Test
    @Description
    void shouldLoadPropertiesFromTestClasspath() throws Exception {
        final Path projectDirectory = prepareProject(
                "properties-file-support-test-classpath",
                resource("/maven-executor/properties-file-support-test-classpath-pom.xml", "pom.xml"),
                resource("/maven-executor-data/allure2-properties-result.xml", "target/allure-results/sample-testsuite.xml"),
                resource("/maven-executor-data/report.properties", "target/test-classes/report.properties")
        );
        installAllure2Commandline(
                projectDirectory, "properties-file-support-test-classpath-allure2-args.txt", 1
        );

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertIssueLink(outputDirectory(projectDirectory));
    }

    /**
     * Project structure: a single-module Maven project with two Allure 2 result cases and a
     * {@code categories.json} file on the test classpath.
     * <p>
     * Verifies that classpath categories are included in the generated report and group the
     * expected failed test.
     */
    @Test
    @Description
    void shouldLoadCategoriesFromTestClasspath() throws Exception {
        final Path projectDirectory = prepareProject(
                "categories-file-support-test-classpath",
                resource("/maven-executor/categories-file-support-test-classpath-pom.xml", "pom.xml"),
                resource("/maven-executor-data/allure2-categories-result.xml", "target/allure-results/sample-testsuite.xml"),
                resource("/maven-executor-data/categories.json", "target/test-classes/categories.json")
        );
        installAllure2Commandline(
                projectDirectory, "categories-file-support-test-classpath-allure2-args.txt", 2
        );

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 2);
        assertCategoriesChildren(outputDirectory(projectDirectory), 1);
    }

    private void installAllure2Commandline(final Path projectDirectory,
                                           final String captureFileName, final int testCases)
            throws Exception {
        final Path captureFile = projectDirectory.resolve(Path.of("target", captureFileName));
        Allure2SetupHelper.installFakeCommandlineArtifact(
                absoluteLocalRepository(),
                ALLURE2_VERSION, captureFile, null, Allure2SetupHelper.Mode.FULL, testCases
        );
    }

    private Path outputDirectory(final Path projectDirectory) {
        return projectDirectory.resolve(Path.of("target", "site", "allure-maven-plugin"));
    }
}
