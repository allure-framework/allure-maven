package ru.yandex.qatools.allure.report;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 30.07.15
 */
@Mojo(name = "report", defaultPhase = LifecyclePhase.SITE)
public class AllureReportMojo extends AllureGenerateMojo {

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getInputDirectories() {
        Path path = getInputDirectoryAbsolutePath();
        if (isDirectoryExists(path)) {
            getLog().info("Found results directory " + path);
            return Collections.singletonList(path.toString());
        }
        getLog().error("Directory " + path + " not found.");
        return Collections.emptyList();
    }

    @Override
    protected String getMojoName() {
        return "report";
    }

    private Path getInputDirectoryAbsolutePath() {
        Path path = Paths.get(resultsDirectory);
        return path.isAbsolute() ? path : Paths.get(buildDirectory).resolve(path);
    }
}
