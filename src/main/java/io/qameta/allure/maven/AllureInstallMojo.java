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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;

import static io.qameta.allure.maven.Allure3Commandline.NODE_DEFAULT_DOWNLOAD_URL;
import static io.qameta.allure.maven.Allure3Commandline.NODE_DEFAULT_VERSION;
import static io.qameta.allure.maven.Allure3Commandline.NPM_DEFAULT_REGISTRY;

/**
 * Install allure tool.
 */
@SuppressWarnings({"unused", "MultipleStringLiterals"})
@Mojo(name = "install", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class AllureInstallMojo extends AbstractMojo {

    @Parameter(property = "report.version")
    private String reportVersion;

    @Parameter(property = "allure.download.url")
    private String allureDownloadUrl;

    @Parameter(property = "allure.install.directory", defaultValue = "${project.basedir}/.allure")
    private String installDirectory;

    @Parameter(property = "allure.node.version", defaultValue = NODE_DEFAULT_VERSION)
    private String nodeVersion;

    @Parameter(property = "allure.node.download.url", defaultValue = NODE_DEFAULT_DOWNLOAD_URL)
    private String nodeDownloadUrl;

    @Parameter(property = "allure.npm.registry", defaultValue = NPM_DEFAULT_REGISTRY)
    private String npmRegistry;

    @Parameter(property = "allure.package.path")
    private String packagePath;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private String projectDirectory;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    @Component
    private DependencyResolver dependencyResolver;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            final AllureVersion allureVersion = AllureVersion.resolve(reportVersion);
            if (allureVersion.isAllure3()) {
                installAllure3(allureVersion);
            } else {
                validateAllure2Configuration();
                installAllure2(allureVersion);
            }
        } catch (IOException | IllegalArgumentException e) {
            getLog().error("Installation error", e);
            throw new MojoExecutionException("Can't install allure", e);
        }
    }

    private void installAllure2(final AllureVersion allureVersion) throws IOException {
        final AllureCommandline commandline =
                new AllureCommandline(Paths.get(installDirectory), allureVersion.getVersion());
        getLog().info(String.format("Allure installation directory %s", installDirectory));
        getLog().info(String.format("Try to finding out allure %s", commandline.getVersion()));

        if (commandline.allureNotExists()) {
            if (StringUtils.isNotBlank(allureDownloadUrl)) {
                getLog().info("Downloading allure commandline from " + allureDownloadUrl);
                commandline.download(allureDownloadUrl, ProxyUtils.getProxy(session, decrypter),
                        AllureCommandline.getDownloadProperties(session));
                getLog().info("Downloading allure commandline complete");
            } else {
                commandline.downloadWithMaven(session, dependencyResolver);
            }
        }
    }

    private void installAllure3(final AllureVersion allureVersion) throws IOException {
        if (StringUtils.isNotBlank(allureDownloadUrl)) {
            throw new IOException("Parameter allure.download.url is only supported for Allure 2. "
                    + "Configure reportVersion 2.x to use it, or use allure.node.download.url "
                    + "for the Allure 3 Node.js runtime.");
        }

        final Allure3Commandline commandline = new Allure3Commandline(Paths.get(installDirectory),
                allureVersion.getVersion(), nodeVersion, nodeDownloadUrl, npmRegistry,
                resolveAllurePackagePathOrNull(), ProxyUtils.getProxy(session, decrypter),
                AllureCommandline.getDownloadProperties(session),
                session != null && session.isOffline(), 3600);
        getLog().info(String.format("Allure installation directory %s", installDirectory));
        getLog().info(String.format("Try to finding out allure %s using Node.js %s",
                commandline.getVersion(), commandline.getNodeVersion()));
        commandline.install();
    }

    private void validateAllure2Configuration() throws IOException {
        if (StringUtils.isNotBlank(packagePath)) {
            throw new IOException("Parameter allure.package.path is only supported for Allure 3. "
                    + "Configure reportVersion 3.x to use it.");
        }
    }

    private Path resolveAllurePackagePathOrNull() throws IOException {
        if (StringUtils.isBlank(packagePath)) {
            return null;
        }

        Path resolvedPath = Paths.get(packagePath);
        if (!resolvedPath.isAbsolute()) {
            final Path baseDirectory =
                    StringUtils.isBlank(projectDirectory) ? Paths.get("").toAbsolutePath()
                            : Paths.get(projectDirectory);
            resolvedPath = baseDirectory.resolve(resolvedPath).normalize();
        }
        if (!Files.exists(resolvedPath)) {
            throw new IOException(
                    "Configured Allure 3 package archive does not exist: " + resolvedPath);
        }
        if (!Files.isRegularFile(resolvedPath)) {
            throw new IOException(
                    "Configured Allure 3 package archive is not a file: " + resolvedPath);
        }
        final String fileName = resolvedPath.getFileName().toString().toLowerCase(Locale.ENGLISH);
        if (!(fileName.endsWith(".tgz") || fileName.endsWith(".tar.gz"))) {
            throw new IOException("Configured Allure 3 package archive must be a .tgz or "
                    + ".tar.gz file: " + resolvedPath);
        }
        getLog().info("Using Allure 3 package archive " + resolvedPath.toAbsolutePath());
        return resolvedPath;
    }

}
