package ru.yandex.qatools.allure.report;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import ru.qatools.clay.aether.Aether;
import ru.qatools.clay.aether.AetherException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 31.07.15
 */
public abstract class AllureResolveMojo extends AllureBaseMojo {

    @Component
    protected RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    protected RepositorySystemSession repositorySession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    protected List<RemoteRepository> projectRepositories;

    /**
     * The version on Allure report to generate. 
     */
    @Parameter(property = "allure.version", defaultValue = "RELEASE")
    protected String version;

    /**
     * The list of report plugins for the report.
     */
    @Parameter
    protected List<Dependency> plugins = Collections.emptyList();

    /**
     * Creates an instance of aether to resolve bundle and plugins.
     */
    protected Aether createAether() {
        return Aether.aether(repositorySystem, repositorySession, projectRepositories);
    }

    /**
     * Resolve all artifacts {@link #getArtifactsToResolve()}
     *
     * @throws AetherException if any occurs.
     */
    protected ClassLoader resolve() throws AetherException {
        return createAether().resolveAll(getArtifactsToResolve()).getAsClassLoader();
    }

    /**
     * Get the list of artifacts to resolve. The list contains all plugins artifacts
     * and bundle artifact.
     */
    protected Artifact[] getArtifactsToResolve() {
        List<Artifact> artifacts = new ArrayList<>();
        for (Dependency plugin : plugins) {
            artifacts.add(convert(plugin));
        }
        artifacts.add(convert(getDefaultBundleDependency()));
        return artifacts.toArray(new Artifact[artifacts.size()]);
    }

    /**
     * Convert the given dependency to {@link Artifact}.
     */
    protected Artifact convert(Dependency dependency) {
        return new DefaultArtifact(
                dependency.getGroupId(), dependency.getArtifactId(),
                dependency.getClassifier(), dependency.getType(), dependency.getVersion()
        );
    }

    /**
     * Returns the default bundle dependency.
     */
    protected Dependency getDefaultBundleDependency() {
        Dependency dependency = new Dependency();
        dependency.setGroupId("ru.yandex.qatools.allure");
        dependency.setArtifactId("allure-bundle");
        dependency.setVersion(version);
        return dependency;
    }
}
