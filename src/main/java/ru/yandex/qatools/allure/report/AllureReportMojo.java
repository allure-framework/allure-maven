package ru.yandex.qatools.allure.report;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import ru.yandex.qatools.allure.report.utils.DependencyResolver;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 09.06.14
 */
@SuppressWarnings("unused")
@Mojo(name = "report", defaultPhase = LifecyclePhase.SITE)
public class AllureReportMojo extends AbstractMavenReport {

    @Parameter(property = "allure.report.directory", required = false,
            defaultValue = "${project.reporting.outputDirectory}/allure-maven-plugin")
    private File outputDirectory;

    @Parameter(property = "allure.results.directory", required = false,
            defaultValue = "${project.build.directory}/allure-results")
    private File allureResultsDirectory;

    @Parameter(property = "allure.version", required = false, defaultValue = "1.3.9")
    private String version;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    @Component
    protected PluginDescriptor plugin;

    @Component
    protected Renderer siteRenderer;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> projectRepos;

    @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true)
    private List<RemoteRepository> pluginRepos;

    @Override
    protected Renderer getSiteRenderer() {
        return siteRenderer;
    }

    @Override
    protected String getOutputDirectory() {
        return outputDirectory.getAbsolutePath();
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        getLog().info("Allure version: " + version);
        try {
            DependencyResolver resolver = new DependencyResolver(repoSystem, repoSession, projectRepos);
            AllureReportBuilder builder = new AllureReportBuilder(version, outputDirectory, resolver);
            getLog().info("Generate report from " + allureResultsDirectory + " to " + outputDirectory);
            builder.processResults(allureResultsDirectory);
            getLog().info("Report data generated successfully. Unpack report face...");
            builder.unpackFace();
            getLog().info("Report unpacked successfully.");
            render(getSink(), getName(locale));
        } catch (AllureReportBuilderException e) {
            getLog().error("Can't generate allure report data", e);
            throw new MavenReportException("Can't generate allure report data", e);
        }
    }

    /**
     * Render allure report page in project-reports.html.
     * @param sink
     * @param title
     */
    private void render(Sink sink, String title) {
        sink.head();
        sink.title();
        sink.text(title);
        sink.title_();
        sink.head_();
        sink.body();

        sink.lineBreak();

        sink.rawText("<meta http-equiv=\"refresh\" content=\"0;url=allure-maven-plugin/index.html\" />");
        sink.link("allure-maven-plugin/index.html");

        sink.body_();
        sink.flush();
        sink.close();
    }

    /**
     * Get the base name used to create report's output file(s).
     *
     * @return the output name of this report.
     */
    @Override
    public String getOutputName() {
        return plugin.getArtifactId();
    }

    /**
     * Get the localized report name.
     *
     * @param locale the wanted locale to return the report's name, could be null.
     * @return the name of this report.
     */
    @Override
    public String getName(Locale locale) {
        return "Allure";
    }

    /**
     * Get the localized report description.
     *
     * @param locale the wanted locale to return the report's description, could be null.
     * @return the description of this report.
     */
    @Override
    public String getDescription(Locale locale) {
        return "Extended report on the test results of the project.";
    }
}
