package ru.yandex.qatools.allure.report;

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
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 04.08.15
 */
@Mojo(name = "aggregate", defaultPhase = LifecyclePhase.SITE,
        inheritByDefault = false, aggregator = true)
public class AlureAggregateMojo extends AllureGenerateMojo {


    @Parameter(property = "allure.install.directory", required = false,
            defaultValue = "${project.basedir}/.allure")
    private String installDirectory;

    @Parameter(property = "allure.download.url", required = false,
            defaultValue = "https://dl.bintray.com/qameta/generic/")
    private String allureDownloadRoot;

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
        Path relative = Paths.get(resultsDirectory);
        if (relative.isAbsolute()) {
            getLog().error("Input directory should be not absolute for aggregate goal.");
            return Collections.emptyList();
        }

        List<Path> result = new ArrayList<>();
        for (MavenProject child : reactorProjects) {
            Path target = Paths.get(child.getBuild().getDirectory());
            Path path = target.resolve(relative).toAbsolutePath();
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
    protected String getInstallDirectory() {
        return this.installDirectory;
    }

    @Override
    protected String getAllureDownloadRoot() {
        return this.allureDownloadRoot;
    }

    @Override
    protected boolean isAggregate() {
        return true;
    }


}
