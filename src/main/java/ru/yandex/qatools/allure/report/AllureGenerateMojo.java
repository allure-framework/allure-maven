package ru.yandex.qatools.allure.report;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.reporting.MavenReportException;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.String.format;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 04.08.15
 */
public abstract class AllureGenerateMojo extends AllureResolveMojo {

    public static final String MAIN = "main";

    /**
     * The project build directory. For maven projects it is usually the
     * target folder.
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    protected String buildDirectory;

    /**
     * The project reporting output directory. For maven projects it is
     * usually the target/site folder.
     */
    @Parameter(defaultValue = "${project.reporting.outputDirectory}", readonly = true)
    protected String reportingOutputDirectory;

    /**
     * The path to Allure results directory. In general it is the directory created
     * by allure adaptor and contains allure xml files and attachments. This path can
     * be relative from build directory (for maven it is the target directory) or absolute
     * (absolute only for <code>report</code> mojo). Will be ignored for <code>bulk</code>
     * mojo.
     */
    @Parameter(property = "allure.results.directory", defaultValue = "allure-results/")
    protected String resultsDirectory;

    /**
     * The directory to generate Allure report into.
     */
    @Parameter(property = "allure.report.directory",
            defaultValue = "${project.reporting.outputDirectory}/allure-maven-plugin")
    protected String reportDirectory;

    /**
     * The full name of Allure main class to run during report generation.
     */
    @Parameter(readonly = true, defaultValue = "ru.yandex.qatools.allure.AllureMain")
    protected String allureMain;

    /**
     * Path for @Issues
     */
    @Parameter
    protected String issuesPattern;

    /**
     * Path for @Story
     */
    @Parameter
    protected String testIdPattern;

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getOutputDirectory() {
        return reportDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        try {
            getLog().info(format("Generate Allure report (%s) with version %s", getMojoName(), version));

            ClassLoader loader = resolve();
            Class<?> clazz = loader.loadClass(allureMain);
            Method main = clazz.getMethod(MAIN, String[].class);

            getLog().info("Generate Allure report to " + reportDirectory);

            List<String> inputDirectories = getInputDirectories();
            if (inputDirectories.isEmpty()) {
                getLog().warn("Allure report was skipped because there is no results directories found.");
                return;
            }

            if (issuesPattern != null){
                System.setProperty("allure.issues.tracker.pattern" , issuesPattern);
                getLog().info("allure.issues.tracker.pattern set to: " + issuesPattern);
            }

            if (testIdPattern != null){
                System.setProperty("allure.tests.management.pattern" , testIdPattern);
                getLog().info("allure.tests.management.pattern set to: " + testIdPattern);
            }

            List<String> parameters = new ArrayList<>();
            parameters.addAll(inputDirectories);
            parameters.add(reportDirectory);

            main.invoke(null, new Object[]{parameters.toArray(new String[parameters.size()])});

            render(getSink(), getName(locale));
        } catch (Exception e) {
            throw new MavenReportException("Could not generate the report", e);
        }
    }

    /**
     * Render allure report page in project-reports.html.
     */
    protected void render(Sink sink, String title) {
        sink.head();
        sink.title();
        sink.text(title);
        sink.title_();
        sink.head_();
        sink.body();

        sink.lineBreak();

        Path indexHtmlFile = Paths.get(reportDirectory, "index.html");
        String relativePath = Paths.get(reportingOutputDirectory)
                .relativize(indexHtmlFile).toString();

        sink.rawText(format("<meta http-equiv=\"refresh\" content=\"0;url=%s\" />",
                relativePath));

        sink.link(relativePath);

        sink.body_();
        sink.flush();
        sink.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canGenerateReport() {
        return !isAggregate() || project.isExecutionRoot();
    }

    /**
     * Is the current report aggregated?
     */
    protected boolean isAggregate() {
        return false;
    }

    /**
     * Returns true if given path is an existed directory.
     */
    protected boolean isDirectoryExists(Path path) {
        return Files.exists(path) && Files.isDirectory(path);
    }

    /**
     * Get list of Allure results directories to generate the report.
     */
    protected abstract List<String> getInputDirectories();

    /**
     * Get the current mojo name.
     */
    protected abstract String getMojoName();
}
