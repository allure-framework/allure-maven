package io.qameta.allure.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import java.nio.file.Paths;

import static io.qameta.allure.maven.AllureCommandline.ALLURE_DEFAULT_VERSION;

/**
 * Install allure tool.
 */
@SuppressWarnings("unused")
@Mojo(name = "install", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class AllureInstallMojo extends AbstractMojo {

    @Parameter(property = "report.version")
    private String reportVersion;

    @Parameter(property = "allure.download.url",
            defaultValue = "https://dl.bintray.com/qameta/generic/io/qameta/allure/allure/%s/allure-%s.zip")
    private String allureDownloadUrl;

    @Parameter(property = "allure.install.directory", defaultValue = "${project.basedir}/.allure")
    private String installDirectory;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getLog().info(String.format("Allure installation directory %s", installDirectory));
            getLog().info(String.format("Try to finding out allure %s", reportVersion != null ? reportVersion : ALLURE_DEFAULT_VERSION));

            AllureCommandline commandline = new AllureCommandline(Paths.get(installDirectory), reportVersion, null);
            if (commandline.allureNotExists()) {
                getLog().info("Downloading allure commandline...");
                commandline.download(allureDownloadUrl, ProxyUtils.getProxy(session, decrypter));
                getLog().info("Downloading allure commandline complete");
            }
        } catch (Exception e) {
            getLog().error("Can't install allure", e);
            throw new MojoExecutionException("Can't install allure", e);
        }
    }
}
