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
import org.apache.maven.plugins.annotations.Parameter;
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
     * The projects in the reactor.
     */
    @Parameter(defaultValue = "${reactorProjects}", required = true, readonly = true)
    protected List<MavenProject> reactorProjects;

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

        final List<Path> result = new ArrayList<>();
        for (MavenProject child : reactorProjects) {
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

    @Override
    protected String getMojoName() {
        return "aggregate";
    }

    @Override
    protected boolean isAggregate() {
        return true;
    }

}
