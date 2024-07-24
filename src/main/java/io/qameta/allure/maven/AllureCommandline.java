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

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.Proxy;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolverException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static io.qameta.allure.maven.VersionUtils.versionCompare;

@SuppressWarnings({"ClassDataAbstractionCoupling", "ClassFanOutComplexity",
        "MultipleStringLiterals"})
public class AllureCommandline {

    public static final String ALLURE_DEFAULT_VERSION = "2.30.0";

    private static final int DEFAULT_TIMEOUT = 3600;

    private final String version;

    private final int timeout;

    private final Path installationDirectory;

    public AllureCommandline(final Path installationDirectory, final String version) {
        this(installationDirectory, version, DEFAULT_TIMEOUT);
    }

    public AllureCommandline(final Path installationDirectory, final String version,
            final int timeout) {
        this.installationDirectory = installationDirectory;
        this.version = StringUtils.isBlank(version) || versionCompare(version, "2.8.0") < 0
                ? ALLURE_DEFAULT_VERSION
                : version;
        this.timeout = timeout;
    }

    public int generateReport(final List<Path> resultsPaths, final Path reportPath)
            throws IOException {

        this.checkAllureExists();

        FileUtils.deleteQuietly(reportPath.toFile());

        final CommandLine commandLine =
                new CommandLine(getAllureExecutablePath().toAbsolutePath().toFile());
        commandLine.addArgument("generate");
        commandLine.addArgument("--clean");
        for (Path resultsPath : resultsPaths) {
            commandLine.addArgument(resultsPath.toAbsolutePath().toString(), true);
        }
        commandLine.addArgument("-o");
        commandLine.addArgument(reportPath.toAbsolutePath().toString(), true);

        return execute(commandLine, timeout);
    }

    public int serve(final List<Path> resultsPaths, final Path reportPath, final String serveHost,
            final Integer servePort) throws IOException {

        this.checkAllureExists();

        FileUtils.deleteQuietly(reportPath.toFile());

        final CommandLine commandLine =
                new CommandLine(getAllureExecutablePath().toAbsolutePath().toFile());
        commandLine.addArgument("serve");
        if (serveHost != null && serveHost.matches("(\\d{1,3}\\.){3}\\d{1,3}")) {
            commandLine.addArgument("--host");
            commandLine.addArgument(serveHost);
        }
        if (servePort > 0) {
            commandLine.addArgument("--port");
            commandLine.addArgument(Objects.toString(servePort));
        }
        for (Path resultsPath : resultsPaths) {
            commandLine.addArgument(resultsPath.toAbsolutePath().toString(), true);
        }
        return execute(commandLine, timeout);
    }

    private void checkAllureExists() throws FileNotFoundException {
        if (allureNotExists()) {
            throw new FileNotFoundException("There is no valid allure installation."
                    + " Make sure you're using allure version not less then 2.x.");
        }
    }

    private int execute(final CommandLine commandLine, final int timeout) throws IOException {
        final DefaultExecutor executor = DefaultExecutor.builder().get();
        final ExecuteWatchdog watchdog = ExecuteWatchdog.builder()
                .setTimeout(Duration.ofMillis(TimeUnit.SECONDS.toMillis(timeout))).get();
        executor.setWatchdog(watchdog);
        executor.setExitValue(0);
        return executor.execute(commandLine);
    }

    private Path getAllureExecutablePath() {
        final String allureExecutable = isWindows() ? "allure.bat" : "allure";
        return getAllureHome().resolve("bin").resolve(allureExecutable);
    }

    private Path getAllureHome() {
        return installationDirectory.resolve(String.format("allure-%s", version));
    }

    public boolean allureExists() {
        final Path allureExecutablePath = getAllureExecutablePath();
        return Files.exists(allureExecutablePath) && Files.isExecutable(allureExecutablePath);
    }

    public boolean allureNotExists() {
        return !allureExists();
    }

    public void downloadWithMaven(final MavenSession session,
            final DependencyResolver dependencyResolver) throws IOException {
        final ProjectBuildingRequest buildingRequest =
                new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        buildingRequest.setResolveDependencies(false);

        final Dependency cliDep = new Dependency();
        cliDep.setGroupId("io.qameta.allure");
        cliDep.setArtifactId("allure-commandline");
        cliDep.setVersion(version);
        cliDep.setType("zip");

        try {
            final Iterator<ArtifactResult> resolved =
                    dependencyResolver.resolveDependencies(buildingRequest,
                            Collections.singletonList(cliDep), null, null).iterator();

            if (resolved.hasNext()) {
                unpack(resolved.next().getArtifact().getFile());
            } else {
                throw new IOException("No allure commandline artifact found.");

            }
        } catch (DependencyResolverException e) {
            throw new IOException("Cannot resolve allure commandline dependencies.", e);
        }
    }

    public void download(final String allureDownloadUrl, final Proxy mavenProxy)
            throws IOException {
        if (allureExists()) {
            return;
        }

        final Path allureZip = Files.createTempFile("allure", version);
        final String allureUrl = String.format(allureDownloadUrl, version, version);
        final URL url = new URL(allureUrl);

        if (mavenProxy != null && version != null) {
            final InetSocketAddress proxyAddress =
                    new InetSocketAddress(mavenProxy.getHost(), mavenProxy.getPort());

            if (mavenProxy.getUsername() != null && mavenProxy.getPassword() != null) {
                final String proxyUser = mavenProxy.getUsername();
                final String proxyPassword = mavenProxy.getPassword();

                Authenticator.setDefault(new Authenticator() {
                    @Override
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                    }
                });
            }

            final java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, proxyAddress);
            final InputStream inputStream = url.openConnection(proxy).getInputStream();
            Files.copy(inputStream, allureZip, StandardCopyOption.REPLACE_EXISTING);
        } else {
            FileUtils.copyURLToFile(url, allureZip.toFile());
        }

        unpack(allureZip.toFile());
    }

    private void unpack(final File file) throws IOException {
        try (ZipFile zipFile = new ZipFile(file)) {
            zipFile.extractAll(getInstallationDirectory().toAbsolutePath().toString());
        } catch (ZipException e) {
            throw new IOException(e);
        }

        final Path allureExecutable = getAllureExecutablePath();
        if (Files.exists(allureExecutable)) {
            allureExecutable.toFile().setExecutable(true);
        }
    }

    public Path getInstallationDirectory() {
        return installationDirectory;
    }

    public String getVersion() {
        return version;
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
