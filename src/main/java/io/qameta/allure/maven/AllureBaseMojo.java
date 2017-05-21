package io.qameta.allure.maven;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;

import java.util.Locale;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 30.07.15
 */
public abstract class AllureBaseMojo extends AbstractMavenReport {

    @Component
    protected PluginDescriptor pluginDescriptor;

    @Component
    protected Renderer siteRenderer;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

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
    public String getName(Locale locale) {
        return "Allure";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription(Locale locale) {
        return "Extended report on the test results of the project.";
    }
}
