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
import java.nio.file.Paths;

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

    @Component
    private DependencyResolver dependencyResolver;

    @Override
    public void execute() throws MojoExecutionException {
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
            throw new MojoExecutionException("Can't install allure", e);
        }
    }

}
