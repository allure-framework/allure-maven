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

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;

import java.util.Locale;

/**
 * @author Dmitry Baev dmitry.baev@qameta.io Date: 30.07.15
 */
public abstract class AllureBaseMojo extends AbstractMavenReport {

    @Parameter(defaultValue = "${plugin}", readonly = true)
    protected PluginDescriptor pluginDescriptor;

    /**
     * {@inheritDoc}
     */
    @Override
    protected Renderer getSiteRenderer() {
        return siteRenderer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MavenProject getProject() {
        return project;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutputName() {
        return pluginDescriptor.getArtifactId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName(final Locale locale) {
        return "Allure";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription(final Locale locale) {
        return "Extended report on the test results of the project.";
    }
}
