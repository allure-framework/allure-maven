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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Baev dmitry.baev@qameta.io Date: 04.08.15
 */
@Mojo(
        name = "aggregate",
        defaultPhase = LifecyclePhase.SITE,
        inheritByDefault = false,
        aggregator = true
)
@SuppressWarnings("PMD.GodClass")
public class AllureAggregateMojo extends AllureGenerateMojo {

    private static final String MODULE_RESULTS_DIRECTORY_PREFIX = "Results directory for module ";

    private static final String RESULTS_DIRECTORY_PROPERTY = "allure.results.directory";

    private static final String UNAVAILABLE_PROJECT_LABEL = "N/A";

    /**
     * Normalize the inherited report parameter so direct CLI invocations can fall back at runtime
     * instead of failing during configuration when Maven injects null here.
     */
    @SuppressWarnings("unused")
    public void setReactorProjects(final List<MavenProject> reactorProjects) {
        this.reactorProjects = reactorProjects == null ? Collections.<MavenProject>emptyList() : reactorProjects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Path> getInputDirectories() {
        final List<MavenProject> projects = resolveReactorProjects();
        if (projects.isEmpty()) {
            getLog().warn("Reactor projects were not resolved for aggregate goal.");
            return Collections.emptyList();
        }

        final List<Path> result = new ArrayList<>();
        for (MavenProject child : projects) {
            final Path path = resolveResultsDirectory(child, true);
            if (path == null) {
                continue;
            }
            if (isDirectoryExists(path)) {
                result.add(path);
                getLog().info("Found results directory " + path);
            } else {
                getLog().warn(MODULE_RESULTS_DIRECTORY_PREFIX + child.getName() + " not found.");
            }
        }

        return result;
    }

    protected List<MavenProject> resolveReactorProjects() {
        final List<MavenProject> injectedProjects = getInjectedReactorProjects();
        if (!injectedProjects.isEmpty()) {
            return injectedProjects;
        }

        final List<MavenProject> sessionProjects = getSessionProjects();
        if (!sessionProjects.isEmpty()) {
            return sessionProjects;
        }

        final MavenProject currentProject = getProject();
        if (currentProject != null) {
            return Collections.singletonList(currentProject);
        }

        return Collections.emptyList();
    }

    protected List<MavenProject> getInjectedReactorProjects() {
        if (reactorProjects == null) {
            return Collections.emptyList();
        }
        return reactorProjects;
    }

    protected List<MavenProject> getSessionProjects() {
        if (session == null || session.getProjects() == null) {
            return Collections.emptyList();
        }
        return session.getProjects();
    }

    @Override
    protected List<Path> getExecutorInfoDirectories(final List<Path> inputDirectories) {
        final MavenProject currentProject = getProject();
        if (currentProject == null) {
            return Collections.emptyList();
        }

        final Path currentResultsDirectory = resolveResultsDirectory(currentProject, false);
        if (currentResultsDirectory == null) {
            return Collections.emptyList();
        }

        for (Path inputDirectory : inputDirectories) {
            if (inputDirectory.toAbsolutePath().normalize().equals(currentResultsDirectory)) {
                return Collections.singletonList(inputDirectory);
            }
        }

        return Collections.emptyList();
    }

    @Override
    protected String getMojoName() {
        return "aggregate";
    }

    @Override
    protected boolean isAggregate() {
        return true;
    }

    protected Path resolveResultsDirectory(final MavenProject project) {
        return resolveResultsDirectory(project, true);
    }

    private Path resolveResultsDirectory(final MavenProject project, final boolean logWarnings) {
        Path resolvedDirectory = null;
        final Path moduleDirectory = getModuleDirectory(project);
        if (moduleDirectory == null) {
            logInvalidResultsDirectory(project, "module directory is not available", logWarnings);
        } else {
            final Path buildDirectory = getBuildDirectory(project, moduleDirectory, logWarnings);
            final Path relativeDirectory = getRelativeResultsDirectory(project, logWarnings);
            if (buildDirectory != null && relativeDirectory != null) {
                resolvedDirectory = buildDirectory.resolve(relativeDirectory).normalize();
                if (!resolvedDirectory.startsWith(moduleDirectory)) {
                    logInvalidResultsDirectory(
                            project,
                            "results directory resolves outside module: " + resolvedDirectory,
                            logWarnings
                    );
                    resolvedDirectory = null;
                }
            }
        }
        return resolvedDirectory;
    }

    private Path getRelativeResultsDirectory(final MavenProject project,
                                             final boolean logWarnings) {
        final String configuredResultsDirectory = getConfiguredResultsDirectory(project);
        try {
            final Path relativeDirectory = Paths.get(configuredResultsDirectory);
            if (relativeDirectory.isAbsolute()) {
                logInvalidResultsDirectory(
                        project,
                        "results directory should not be absolute for aggregate goal: "
                                + configuredResultsDirectory,
                        logWarnings
                );
                return null;
            }
            return relativeDirectory;
        } catch (InvalidPathException e) {
            logInvalidResultsDirectory(
                    project,
                    "results directory path is invalid: " + configuredResultsDirectory,
                    logWarnings
            );
            return null;
        }
    }

    private Path getBuildDirectory(final MavenProject project, final Path moduleDirectory,
                                   final boolean logWarnings) {
        if (project.getBuild() == null || StringUtils.isBlank(project.getBuild().getDirectory())) {
            logInvalidResultsDirectory(project, "build directory is not available", logWarnings);
            return null;
        }

        try {
            Path buildDirectory = Paths.get(project.getBuild().getDirectory());
            if (!buildDirectory.isAbsolute()) {
                buildDirectory = moduleDirectory.resolve(buildDirectory);
            }
            return buildDirectory.toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            logInvalidResultsDirectory(
                    project,
                    "build directory path is invalid: " + project.getBuild().getDirectory(),
                    logWarnings
            );
            return null;
        }
    }

    private Path getModuleDirectory(final MavenProject project) {
        if (project == null || project.getBasedir() == null) {
            return null;
        }
        return project.getBasedir().toPath().toAbsolutePath().normalize();
    }

    private String getConfiguredResultsDirectory(final MavenProject project) {
        if (project != null && project.getProperties() != null) {
            final String propertyOverride = project.getProperties().getProperty(RESULTS_DIRECTORY_PROPERTY);
            if (StringUtils.isNotBlank(propertyOverride)) {
                return propertyOverride;
            }
        }
        return resultsDirectory;
    }

    private void logInvalidResultsDirectory(final MavenProject project, final String reason,
                                            final boolean logWarnings) {
        if (logWarnings) {
            getLog().warn(
                    MODULE_RESULTS_DIRECTORY_PREFIX + getProjectLabel(project)
                            + " is invalid for aggregate goal: " + reason + "."
            );
        }
    }

    private String getProjectLabel(final MavenProject project) {
        if (project == null) {
            return UNAVAILABLE_PROJECT_LABEL;
        }
        if (StringUtils.isNotBlank(project.getName())) {
            return project.getName();
        }
        if (StringUtils.isNotBlank(project.getArtifactId())) {
            return project.getArtifactId();
        }
        return UNAVAILABLE_PROJECT_LABEL;
    }

}
