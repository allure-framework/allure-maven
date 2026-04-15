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
import org.apache.maven.model.Build;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class AllureAggregateMojoTest {

    @Test
    public void shouldPreferInjectedReactorProjectsOverSessionProjects() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-aggregate-injected");
        try {
            final MavenProject injectedProject = createProject(workspace, "injected", "Injected");
            final MavenProject sessionProject = createProject(workspace, "session", "Session");

            final TestAggregateMojo mojo =
                    new TestAggregateMojo(Collections.singletonList(sessionProject), null);
            mojo.setReactorProjects(Collections.singletonList(injectedProject));

            assertThat(mojo.getInputDirectories(), contains(resultsDirectory(injectedProject)));
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    public void shouldUseSessionProjectsWhenInjectedProjectsAreMissing() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-aggregate-session");
        try {
            final MavenProject sessionProject = createProject(workspace, "session", "Session");

            final TestAggregateMojo mojo =
                    new TestAggregateMojo(Collections.singletonList(sessionProject), null);

            assertThat(mojo.getInputDirectories(), contains(resultsDirectory(sessionProject)));
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    public void shouldFallbackToCurrentProjectWhenReactorProjectsAreMissing() throws Exception {
        final Path workspace = Files.createTempDirectory("allure-aggregate-project");
        try {
            final MavenProject currentProject = createProject(workspace, "current", "Current");

            final TestAggregateMojo mojo =
                    new TestAggregateMojo(Collections.<MavenProject>emptyList(), currentProject);

            assertThat(mojo.getInputDirectories(), contains(resultsDirectory(currentProject)));
        } finally {
            FileUtils.deleteQuietly(workspace.toFile());
        }
    }

    @Test
    public void shouldWarnAndReturnEmptyListWhenNoProjectsAreAvailable() {
        final RecordingLog log = new RecordingLog();
        final TestAggregateMojo mojo =
                new TestAggregateMojo(Collections.<MavenProject>emptyList(), null);
        mojo.setLog(log);

        assertThat(mojo.getInputDirectories(), is(empty()));
        assertThat(log.warnMessages,
                contains("Reactor projects were not resolved for aggregate " + "goal."));
    }

    private static MavenProject createProject(final Path workspace, final String directoryName,
            final String projectName) throws Exception {
        final Path projectDirectory = workspace.resolve(directoryName);
        final Path buildDirectory = projectDirectory.resolve("target");
        Files.createDirectories(buildDirectory.resolve("allure-results"));

        final Build build = new Build();
        build.setDirectory(buildDirectory.toString());

        final MavenProject project = new MavenProject();
        project.setName(projectName);
        project.setBuild(build);
        return project;
    }

    private static Path resultsDirectory(final MavenProject project) {
        return Paths.get(project.getBuild().getDirectory(), "allure-results").toAbsolutePath();
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
