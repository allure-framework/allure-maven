package ru.yandex.qatools.allure.report;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 04.08.15
 */
@Mojo(name = "bulk", defaultPhase = LifecyclePhase.SITE, inheritByDefault = false)
public class AllureBulkMojo extends AllureGenerateMojo {

    @Parameter(property = "allure.install.directory", required = false,
            defaultValue = "${project.basedir}/.allure")
    private String installDirectory;

    /**
     * The comma-separated list of additional input directories. As long as
     * unix path can contains commas it is bad way to specify few input
     * directories. The main usage of this parameter is some scripts
     * to generate aggregated report. This parameter will be used only
     * in "bulk" mojo.
     */
    @Parameter(property = "allure.results.inputDirectories")
    protected String inputDirectories;

    @Parameter(property = "allure.download.url", required = false,
            defaultValue = "https://dl.bintray.com/qameta/generic/")
    private String allureDownloadRoot;

    @Override
    protected List<Path> getInputDirectories() {
        List<Path> results = new ArrayList<>();
        for (String dir : inputDirectories.split(",")) {
            Path path = Paths.get(dir).toAbsolutePath();
            if (isDirectoryExists(path)) {
                results.add(path);
                getLog().info("Found results directory " + path);
            } else {
                getLog().warn("Directory " + path + " not found.");
            }
        }

        return results;
    }

    @Override
    protected String getMojoName() {
        return "bulk";
    }

    @Override
    protected String getInstallDirectory() {
        return this.installDirectory;
    }

    @Override
    protected String getAllureDownloadRoot() {
        return this.allureDownloadRoot;
    }
}
