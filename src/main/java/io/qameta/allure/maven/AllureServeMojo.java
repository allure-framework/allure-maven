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
import org.apache.maven.reporting.MavenReportException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Calls allure serve command.
 */
@SuppressWarnings("unused")
@Mojo(name = "serve", defaultPhase = LifecyclePhase.SITE, inheritByDefault = false)
public class AllureServeMojo extends AllureGenerateMojo {

    /**
     * Serve timeout parameter in seconds.
     */
    @Parameter(property = "allure.serve.timeout", defaultValue = "3600")
    private int serveTimeout;

    /**
     * Serve host parameter.
     */
    @Parameter(property = "allure.serve.host")
    private String serveHost;

    /**
     * Serve port parameter.
     */
    @Parameter(property = "allure.serve.port", defaultValue = "0")
    private Integer servePort;

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

    private Path getInputDirectoryAbsolutePath() {
        final Path path = Paths.get(resultsDirectory);
        return path.isAbsolute() ? path : Paths.get(buildDirectory).resolve(path);
    }

    @Override
    protected void generateReport(final List<Path> resultsPaths) throws MavenReportException {
        try {
            final Path reportPath = Paths.get(getReportDirectory());

            final AllureCommandline commandline = new AllureCommandline(
                    Paths.get(getInstallDirectory()), reportVersion, this.serveTimeout);

            getLog().info("Generate report to " + reportPath);
            commandline.serve(resultsPaths, reportPath, this.serveHost, this.servePort);
            getLog().info("Report generated successfully.");
        } catch (Exception e) {
            getLog().error("Generate error", e);
            throw new MavenReportException("Can't generate allure report", e);
        }
    }

    @Override
    protected String getMojoName() {
        return "serve";
    }
}
