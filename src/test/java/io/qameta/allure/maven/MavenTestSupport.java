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
import io.qameta.allure.AttachmentOptions;
import org.apache.maven.executor.ExecutorRequest;
import org.apache.maven.executor.ExecutorResult;
import org.apache.maven.executor.forked.ForkedMavenExecutor;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

abstract class MavenTestSupport {

    private static final String BUILD_LOG = "build.log";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern ANSI_ESCAPE_PATTERN = Pattern.compile("\u001B\\[[;\\d]*[ -/]*[@-~]");

    @TempDir
    Path tempDir;

    record ProjectFile(String resourceName, String targetPath) {
    }

    protected final ProjectFile resource(final String resourceName, final String targetPath) {
        return new ProjectFile(resourceName, targetPath);
    }

    protected final Path prepareProject(final String directoryName, final ProjectFile... files)
            throws IOException {
        return prepareProject(directoryName, Map.of(), files);
    }

    protected final Path prepareProject(final String directoryName,
                                        final Map<String, String> replacements, final ProjectFile... files)
            throws IOException {
        return Allure.step("Prepare Maven project " + directoryName, () -> {
            final Path projectDirectory = tempDir.resolve(directoryName);
            Files.createDirectories(projectDirectory);

            for (ProjectFile file : files) {
                copyResourceFile(
                        file.resourceName(),
                        projectDirectory.resolve(file.targetPath())
                );
            }

            filterPomFiles(projectDirectory, replacements);
            attachPreparedProject(projectDirectory);
            return projectDirectory;
        });
    }

    protected final ExecutorResult runGoals(final Path projectDirectory, final List<String> goals)
            throws Exception {
        return runGoals(projectDirectory, goals, List.of());
    }

    protected final ExecutorResult runGoals(final Path projectDirectory, final List<String> goals,
                                            final List<String> cliOptions)
            throws Exception {
        return Allure.step("Run Maven goals " + goals, () -> {
            final ExecutorResult result = executeMaven(projectDirectory, goals, cliOptions);
            assertThat(result.exitCode()).as("Maven goals %s should succeed; see %s", goals, BUILD_LOG)
                    .contains(0);
            assertThat(readLines(projectDirectory.resolve(BUILD_LOG)))
                    .as("Maven build log should be free of errors")
                    .noneMatch(line -> ANSI_ESCAPE_PATTERN.matcher(line).replaceAll("").contains("[ERROR]"));
            return result;
        });
    }

    protected final ExecutorResult expectFailure(final Path projectDirectory,
                                                 final List<String> goals, final List<String> cliOptions)
            throws Exception {
        return Allure.step("Expect Maven goals " + goals + " to fail", () -> {
            final ExecutorResult result = executeMaven(projectDirectory, goals, cliOptions);
            assertThat(result.success()).as("Expected Maven goals to fail: %s", goals).isFalse();
            return result;
        });
    }

    protected final String pluginGoal(final String goal) {
        return "%s:%s:%s:%s".formatted(
                requiredProperty("project.groupId"),
                requiredProperty("project.artifactId"), requiredProperty("project.version"), goal
        );
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
                                                  final int expectedCount)
            throws IOException {
        final JsonNode categories = readJson(outputDirectory.resolve(Path.of("data", "categories.json")));
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
                                         final String extension)
            throws IOException {
        if (Files.exists(file)) {
            Allure.attachment(
                    name, type, Files.readString(file, StandardCharsets.UTF_8),
                    AttachmentOptions.withFileExtension(extension)
            );
        }
    }

    protected final Path canonical(final Path path) throws IOException {
        return path.toRealPath();
    }

    protected final void assertCommandLines(final List<String> actual,
                                            final List<String> expected) {
        assertThat(actual).hasSize(expected.size());
        for (int index = 0; index < expected.size(); index++) {
            assertCommandLine(index + 1, actual.get(index), expected.get(index));
        }
    }

    protected final void assertCommandLinesContain(final List<String> actual,
                                                   final String expected) {
        assertThat(
                actual.stream()
                        .anyMatch(line -> line.equals(expected) || areSamePathLine(line, expected))
        )
                .as("captured command lines should contain <%s>", expected).isTrue();
    }

    protected final void assertSamePath(final String actual, final Path expected) {
        assertThat(isSameFile(Path.of(actual), expected))
                .as("Expected path <%s> to point to <%s>", actual, expected).isTrue();
    }

    private void attachPreparedProject(final Path projectDirectory) throws IOException {
        try (Stream<Path> paths = Files.walk(projectDirectory)) {
            final List<Path> files = paths.filter(Files::isRegularFile).sorted().toList();
            Allure.attachment(
                    "Prepared project files", "text/plain",
                    String.join(
                            System.lineSeparator(),
                            files.stream().map(projectDirectory::relativize).map(Path::toString)
                                    .map(this::normalizeSeparators).toList()
                    ),
                    AttachmentOptions.withFileExtension(".txt")
            );
            for (Path file : files) {
                attachPreparedProjectFile(projectDirectory, file);
            }
        }
    }

    private void assertCommandLine(final int lineNumber, final String actual,
                                   final String expected) {
        if (actual.equals(expected) || areSamePathLine(actual, expected)) {
            return;
        }

        assertThat(actual).as("captured command line %s", lineNumber).isEqualTo(expected);
    }

    private boolean areSamePathLine(final String actual, final String expected) {
        for (String prefix : List.of("arg=", "cli=")) {
            if (actual.startsWith(prefix) && expected.startsWith(prefix)) {
                return areSameFileValues(
                        actual.substring(prefix.length()),
                        expected.substring(prefix.length())
                );
            }
        }
        return false;
    }

    private boolean areSameFileValues(final String actual, final String expected) {
        try {
            return isSameFile(Path.of(actual), Path.of(expected));
        } catch (InvalidPathException error) {
            return false;
        }
    }

    private boolean isSameFile(final Path actual, final Path expected) {
        try {
            return Files.isSameFile(actual, expected);
        } catch (IOException error) {
            return false;
        }
    }

    private void attachPreparedProjectFile(final Path projectDirectory, final Path file)
            throws IOException {
        final String relativePath = normalizeSeparators(projectDirectory.relativize(file).toString());
        Allure.attachment(
                "Project file: " + relativePath, mediaType(file),
                Files.readString(file, StandardCharsets.UTF_8),
                AttachmentOptions.withFileExtension(fileExtension(relativePath))
        );
    }

    private void attachCliOptions(final List<String> cliOptions) {
        if (!cliOptions.isEmpty()) {
            Allure.attachment(
                    "Maven CLI options", "text/plain",
                    String.join(System.lineSeparator(), cliOptions), AttachmentOptions.withFileExtension(".txt")
            );
        }
    }

    private ExecutorResult executeMaven(final Path projectDirectory, final List<String> goals,
                                        final List<String> cliOptions)
            throws IOException {
        final List<String> arguments = new ArrayList<>(
                List.of(
                        "--batch-mode", "--errors", "-Dstyle.color=never",
                        "-Dmaven.repo.local=" + absoluteLocalRepository()
                )
        );
        arguments.addAll(cliOptions);
        arguments.addAll(goals);
        attachCliOptions(arguments);

        // Keep partial output available even when execution throws before returning a result.
        final ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
        final ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
        final ExecutorRequest request = ExecutorRequest.mavenBuilder()
                .cwd(projectDirectory)
                .arguments(arguments)
                .stdOut(stdOut)
                .stdErr(stdErr)
                .build();
        try (ForkedMavenExecutor executor = new ForkedMavenExecutor(Path.of(requiredProperty("maven.home")))) {
            return executor.execute(request);
        } finally {
            final Path buildLog = projectDirectory.resolve(BUILD_LOG);
            Files.writeString(
                    buildLog, String.join(
                            System.lineSeparator(),
                            stdOut.toString(StandardCharsets.UTF_8), stdErr.toString(StandardCharsets.UTF_8)
                    ),
                    StandardCharsets.UTF_8
            );
            attachIfPresent(buildLog, "Maven build log", "text/plain", ".txt");
        }
    }

    private void copyResourceFile(final String resourcePath, final Path targetFile)
            throws IOException {
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }

        try (InputStream stream = MavenTestSupport.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing project resource: " + resourcePath
                );
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
