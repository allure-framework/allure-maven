package ru.yandex.qatools.allure.report;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.reporting.MavenReportException;

import java.nio.file.Paths;

/**
 * Install allure tool.
 */
@SuppressWarnings("unused")
@Mojo(name = "install", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class AllureInstallMojo extends AbstractMojo {

    @Parameter(property = "allure.version", required = false,
            defaultValue = "${project.version}")
    private String reportVersion;

    @Parameter(property = "allure.download.url", required = false,
            defaultValue = "https://dl.bintray.com/qameta/generic/")
    private String allureDownloadRoot;

    @Parameter(property = "allure.install.directory", required = false,
            defaultValue = "${project.basedir}/.allure")
    private String installDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getLog().info(String.format("Allure installation directory %s", installDirectory));
            getLog().info(String.format("Try to finding out allure %s", reportVersion));

            AllureCommandline commandline = new AllureCommandline(Paths.get(installDirectory), reportVersion);
            if (commandline.notExists()) {
                getLog().info("Downloading allure commandline...");
                commandline.download(allureDownloadRoot, false);
                getLog().info("Downloading allure commandline complete");
            }
        } catch (Exception e) {
            getLog().error("Can't install allure", e);
            throw new MojoExecutionException("Can't install allure", e);
        }

    }
}
