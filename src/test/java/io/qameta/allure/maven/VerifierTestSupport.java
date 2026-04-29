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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

abstract class VerifierTestSupport {

    private static final String BUILD_LOG = "build.log";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TEMPLATE_ROOT = "/verifier/";
    private static final String TESTDATA_ROOT = "/verifier-data/";

    @TempDir
    Path tempDir;

    record TemplateFile(String resourceName, String relativePath) {
    }

    protected final TemplateFile rootPom(final String scenario) {
        return new TemplateFile(scenario + "-pom.xml", "pom.xml");
    }

    protected final TemplateFile modulePom(final String scenario, final String module) {
        return new TemplateFile(scenario + "-" + module + "-pom.xml",
                Path.of(module, "pom.xml").toString());
    }

    protected final Path prepareProject(final String scenario, final TemplateFile... templates)
            throws IOException {
        return prepareProject(scenario, Map.of(), templates);
    }

    protected final Path prepareProject(final String scenario, final String directoryName,
            final TemplateFile... templates) throws IOException {
        return prepareProject(scenario, directoryName, Map.of(), templates);
    }

    protected final Path prepareProject(final String scenario,
            final Map<String, String> replacements, final TemplateFile... templates)
            throws IOException {
        return prepareProject(scenario, scenario, replacements, templates);
    }

    protected final Path prepareProject(final String scenario, final String directoryName,
            final Map<String, String> replacements, final TemplateFile... templates)
            throws IOException {
        return Allure.step("Prepare verifier project " + scenario, () -> {
            final Path projectDirectory = tempDir.resolve(directoryName);
            Files.createDirectories(projectDirectory);
            copyScenarioData(scenario, projectDirectory);

            for (TemplateFile template : templates) {
                copyResourceFile(TEMPLATE_ROOT + template.resourceName(),
                        projectDirectory.resolve(template.relativePath()));
            }

            filterPomFiles(projectDirectory, replacements);
            attachPreparedProject(projectDirectory);
            return projectDirectory;
        });
    }

    protected final Verifier runGoals(final Path projectDirectory, final List<String> goals)
            throws Exception {
        return runGoals(projectDirectory, goals, List.of());
    }

    protected final Verifier runGoals(final Path projectDirectory, final List<String> goals,
            final List<String> cliOptions) throws Exception {
        return Allure.step("Run verifier goals " + goals, () -> {
            final Verifier verifier = createVerifier(projectDirectory, cliOptions);
            attachCliOptions(cliOptions);
            try {
                verifier.executeGoals(goals);
                verifier.verifyErrorFreeLog();
            } catch (VerificationException error) {
                attachIfPresent(projectDirectory.resolve(verifier.getLogFileName()),
                        "Verifier build log", "text/plain", ".txt");
                throw error;
            }

            attachIfPresent(projectDirectory.resolve(verifier.getLogFileName()),
                    "Verifier build log", "text/plain", ".txt");
            return verifier;
        });
    }

    protected final VerificationException expectFailure(final Path projectDirectory,
            final List<String> goals, final List<String> cliOptions) throws Exception {
        return Allure.step("Expect verifier goals " + goals + " to fail", () -> {
            final Verifier verifier = createVerifier(projectDirectory, cliOptions);
            attachCliOptions(cliOptions);
            try {
                verifier.executeGoals(goals);
            } catch (VerificationException error) {
                attachIfPresent(projectDirectory.resolve(verifier.getLogFileName()),
                        "Verifier build log", "text/plain", ".txt");
                return error;
            }

            attachIfPresent(projectDirectory.resolve(verifier.getLogFileName()),
                    "Verifier build log", "text/plain", ".txt");
            throw new AssertionError("Expected verifier goals to fail: " + goals);
        });
    }

    protected final String pluginGoal(final String goal) {
        return "%s:%s:%s:%s".formatted(requiredProperty("project.groupId"),
                requiredProperty("project.artifactId"), requiredProperty("project.version"), goal);
    }

    protected final List<String> readLines(final Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    }

    protected final JsonNode readJson(final Path file) throws IOException {
        return OBJECT_MAPPER.readTree(file.toFile());
    }

    protected final JsonNode readFirstTestCase(final Path outputDirectory) throws IOException {
        final Path testCasesDirectory = outputDirectory.resolve(Path.of("data", "test-cases"));
        final String testCaseFile = TestHelper.getTestCases(testCasesDirectory).get(0);
        return readJson(testCasesDirectory.resolve(testCaseFile));
    }

    protected final void assertIssueLink(final Path outputDirectory) throws IOException {
        final JsonNode links = readFirstTestCase(outputDirectory).path("links");
        assertThat(links.isArray()).isTrue();
        assertThat(links).hasSize(1);
        assertThat(links.get(0).path("name").asText()).isEqualTo("issue-123");
        assertThat(links.get(0).path("url").asText()).isEqualTo("http://example.com/issue-123");
    }

    protected final void assertCategoriesChildren(final Path outputDirectory,
            final int expectedCount) throws IOException {
        final JsonNode categories =
                readJson(outputDirectory.resolve(Path.of("data", "categories.json")));
        final JsonNode root = categories.isArray() ? categories.get(0) : categories;
        assertThat(root.path("children")).hasSize(expectedCount);
    }

    protected final Path absoluteLocalRepository() {
        final String localRepository = requiredProperty("maven.repo.local");
        final Path localRepositoryPath = Path.of(localRepository);
        if (localRepositoryPath.isAbsolute()) {
            return localRepositoryPath;
        }
        return Path.of(requiredProperty("project.basedir")).resolve(localRepositoryPath)
                .normalize();
    }

    protected final String requiredProperty(final String name) {
        final String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required system property: " + name);
        }
        return value;
    }

    protected final void attachIfPresent(final Path file, final String name, final String type,
            final String extension) throws IOException {
        if (Files.exists(file)) {
            Allure.addAttachment(name, type, Files.readString(file, StandardCharsets.UTF_8),
                    extension);
        }
    }

    protected final Path canonical(final Path path) throws IOException {
        return path.toRealPath();
    }

    private void attachPreparedProject(final Path projectDirectory) throws IOException {
        try (Stream<Path> paths = Files.walk(projectDirectory)) {
            final List<Path> files = paths.filter(Files::isRegularFile).sorted().toList();
            Allure.addAttachment("Prepared project files", "text/plain",
                    String.join(System.lineSeparator(),
                            files.stream().map(projectDirectory::relativize).map(Path::toString)
                                    .map(this::normalizeSeparators).toList()),
                    ".txt");
            for (Path file : files) {
                attachPreparedProjectFile(projectDirectory, file);
            }
        }
    }

    private void attachPreparedProjectFile(final Path projectDirectory, final Path file)
            throws IOException {
        final String relativePath =
                normalizeSeparators(projectDirectory.relativize(file).toString());
        Allure.addAttachment("Project file: " + relativePath, mediaType(file),
                Files.readString(file, StandardCharsets.UTF_8), fileExtension(relativePath));
    }

    private void attachCliOptions(final List<String> cliOptions) {
        if (!cliOptions.isEmpty()) {
            Allure.addAttachment("Verifier CLI options", "text/plain",
                    String.join(System.lineSeparator(), cliOptions), ".txt");
        }
    }

    private Verifier createVerifier(final Path projectDirectory, final List<String> cliOptions)
            throws VerificationException {
        final Verifier verifier = new Verifier(projectDirectory.toString());
        verifier.setAutoclean(false);
        verifier.setForkJvm(true);
        verifier.setLogFileName(BUILD_LOG);
        verifier.setLocalRepo(absoluteLocalRepository().toString());
        cliOptions.forEach(verifier::addCliOption);
        return verifier;
    }

    private void copyScenarioData(final String scenario, final Path projectDirectory)
            throws IOException {
        final URL resource = VerifierTestSupport.class.getResource(TESTDATA_ROOT + scenario);
        if (resource == null) {
            return;
        }

        final Path sourceDirectory;
        try {
            sourceDirectory = Path.of(resource.toURI());
        } catch (URISyntaxException error) {
            throw new IOException("Could not read verifier test data for " + scenario, error);
        }

        try (Stream<Path> paths = Files.walk(sourceDirectory)) {
            paths.forEach(source -> copyPath(sourceDirectory, projectDirectory, source));
        }
    }

    private void copyResourceFile(final String resourcePath, final Path targetFile)
            throws IOException {
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }

        try (InputStream stream = VerifierTestSupport.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing verifier template resource: " + resourcePath);
            }
            Files.copy(stream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void filterPomFiles(final Path projectDirectory, final Map<String, String> replacements)
            throws IOException {
        final Map<String, String> tokens = new HashMap<>();
        tokens.put("@project.groupId@", requiredProperty("project.groupId"));
        tokens.put("@project.artifactId@", requiredProperty("project.artifactId"));
        tokens.put("@project.version@", requiredProperty("project.version"));
        tokens.putAll(replacements);

        try (Stream<Path> paths = Files.walk(projectDirectory)) {
            for (Path pomFile : paths
                    .filter(path -> path.getFileName().toString().equals("pom.xml")).toList()) {
                String content = Files.readString(pomFile, StandardCharsets.UTF_8);
                for (Map.Entry<String, String> entry : tokens.entrySet()) {
                    content = content.replace(entry.getKey(), entry.getValue());
                }
                Files.writeString(pomFile, content, StandardCharsets.UTF_8);
            }
        }
    }

    private void copyPath(final Path sourceDirectory, final Path projectDirectory,
            final Path source) {
        try {
            final Path relative = sourceDirectory.relativize(source);
            final Path target = projectDirectory.resolve(relative.toString());
            if (Files.isDirectory(source)) {
                Files.createDirectories(target);
                return;
            }

            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            throw new IllegalStateException("Could not copy verifier test data " + source, error);
        }
    }

    private String normalizeSeparators(final String path) {
        return path.replace('\\', '/');
    }

    private String mediaType(final Path file) {
        final String fileName = file.getFileName().toString();
        if (fileName.endsWith(".json")) {
            return "application/json";
        }
        if (fileName.endsWith(".xml") || fileName.endsWith(".pom")) {
            return "application/xml";
        }
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            return "application/yaml";
        }
        if (fileName.endsWith(".properties")) {
            return "text/x-java-properties";
        }
        return "text/plain";
    }

    private String fileExtension(final String path) {
        final int dotIndex = path.lastIndexOf('.');
        if (dotIndex < 0) {
            return ".txt";
        }
        return path.substring(dotIndex);
    }
}
