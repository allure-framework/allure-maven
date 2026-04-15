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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Baev dmitry.baev@qameta.io Date: 04.08.15
 */
@Mojo(name = "aggregate", defaultPhase = LifecyclePhase.SITE, inheritByDefault = false,
        aggregator = true)
public class AllureAggregateMojo extends AllureGenerateMojo {

    /**
     * Normalize the inherited report parameter so direct CLI invocations can fall back at runtime
     * instead of failing during configuration when Maven injects null here.
     */
    @SuppressWarnings("unused")
    public void setReactorProjects(final List<MavenProject> reactorProjects) {
        this.reactorProjects =
                reactorProjects == null ? Collections.<MavenProject>emptyList() : reactorProjects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Path> getInputDirectories() {
        final Path relative = Paths.get(resultsDirectory);
        if (relative.isAbsolute()) {
            getLog().error("Input directory should be not absolute for aggregate goal.");
            return Collections.emptyList();
        }

        final List<MavenProject> projects = resolveReactorProjects();
        if (projects.isEmpty()) {
            getLog().warn("Reactor projects were not resolved for aggregate goal.");
            return Collections.emptyList();
        }

        final List<Path> result = new ArrayList<>();
        for (MavenProject child : projects) {
            final Path target = Paths.get(child.getBuild().getDirectory());
            final Path path = target.resolve(relative).toAbsolutePath();
            if (isDirectoryExists(path)) {
                result.add(path);
                getLog().info("Found results directory " + path);
            } else {
                getLog().warn("Results directory for module " + child.getName() + " not found.");
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
        if (currentProject == null || currentProject.getBuild() == null
                || currentProject.getBuild().getDirectory() == null) {
            return Collections.emptyList();
        }

        final Path relative = Paths.get(resultsDirectory);
        if (relative.isAbsolute()) {
            return Collections.emptyList();
        }

        final Path currentResultsDirectory = Paths.get(currentProject.getBuild().getDirectory())
                .resolve(relative).toAbsolutePath().normalize();
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

}
