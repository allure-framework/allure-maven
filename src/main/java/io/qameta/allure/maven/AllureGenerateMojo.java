package io.qameta.allure.maven;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static io.qameta.allure.maven.AllureCommandline.ALLURE_DEFAULT_VERSION;
import static java.lang.String.format;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 04.08.15
 */

public abstract class AllureGenerateMojo extends AllureBaseMojo {

    public static final String ALLURE_OLD_PROPERTIES = "allure.properties";

    public static final String ALLURE_NEW_PROPERTIES = "report.properties";

    public static final String CATEGORIES_FILE_NAME = "categories.json";

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
    @Parameter(property = "report.version")
    protected String reportVersion;

    @Parameter(property = "allure.report.directory",
            defaultValue = "${project.reporting.outputDirectory}/allure-maven-plugin")
    private String reportDirectory;

    /**
     * The path to the allure.properties file
     */
    @Parameter(defaultValue = "report.properties")
    protected String propertiesFilePath;

    @Parameter(property = "allure.install.directory", defaultValue = "${project.basedir}/.allure")
    private String installDirectory;

    @Parameter(property = "allure.download.url",
            defaultValue = "https://dl.bintray.com/qameta/generic/io/qameta/allure/allure/%s/allure-%s.zip")
    private String allureDownloadUrl;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

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
        return getReportDirectory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        try {

            this.installAllure();

            getLog().info(format("Generate Allure report (%s) with version %s", getMojoName(), reportVersion != null ? reportVersion : ALLURE_DEFAULT_VERSION));
            getLog().info("Generate Allure report to " + getReportDirectory());

            List<Path> inputDirectories = getInputDirectories();
            if (inputDirectories.isEmpty()) {
                getLog().warn("Allure report was skipped because there is no results directories found.");
                return;
            }

            this.loadProperties(inputDirectories);
            this.loadCategories(inputDirectories);
            this.copyExecutorInfo(inputDirectories);
            this.generateReport(inputDirectories);

            render(getSink(), getName(locale));
        } catch (Exception e) {
            throw new MavenReportException("Could not generate the report", e);
        }
    }

    private void copyExecutorInfo(List<Path> inputDirectories) throws IOException, DependencyResolutionRequiredException {

        Map<String, Object> executorInfo = new HashMap<>();
        executorInfo.put("name", "Maven");
        executorInfo.put("type", "maven");
        executorInfo.put("buildName", getProject().getName());

        for (Path dir : inputDirectories) {
            Path executorInfoFile = dir.resolve("executor.json");
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(executorInfoFile.toFile(), executorInfo);
        }
    }

    private void loadCategories(List<Path> inputDirectories) throws URISyntaxException, IOException, DependencyResolutionRequiredException {
        URL categoriesUrl = createProjectClassLoader().getResource(CATEGORIES_FILE_NAME);
        if (categoriesUrl == null) {
            getLog().info(String.format("Can't find information about categories."));
            return;
        }
        for (Path dir : inputDirectories) {
            Path categories = Paths.get(categoriesUrl.toURI());
            Files.copy(categories, dir.resolve(CATEGORIES_FILE_NAME), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void loadProperties(List<Path> inputDirectories) throws IOException, DependencyResolutionRequiredException {
        Properties properties = new Properties();
        readPropertiesFile(properties);
        readPropertiesFileFromClasspath(ALLURE_OLD_PROPERTIES, properties);
        readPropertiesFileFromClasspath(ALLURE_NEW_PROPERTIES, properties);
        readPropertiesFromMap(properties);
        prepareProperties(properties);
        for (Path dir : inputDirectories) {
            try (OutputStream outputStream = new FileOutputStream(dir.resolve(ALLURE_OLD_PROPERTIES).toString())){
                properties.store(outputStream, null);
            } catch (IOException e) {
                getLog().info(String.format("Can't store properties in directory %s", dir.toString()), e);
            }
        }
    }

    private void installAllure() throws MavenReportException{
        try {
            getLog().info(String.format("Allure installation directory %s", getInstallDirectory()));
            getLog().info(String.format("Try to finding out allure %s", reportVersion != null ? reportVersion : ALLURE_DEFAULT_VERSION));

            AllureCommandline commandline
                    = new AllureCommandline(Paths.get(getInstallDirectory()), reportVersion);
            if (commandline.allureNotExists()) {
                getLog().info("Downloading allure commandline...");
                commandline.download(allureDownloadUrl, ProxyUtils.getProxy(session, decrypter));
                getLog().info("Downloading allure commandline complete");
            }
        } catch (Exception e) {
            getLog().error("Can't install allure", e);
            throw new MavenReportException("Can't install allure", e);
        }
    }

    protected void generateReport(List<Path> resultsPaths) throws MavenReportException {
        try {
            Path reportPath = Paths.get(getReportDirectory());

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
    protected void readPropertiesFile(Properties properties) throws IOException {
        Path path = Paths.get(propertiesFilePath);
        if (Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path)) {
                properties.load(is);
            }
        }
    }

    /**
     * Read allure.properties from classpath.
     *
     * @throws IOException if any occurs.
     */
    protected void readPropertiesFileFromClasspath(String propertiesFileName, Properties properties)
            throws IOException, DependencyResolutionRequiredException {
        try (InputStream is = createProjectClassLoader().getResourceAsStream(propertiesFileName)) {
            if (is != null) {
                properties.load(is);
            }
        }
    }

    /**
     * Set properties from {@link #properties}
     */
    protected void readPropertiesFromMap(Properties properties) {
        for (Map.Entry<String, String> property : this.properties.entrySet()) {
            if (property.getKey() != null && property.getValue() != null) {
                properties.setProperty(property.getKey(), property.getValue());
            }
        }
    }

    /**
     * Replaces the placeholders in properties.
     * You can use properties like:
     * name1=value1
     * name2=value2 with ${name1}
     * You can also use system and maven properties for the placeholder.
     */
    protected void prepareProperties(Properties properties) {
        Properties allProperties = new Properties();
        allProperties.putAll(properties);
        allProperties.putAll(this.getProject().getProperties());
        allProperties.putAll(System.getProperties());
        for (String name : properties.stringPropertyNames()) {
            properties.setProperty(name, StrSubstitutor.replace(properties.getProperty(name), allProperties));
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

        Path indexHtmlFile = Paths.get(getReportDirectory(), "index.html");
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
     * The directory to generate Allure report into.
     */
    public String getReportDirectory() {
        return reportDirectory;
    }


    public String getInstallDirectory() {
        return installDirectory;
    }
}
