package io.qameta.allure.maven;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.reporting.MavenReportException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.lang.String.format;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 04.08.15
 */

public abstract class AllureGenerateMojo extends AllureBaseMojo {

    public static final String ALLURE_OLD_PROPERTIES = "allure.properties";

    public static final String ALLURE_NEW_PROPERTIES = "report.properties";

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
     * The version on Allure report to generate.
     */
    @Parameter(property = "allure.version", required = false,
            defaultValue = "${project.version}")
    protected String reportVersion;

    /**
     * The directory to generate Allure report into.
     */
    @Parameter(property = "allure.report.directory",
            defaultValue = "${project.reporting.outputDirectory}/allure-maven-plugin")
    protected String reportDirectory;

    /**
     * The path to the allure.properties file
     */
    @Parameter(defaultValue = "report.properties")
    protected String propertiesFilePath;

    /**
     * The additional Allure properties such as issue tracker pattern.
     */
    @Parameter
    protected Map<String, String> properties = new HashMap<>();

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

            installAllure();

            getLog().info(format("Generate Allure report (%s) with version %s", getMojoName(), reportVersion));
            getLog().info("Generate Allure report to " + reportDirectory);

            List<Path> inputDirectories = getInputDirectories();
            if (inputDirectories.isEmpty()) {
                getLog().warn("Allure report was skipped because there is no results directories found.");
                return;
            }

            readPropertiesFile();
            readPropertiesFileFromClasspath(ALLURE_OLD_PROPERTIES);
            readPropertiesFileFromClasspath(ALLURE_NEW_PROPERTIES);
            readPropertiesFromMap();

            generateReport(inputDirectories);

            render(getSink(), getName(locale));
        } catch (Exception e) {
            throw new MavenReportException("Could not generate the report", e);
        }
    }

    private void installAllure() throws MavenReportException{
        try {
            AllureCommandline commandline
                    = new AllureCommandline(Paths.get(getInstallDirectory()), reportVersion);
            if (commandline.notExists()) {

                getLog().info(String.format("Allure installation directory %s", getInstallDirectory()));
                getLog().info(String.format("Try to finding out allure %s", reportVersion));

                getLog().info("Downloading allure commandline...");
                commandline.download(getAllureDownloadRoot(), false);
                getLog().info("Downloading allure commandline complete");
            }
        } catch (Exception e) {
            getLog().error("Can't install allure", e);
            throw new MavenReportException("Can't install allure", e);
        }
    }

    private void generateReport(List<Path> resultsPaths) throws MavenReportException {
        try {
            Path reportPath = Paths.get(reportDirectory);

            AllureCommandline commandline
                    = new AllureCommandline(Paths.get(getInstallDirectory()), reportVersion);

            getLog().info("Generate report to " + reportPath);
            commandline.generateReport(resultsPaths, reportPath);
            getLog().info("Report generated successfully.");
        } catch (Exception e) {
            getLog().error("Can't generate allure report data", e);
            throw new MavenReportException("Can't generate allure report data", e);
        }
    }

    /**
     * Read system properties from file {@link #propertiesFilePath}.
     *
     * @throws IOException if any occurs.
     */
    protected void readPropertiesFile() throws IOException {
        Path path = Paths.get(propertiesFilePath);
        if (Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path)) {
                readPropertiesFromStream(is);
            }
        }
    }

    /**
     * Read allure.properties from classpath.
     *
     * @throws IOException if any occurs.
     */
    protected void readPropertiesFileFromClasspath(String propertiesFileName)
            throws IOException, DependencyResolutionRequiredException {
        try (InputStream is = createProjectClassLoader().getResourceAsStream(propertiesFileName)) {
            readPropertiesFromStream(is);
        }
    }

    /**
     * Set properties from {@link #properties}
     */
    protected void readPropertiesFromMap() {
        for (Map.Entry<String, String> property : properties.entrySet()) {
            if (property.getKey() != null && property.getValue() != null) {
                System.setProperty(property.getKey(), property.getValue());
            }
        }
    }

    /**
     * Load Allure properties from given input stream.
     *
     * @throws IOException if any occurs.
     */
    protected void readPropertiesFromStream(InputStream is) throws IOException {
        if (is != null)
            System.getProperties().load(is);
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
     * Return ClassLoader with classpath elements
     */
    protected ClassLoader createProjectClassLoader()
            throws MalformedURLException, DependencyResolutionRequiredException {
        List<URL> result = new ArrayList<>();
        for (Object element : project.getTestClasspathElements()) {
            if (element != null && element instanceof String) {
                URL url = Paths.get((String) element).toUri().toURL();
                result.add(url);
            }
        }
        return new URLClassLoader(result.toArray(new URL[result.size()]));
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
    protected abstract List<Path> getInputDirectories();

    /**
     * Get the current mojo name.
     */
    protected abstract String getMojoName();

    /**
     * Get install allure directory.
     */
    protected abstract String getInstallDirectory();

    /**
     * Get allure root url.
     */
    protected abstract String getAllureDownloadRoot();
}
