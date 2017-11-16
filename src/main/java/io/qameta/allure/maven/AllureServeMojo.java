package io.qameta.allure.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.reporting.MavenReportException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

/**
 * Calls allure serve command.
 */
@SuppressWarnings("unused")
@Mojo(name = "serve", defaultPhase = LifecyclePhase.SITE)
public class AllureServeMojo extends AllureGenerateMojo {

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Path> getInputDirectories() {
        Path path = getInputDirectoryAbsolutePath();
        if (isDirectoryExists(path)) {
            getLog().info("Found results directory " + path);
            return Collections.singletonList(path);
        }
        getLog().error("Directory " + path + " not found.");
        return Collections.emptyList();
    }

    private Path getInputDirectoryAbsolutePath() {
        Path path = Paths.get(resultsDirectory);
        return path.isAbsolute() ? path : Paths.get(buildDirectory).resolve(path);
    }

    protected void generateReport(List<Path> resultsPaths) throws MavenReportException {
        try {
            Path reportPath = Paths.get(getReportDirectory());

            AllureCommandline commandline
                    = new AllureCommandline(Paths.get(getInstallDirectory()), reportVersion, this.serveTimeout);

            getLog().info("Generate report to " + reportPath);
            commandline.serve(resultsPaths, reportPath, this.serveHost, this.servePort);
            getLog().info("Report generated successfully.");
        } catch (Exception e) {
            getLog().error("Can't generate allure report data", e);
            throw new MavenReportException("Can't generate allure report data", e);
        }
    }

    @Override
    protected String getMojoName() {
        return "serve";
    }
}
