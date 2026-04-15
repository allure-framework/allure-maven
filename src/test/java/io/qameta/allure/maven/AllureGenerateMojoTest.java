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
import org.apache.maven.reporting.MavenReportException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class AllureGenerateMojoTest {

    @Test
    public void shouldDetectAllureRcYmlInProjectRoot() throws Exception {
        final Path projectDirectory = Files.createTempDirectory("allure3-project-config");
        try {
            Files.write(projectDirectory.resolve("allurerc.yml"),
                    Collections.singletonList("plugins: {}"), StandardCharsets.UTF_8);

            final TestGenerateMojo mojo = new TestGenerateMojo();
            mojo.projectDirectory = projectDirectory.toString();

            assertThat(mojo.resolveAllure3ConfigPath(),
                    is(projectDirectory.resolve("allurerc.yml")));
        } finally {
            FileUtils.deleteQuietly(projectDirectory.toFile());
        }
    }

    @Test
    public void shouldPreferAllureRcJsBeforeYaml() throws Exception {
        final Path projectDirectory = Files.createTempDirectory("allure3-project-config");
        try {
            Files.write(projectDirectory.resolve("allurerc.js"),
                    Collections.singletonList("export default {};"), StandardCharsets.UTF_8);
            Files.write(projectDirectory.resolve("allurerc.yml"),
                    Collections.singletonList("plugins: {}"), StandardCharsets.UTF_8);

            final TestGenerateMojo mojo = new TestGenerateMojo();
            mojo.projectDirectory = projectDirectory.toString();

            assertThat(mojo.resolveAllure3ConfigPath(),
                    is(projectDirectory.resolve("allurerc.js")));
        } finally {
            FileUtils.deleteQuietly(projectDirectory.toFile());
        }
    }

    @Test
    public void shouldResolveExplicitRelativeAllureConfigPath() throws Exception {
        final Path projectDirectory = Files.createTempDirectory("allure3-project-config");
        try {
            final Path configured = projectDirectory.resolve("config").resolve("custom.yml");
            Files.createDirectories(configured.getParent());
            Files.write(configured, Collections.singletonList("plugins: {}"),
                    StandardCharsets.UTF_8);

            final TestGenerateMojo mojo = new TestGenerateMojo();
            mojo.projectDirectory = projectDirectory.toString();
            mojo.configPath = "config/custom.yml";

            assertThat(mojo.resolveAllure3ConfigPath(), is(configured));
        } finally {
            FileUtils.deleteQuietly(projectDirectory.toFile());
        }
    }

    @Test
    public void shouldReturnNullWhenNoAutoDetectedConfigExists() throws Exception {
        final Path projectDirectory = Files.createTempDirectory("allure3-project-config");
        try {
            final TestGenerateMojo mojo = new TestGenerateMojo();
            mojo.projectDirectory = projectDirectory.toString();

            assertThat(mojo.resolveAllure3ConfigPath(), is(nullValue()));
        } finally {
            FileUtils.deleteQuietly(projectDirectory.toFile());
        }
    }

    @Test
    public void shouldResolveExplicitRelativeAllurePackagePath() throws Exception {
        final Path projectDirectory = Files.createTempDirectory("allure3-project-package");
        try {
            final Path configured =
                    projectDirectory.resolve("packages").resolve("custom-allure.tgz");
            Files.createDirectories(configured.getParent());
            Files.write(configured, Collections.singletonList("fake"), StandardCharsets.UTF_8);

            final TestGenerateMojo mojo = new TestGenerateMojo();
            mojo.projectDirectory = projectDirectory.toString();
            mojo.packagePath = "packages/custom-allure.tgz";

            assertThat(mojo.resolveAllurePackagePath(), is(configured));
        } finally {
            FileUtils.deleteQuietly(projectDirectory.toFile());
        }
    }

    @Test
    public void shouldRejectExplicitConfigPathForAllure2() throws Exception {
        final TestGenerateMojo mojo = new TestGenerateMojo();
        mojo.configPath = "config/custom.yml";

        try {
            mojo.validateConfiguredParameters(AllureVersion.resolve("2.30.0"));
        } catch (MavenReportException e) {
            assertThat(e.getMessage(), containsString("only supported for Allure 3"));
            return;
        }

        throw new AssertionError("Expected Allure 2 config path rejection");
    }

    @Test
    public void shouldRejectExplicitPackagePathForAllure2() throws Exception {
        final TestGenerateMojo mojo = new TestGenerateMojo();
        mojo.packagePath = "packages/custom-allure.tgz";

        try {
            mojo.validateConfiguredParameters(AllureVersion.resolve("2.30.0"));
        } catch (MavenReportException e) {
            assertThat(e.getMessage(), containsString("allure.package.path"));
            return;
        }

        throw new AssertionError("Expected Allure 2 package path rejection");
    }

    private static final class TestGenerateMojo extends AllureGenerateMojo {

        @Override
        protected List<Path> getInputDirectories() {
            return Collections.emptyList();
        }

        @Override
        protected void generateReport(final List<Path> resultsPaths,
                final AllureVersion allureVersion) throws MavenReportException {}

        @Override
        protected String getMojoName() {
            return "report";
        }
    }
}
