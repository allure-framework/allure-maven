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
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

/**
 * @author Dmitry Baev dmitry.baev@qameta.io Date: 30.07.15
 */
@Mojo(name = "report", defaultPhase = LifecyclePhase.SITE)
public class AllureReportMojo extends AllureGenerateMojo {

    private static final String FOUND_DIRECTORY = "Found results directory %s";
    private static final String DIRECTORY_NOT_FOUND = "Directory %s not found";

    /**
     * The comma-separated list of additional input directories. As long as unix path can contains
     * commas it is bad way to specify few input directories. The main usage of this parameter is
     * some scripts to generate aggregated report. This parameter will be used only in "bulk" mojo.
     */
    @Parameter(property = "allure.results.inputDirectories")
    protected String inputDirectories;

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Path> getInputDirectories() {

        if (StringUtils.isNotBlank(inputDirectories)) {
            return fromInputDirectories();
        }

        final Path path = getInputDirectoryAbsolutePath();
        if (isDirectoryExists(path)) {
            getLog().info(format(FOUND_DIRECTORY, path));
            return Collections.singletonList(path);
        }
        getLog().error(format(DIRECTORY_NOT_FOUND, path));
        return Collections.emptyList();
    }

    private List<Path> fromInputDirectories() {
        final List<Path> results = new ArrayList<>();
        for (String dir : inputDirectories.split(",")) {
            final Path path = Paths.get(dir).toAbsolutePath();
            if (isDirectoryExists(path)) {
                results.add(path);
                getLog().info(format(FOUND_DIRECTORY, path));
            } else {
                getLog().error(format(DIRECTORY_NOT_FOUND, path));
            }
        }
        return results;
    }

    @Override
    protected String getMojoName() {
        return "report";
    }

    private Path getInputDirectoryAbsolutePath() {
        final Path path = Paths.get(resultsDirectory);
        return path.isAbsolute() ? path : Paths.get(buildDirectory).resolve(path);
    }
}
