/*
 *  Copyright 2016-2024 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.maven;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static io.qameta.allure.maven.AllureCommandline.ALLURE_DEFAULT_VERSION;

/**
 * @author Dmitry Baev dmitry.baev@qameta.io Date: 04.08.15
 */
@SuppressWarnings("ClassFanOutComplexity")
public abstract class AllureGenerateMojo extends AllureBaseMojo {

    public static final String ALLURE_OLD_PROPERTIES = "allure.properties";

    public static final String ALLURE_NEW_PROPERTIES = "report.properties";

    public static final String CATEGORIES_FILE_NAME = "categories.json";

    /**
     * The project build directory. For maven projects it is usually the target folder.
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    protected String buildDirectory;

    /**
     * The project reporting output directory. For maven projects it is usually the target/site
     * folder.
     */
    @Parameter(defaultValue = "${project.reporting.outputDirectory}", readonly = true)
    protected String reportingOutputDirectory;

    /**
     * The path to Allure results directory. In general it is the directory created by allure
     * adaptor and contains allure xml files and attachments. This path can be relative from build
     * directory (for maven it is the target directory) or absolute (absolute only for
     * <code>report</code> mojo). Will be ignored for <code>bulk</code> mojo.
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
    protected String reportDirectory;

    /**
     * Report timeout parameter in seconds.
     */
    @Parameter(property = "allure.report.timeout", defaultValue = "60")
    protected int reportTimeout;

    /**
     * This is key-value map which defines executor.json file content. Default content:
     * {"buildName":"${project.name}","name":"Maven","type":"maven"}
     */
    @Parameter
    protected final Map<String, String> executorInfo = new HashMap<>();

    /**
     * The path to the allure.properties file.
     */
    @Parameter(defaultValue = "report.properties")
    protected String propertiesFilePath;

    @Parameter(property = "allure.install.directory", defaultValue = "${project.basedir}/.allure")
    protected String installDirectory;

    @Parameter(property = "allure.download.url")
    protected String allureDownloadUrl;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    @Component(role = SettingsDecrypter.class)
    protected SettingsDecrypter decrypter;

    @Component
    protected DependencyResolver dependencyResolver;

    /**
     * The additional Allure properties such as issue tracker pattern.
     */
    @Parameter
    protected Map<String, String> properties = new HashMap<>();

    /**
     * The report generation mode.
     */
    @Parameter
    protected Boolean singleFile;

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
    protected void executeReport(final Locale locale) throws MavenReportException {
        try {

            this.installAllure();

            getLog().info(String.format("Generate Allure report (%s) with version %s",
                    getMojoName(), reportVersion != null ? reportVersion : ALLURE_DEFAULT_VERSION));
            getLog().info("Generate Allure report to " + getReportDirectory());

            final List<Path> inputDirectories = getInputDirectories();

            if (inputDirectories.isEmpty()) {
                getLog().warn(
                        "Allure report was skipped because there is no results directories found.");
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

    private void copyExecutorInfo(final List<Path> inputDirectories) throws IOException {
        addPropertyIfAbsent("name", "Maven");
        addPropertyIfAbsent("type", "maven");
        addPropertyIfAbsent("buildName", getProject() == null ? "N/A" : getProject().getName());

        final ObjectMapper mapper = new ObjectMapper();
        for (Path dir : inputDirectories) {
            final Path executorInfoFile = dir.resolve("executor.json");
            mapper.writeValue(executorInfoFile.toFile(), executorInfo);
        }
    }

    private void addPropertyIfAbsent(final String key, final String value) {
        if (!executorInfo.containsKey(key)) {
            executorInfo.put(key, value);
        }
    }

    private void loadCategories(final List<Path> inputDirectories)
            throws URISyntaxException, IOException, DependencyResolutionRequiredException {
        final URL categoriesUrl = createProjectClassLoader().getResource(CATEGORIES_FILE_NAME);
        if (categoriesUrl == null) {
            getLog().info("Can't find information about categories");
            return;
        }
        for (Path dir : inputDirectories) {
            final Path categories = Paths.get(categoriesUrl.toURI());
            Files.copy(categories, dir.resolve(CATEGORIES_FILE_NAME),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void loadProperties(final List<Path> inputDirectories)
            throws IOException, DependencyResolutionRequiredException {
        final Properties properties = new Properties();
        readPropertiesFile(properties);
        readPropertiesFileFromClasspath(ALLURE_OLD_PROPERTIES, properties);
        readPropertiesFileFromClasspath(ALLURE_NEW_PROPERTIES, properties);
        readPropertiesFromMap(properties);
        prepareProperties(properties);
        for (Path dir : inputDirectories) {
            try (OutputStream os = Files.newOutputStream(dir.resolve(ALLURE_OLD_PROPERTIES))) {
                properties.store(os, null);
            } catch (IOException e) {
                getLog().info(
                        String.format("Can't store properties in directory %s", dir.toString()), e);
            }
        }
    }

    private void installAllure() throws MavenReportException {
        try {
            final AllureCommandline commandline =
                    new AllureCommandline(Paths.get(installDirectory), reportVersion);
            getLog().info(String.format("Allure installation directory %s", installDirectory));
            getLog().info(String.format("Try to finding out allure %s", commandline.getVersion()));

            if (commandline.allureNotExists()) {
                if (StringUtils.isNotBlank(allureDownloadUrl)) {
                    getLog().info("Downloading allure commandline from " + allureDownloadUrl);
                    commandline.download(allureDownloadUrl,
                            ProxyUtils.getProxy(session, decrypter));
                    getLog().info("Downloading allure commandline complete");
                } else {
                    commandline.downloadWithMaven(session, dependencyResolver);
                }
            }
        } catch (IOException e) {
            getLog().error("Installation error", e);
            throw new MavenReportException("Can't install allure", e);
        }
    }

    protected void generateReport(final List<Path> resultsPaths) throws MavenReportException {
        try {
            final Path reportPath = Paths.get(getReportDirectory());

            final AllureCommandline commandline = new AllureCommandline(
                    Paths.get(getInstallDirectory()), reportVersion, reportTimeout);

            getLog().info("Generate report to " + reportPath);
            commandline.generateReport(resultsPaths, reportPath, Boolean.TRUE.equals(singleFile));
            getLog().info("Report generated successfully.");
        } catch (Exception e) {
            getLog().error("Generation error", e);
            throw new MavenReportException("Can't generate allure report data", e);
        }
    }

    /**
     * Read system properties from file {@link #propertiesFilePath}.
     *
     * @throws IOException if any occurs.
     */
    protected void readPropertiesFile(final Properties properties) throws IOException {
        final Path path = Paths.get(propertiesFilePath);
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
    protected void readPropertiesFileFromClasspath(final String propertiesFileName,
                                                   final Properties properties)
            throws IOException, DependencyResolutionRequiredException {
        try (InputStream is = createProjectClassLoader().getResourceAsStream(propertiesFileName)) {
            if (is != null) {
                properties.load(is);
            }
        }
    }

    /**
     * Set properties from {@link #properties}.
     */
    protected void readPropertiesFromMap(final Properties properties) {
        for (Map.Entry<String, String> property : this.properties.entrySet()) {
            if (property.getKey() != null && property.getValue() != null) {
                properties.setProperty(property.getKey(), property.getValue());
            }
        }
    }

    /**
     * Replaces the placeholders in properties. You can use properties like: name1=value1
     * name2=value2 with ${name1} You can also use system and maven properties for the placeholder.
     */
    protected void prepareProperties(final Properties properties) {
        final Properties allProperties = new Properties();
        allProperties.putAll(properties);
        allProperties.putAll(
                getProject() == null ? Collections.emptyMap() : getProject().getProperties());
        allProperties.putAll(System.getProperties());
        for (String name : properties.stringPropertyNames()) {
            properties.setProperty(name,
                    StringSubstitutor.replace(properties.getProperty(name), allProperties));
        }
    }

    /**
     * Render allure report page in project-reports.html.
     */
    protected void render(final Sink sink, final String title) {
        sink.head();
        sink.title();
        sink.text(title);
        sink.title_();
        sink.head_();
        sink.body();

        sink.lineBreak();

        final Path indexHtmlFile = Paths.get(getReportDirectory(), "index.html");
        final String relativePath =
                Paths.get(reportingOutputDirectory).relativize(indexHtmlFile).toString();

        sink.rawText(String.format("<meta http-equiv=\"refresh\" content=\"0;url=%s\" />",
                relativePath));

        sink.link(relativePath);

        sink.body_();
        sink.flush();
        sink.close();
    }

    /**
     * Return ClassLoader with classpath elements.
     */
    protected ClassLoader createProjectClassLoader()
            throws MalformedURLException, DependencyResolutionRequiredException {
        final List<URL> result = new ArrayList<>();
        for (String element : project.getTestClasspathElements()) {
            if (element != null) {
                final URL url = Paths.get(element).toUri().toURL();
                result.add(url);
            }
        }
        return new URLClassLoader(result.toArray(new URL[0]));
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
    protected boolean isDirectoryExists(final Path path) {
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
