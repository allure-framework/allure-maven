package io.qameta.allure.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import java.nio.file.Paths;

import static io.qameta.allure.maven.AllureCommandline.ALLURE_DEFAULT_VERSION;
import static io.qameta.allure.maven.DownloadUtils.getAllureDownloadUrl;

/**
 * Install allure tool.
 */
@SuppressWarnings("unused")
@Mojo(name = "install", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class AllureInstallMojo extends AbstractMojo {

    @Parameter(property = "report.version")
    private String reportVersion;

    @Parameter(property = "allure.download.url")
    private String allureDownloadUrl;

    @Parameter(property = "allure.install.directory", defaultValue = "${project.basedir}/.allure")
    private String installDirectory;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            final String version = reportVersion != null ? reportVersion : ALLURE_DEFAULT_VERSION;
            getLog().info(String.format("Allure installation directory %s", installDirectory));
            getLog().info(String.format("Try to finding out allure %s", version));

            AllureCommandline commandline = new AllureCommandline(Paths.get(installDirectory), reportVersion);
            if (commandline.allureNotExists()) {
                final String url = getAllureDownloadUrl(version, allureDownloadUrl);
                getLog().info("Downloading allure commandline from " + url);
                commandline.download(url, ProxyUtils.getProxy(session, decrypter));
                getLog().info("Downloading allure commandline complete");
            }
        } catch (Exception e) {
            getLog().error("Can't install allure", e);
            throw new MojoExecutionException("Can't install allure", e);
        }
    }

}
