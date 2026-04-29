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

import java.nio.file.Path;
import java.util.List;

@Tag("verifier")
@Tag("properties")
class VerifierPropertiesIT extends VerifierTestSupport {

    private static final String ALLURE2_VERSION = "2.30.0";

    @Test
    void shouldLoadPropertiesFromConfiguredFile() throws Exception {
        final Path projectDirectory = preparePropertiesProject("properties-file-support");

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkRegularReportMojoOnly(projectDirectory);
        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertIssueLink(outputDirectory(projectDirectory));
    }

    @Test
    void shouldLoadPropertiesFromCompileClasspath() throws Exception {
        final Path projectDirectory =
                preparePropertiesProject("properties-file-support-compile-classpath");

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertIssueLink(outputDirectory(projectDirectory));
    }

    @Test
    void shouldLoadPropertiesFromPluginConfiguration() throws Exception {
        final Path projectDirectory =
                preparePropertiesProject("properties-file-support-configuration");

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertIssueLink(outputDirectory(projectDirectory));
    }

    @Test
    void shouldLoadPropertiesFromDefaultReportPropertiesLocation() throws Exception {
        final Path projectDirectory =
                preparePropertiesProject("properties-file-support-default-location");

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertIssueLink(outputDirectory(projectDirectory));
    }

    @Test
    void shouldResolvePlaceholdersInPropertiesFile() throws Exception {
        final Path projectDirectory =
                preparePropertiesProject("properties-file-support-placeholder");

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertIssueLink(outputDirectory(projectDirectory));
    }

    @Test
    void shouldLoadPropertiesFromTestClasspath() throws Exception {
        final Path projectDirectory =
                preparePropertiesProject("properties-file-support-test-classpath");

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 1);
        assertIssueLink(outputDirectory(projectDirectory));
    }

    @Test
    void shouldLoadCategoriesFromTestClasspath() throws Exception {
        final Path projectDirectory =
                preparePropertiesProject("categories-file-support-test-classpath", 2);

        runGoals(projectDirectory, List.of("site"));

        TestHelper.checkReportDirectory(outputDirectory(projectDirectory), 2);
        assertCategoriesChildren(outputDirectory(projectDirectory), 1);
    }

    private Path preparePropertiesProject(final String scenario) throws Exception {
        return preparePropertiesProject(scenario, 1);
    }

    private Path preparePropertiesProject(final String scenario, final int testCases)
            throws Exception {
        final Path projectDirectory = prepareProject(scenario, rootPom(scenario));
        final Path captureFile =
                projectDirectory.resolve(Path.of("target", scenario + "-allure2-args.txt"));
        Allure2SetupHelper.installFakeCommandlineArtifact(absoluteLocalRepository(),
                ALLURE2_VERSION, captureFile, null, Allure2SetupHelper.Mode.FULL, testCases);
        return projectDirectory;
    }

    private Path outputDirectory(final Path projectDirectory) {
        return projectDirectory.resolve(Path.of("target", "site", "allure-maven-plugin"));
    }
}
