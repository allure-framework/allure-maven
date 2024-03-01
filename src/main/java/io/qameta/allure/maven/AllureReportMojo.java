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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Baev dmitry.baev@qameta.io Date: 30.07.15
 */
@Mojo(name = "report", defaultPhase = LifecyclePhase.SITE)
public class AllureReportMojo extends AllureGenerateMojo {

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Path> getInputDirectories() {
        final Path path = getInputDirectoryAbsolutePath();
        if (isDirectoryExists(path)) {
            getLog().info("Found results directory " + path);
            return Collections.singletonList(path);
        }
        getLog().error("Directory " + path + " not found.");
        return Collections.emptyList();
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
