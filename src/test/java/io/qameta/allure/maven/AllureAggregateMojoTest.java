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
import org.apache.maven.model.Build;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@Tag("aggregate")
class AllureAggregateMojoTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void shouldPreferInjectedReactorProjectsOverSessionProjects() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-aggregate-injected");
        try {
            final MavenProject injectedProject = createProject(workspace, "injected", "Injected");
            final MavenProject sessionProject = createProject(workspace, "session", "Session");

            final TestAggregateMojo mojo =
                    new TestAggregateMojo(Collections.singletonList(sessionProject), null);
            mojo.setReactorProjects(Collections.singletonList(injectedProject));

            assertThat(mojo.getInputDirectories())
                    .containsExactly(resultsDirectory(injectedProject));
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    void shouldUseSessionProjectsWhenInjectedProjectsAreMissing() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-aggregate-session");
        try {
            final MavenProject sessionProject = createProject(workspace, "session", "Session");

            final TestAggregateMojo mojo =
                    new TestAggregateMojo(Collections.singletonList(sessionProject), null);

            assertThat(mojo.getInputDirectories())
                    .containsExactly(resultsDirectory(sessionProject));
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    void shouldFallbackToCurrentProjectWhenReactorProjectsAreMissing() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-aggregate-project");
        try {
            final MavenProject currentProject = createProject(workspace, "current", "Current");

            final TestAggregateMojo mojo =
                    new TestAggregateMojo(Collections.<MavenProject>emptyList(), currentProject);

            assertThat(mojo.getInputDirectories())
                    .containsExactly(resultsDirectory(currentProject));
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    void shouldWarnAndReturnEmptyListWhenNoProjectsAreAvailable() {
        final RecordingLog log = new RecordingLog();
        final TestAggregateMojo mojo =
                new TestAggregateMojo(Collections.<MavenProject>emptyList(), null);
        mojo.setLog(log);

        assertThat(mojo.getInputDirectories()).isEmpty();
        assertThat(log.warnMessages)
                .containsExactly("Reactor projects were not resolved for aggregate goal.");
    }

    @Test
    void shouldUseChildResultsDirectoryPropertyOverrideWhenPresent() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-aggregate-property-override");
        try {
            final MavenProject project =
                    createProject(workspace, "child", "Child", "child-results");
            project.getProperties().setProperty("allure.results.directory", "child-results");

            final TestAggregateMojo mojo =
                    new TestAggregateMojo(Collections.singletonList(project), null);

            assertThat(mojo.getInputDirectories())
                    .containsExactly(resultsDirectory(project, "child-results"));
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    void shouldAllowResultsDirectoryThatResolvesWithinModule() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-aggregate-module-relative");
        try {
            final MavenProject project =
                    createProject(workspace, "child", "Child", "../allure-results");

            final TestAggregateMojo mojo =
                    new TestAggregateMojo(Collections.singletonList(project), null);
            mojo.setResultsDirectoryValue("../allure-results");

            assertThat(mojo.getInputDirectories())
                    .containsExactly(resultsDirectory(project, "../allure-results"));
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    void shouldSkipResultsDirectoryOutsideModule() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-aggregate-module-escape");
        try {
            createDirectories(workspace.resolve("shared-results"));
            final MavenProject project =
                    createProject(workspace, "child", "Child", "../../shared-results");

            final RecordingLog log = new RecordingLog();
            final TestAggregateMojo mojo =
                    new TestAggregateMojo(Collections.singletonList(project), null);
            mojo.setResultsDirectoryValue("../../shared-results");
            mojo.setLog(log);

            assertThat(mojo.getInputDirectories()).isEmpty();
            assertThat(log.warnMessages).anySatisfy(message -> assertThat(message)
                    .contains("results directory resolves outside module"));
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    void shouldSkipAbsoluteResultsDirectoryOverride() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-aggregate-absolute-override");
        try {
            final Path absoluteResults = workspace.resolve("absolute-results").toAbsolutePath();
            createDirectories(absoluteResults);
            final MavenProject project = createProject(workspace, "child", "Child");
            project.getProperties().setProperty("allure.results.directory",
                    absoluteResults.toString());

            final RecordingLog log = new RecordingLog();
            final TestAggregateMojo mojo =
                    new TestAggregateMojo(Collections.singletonList(project), null);
            mojo.setLog(log);

            assertThat(mojo.getInputDirectories()).isEmpty();
            assertThat(log.warnMessages).anySatisfy(message -> assertThat(message)
                    .contains("should not be absolute for aggregate goal"));
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    void shouldPreserveChildExecutorInfoWhenAggregating() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-aggregate-executor");
        try {
            final MavenProject currentProject =
                    createProject(workspace, "current", "Current", "current-results");
            final MavenProject childProject = createProject(workspace, "child", "Child");
            currentProject.getProperties().setProperty("allure.results.directory",
                    "current-results");
            final Path currentResultsDirectory =
                    resultsDirectory(currentProject, "current-results");
            final Path childResultsDirectory = resultsDirectory(childProject);
            final Path childExecutor = childResultsDirectory.resolve("executor.json");

            Files.writeString(currentResultsDirectory.resolve("executor.json"),
                    "{\"buildName\":\"Old\"}");
            Files.writeString(childExecutor,
                    "{\"buildName\":\"Child\",\"name\":\"Existing\",\"type\":\"custom\"}");

            final TestAggregateMojo mojo = new TestAggregateMojo(
                    Arrays.asList(currentProject, childProject), currentProject);
            final List<Path> inputDirectories =
                    Arrays.asList(currentResultsDirectory, childResultsDirectory);

            assertThat(mojo.executorInfoDirectoriesFrom(inputDirectories))
                    .containsExactly(currentResultsDirectory);

            mojo.copyExecutorInfoTo(inputDirectories);

            final JsonNode currentExecutorInfo = OBJECT_MAPPER
                    .readTree(currentResultsDirectory.resolve("executor.json").toFile());
            assertThat(currentExecutorInfo.get("buildName").asText()).isEqualTo("Current");
            assertThat(currentExecutorInfo.get("name").asText()).isEqualTo("Maven");
            assertThat(currentExecutorInfo.get("type").asText()).isEqualTo("maven");

            final JsonNode childExecutorInfo = OBJECT_MAPPER.readTree(childExecutor.toFile());
            assertThat(childExecutorInfo.get("buildName").asText()).isEqualTo("Child");
            assertThat(childExecutorInfo.get("name").asText()).isEqualTo("Existing");
            assertThat(childExecutorInfo.get("type").asText()).isEqualTo("custom");
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    void shouldSkipExecutorInfoWhenCurrentModuleResultsAreNotInAggregateInputs() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-aggregate-executor-missing");
        try {
            final MavenProject currentProject = createProject(workspace, "current", "Current");
            final MavenProject childProject = createProject(workspace, "child", "Child");

            final TestAggregateMojo mojo =
                    new TestAggregateMojo(Collections.singletonList(childProject), currentProject);

            assertThat(mojo.executorInfoDirectoriesFrom(
                    Collections.singletonList(resultsDirectory(childProject)))).isEmpty();
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    private static MavenProject createProject(final Path workspace, final String directoryName,
            final String projectName, final String... resultsDirectories) throws Exception {
        final Path projectDirectory = workspace.resolve(directoryName);
        final Path buildDirectory = projectDirectory.resolve("target");
        createDirectories(projectDirectory);
        Files.writeString(projectDirectory.resolve("pom.xml"), "<project/>");
        if (resultsDirectories.length == 0) {
            createDirectories(buildDirectory.resolve("allure-results"));
        } else {
            for (String resultsDirectory : resultsDirectories) {
                createDirectories(buildDirectory.resolve(resultsDirectory).normalize());
            }
        }

        final Build build = new Build();
        build.setDirectory(buildDirectory.toString());

        final MavenProject project = new MavenProject();
        project.setName(projectName);
        project.setBuild(build);
        project.setFile(projectDirectory.resolve("pom.xml").toFile());
        return project;
    }

    private static Path resultsDirectory(final MavenProject project) {
        return Paths.get(project.getBuild().getDirectory(), "allure-results").toAbsolutePath();
    }

    private static Path resultsDirectory(final MavenProject project,
            final String configuredResultsDirectory) {
        return Paths.get(project.getBuild().getDirectory()).resolve(configuredResultsDirectory)
                .toAbsolutePath().normalize();
    }

    private static void createDirectories(final Path directory) throws IOException {
        Files.createDirectories(directory);
    }

    private static final class TestAggregateMojo extends AllureAggregateMojo {

        private final List<MavenProject> sessionProjects;
        private final MavenProject currentProject;

        private TestAggregateMojo(final List<MavenProject> sessionProjects,
                final MavenProject currentProject) {
            this.sessionProjects = sessionProjects;
            this.currentProject = currentProject;
            this.resultsDirectory = "allure-results";
        }

        @Override
        protected List<MavenProject> getSessionProjects() {
            return sessionProjects;
        }

        @Override
        protected MavenProject getProject() {
            return currentProject;
        }

        private List<Path> executorInfoDirectoriesFrom(final List<Path> inputDirectories) {
            return getExecutorInfoDirectories(inputDirectories);
        }

        private void copyExecutorInfoTo(final List<Path> inputDirectories) throws Exception {
            copyExecutorInfo(inputDirectories);
        }

        private void setResultsDirectoryValue(final String resultsDirectory) {
            this.resultsDirectory = resultsDirectory;
        }
    }

    private static final class RecordingLog implements Log {

        private final List<String> warnMessages = new ArrayList<String>();

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void debug(final CharSequence content) {}

        @Override
        public void debug(final CharSequence content, final Throwable error) {}

        @Override
        public void debug(final Throwable error) {}

        @Override
        public boolean isInfoEnabled() {
            return false;
        }

        @Override
        public void info(final CharSequence content) {}

        @Override
        public void info(final CharSequence content, final Throwable error) {}

        @Override
        public void info(final Throwable error) {}

        @Override
        public boolean isWarnEnabled() {
            return true;
        }

        @Override
        public void warn(final CharSequence content) {
            warnMessages.add(content.toString());
        }

        @Override
        public void warn(final CharSequence content, final Throwable error) {
            warn(content);
        }

        @Override
        public void warn(final Throwable error) {
            warn(error.getMessage());
        }

        @Override
        public boolean isErrorEnabled() {
            return false;
        }

        @Override
        public void error(final CharSequence content) {}

        @Override
        public void error(final CharSequence content, final Throwable error) {}

        @Override
        public void error(final Throwable error) {}
    }
}
