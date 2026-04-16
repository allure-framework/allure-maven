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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.settings.Proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Allure 3 commandline runner backed by a plugin-managed Node.js runtime.
 */
@SuppressWarnings({"PMD.GodClass", "ClassDataAbstractionCoupling", "ClassFanOutComplexity",
        "MultipleStringLiterals", "ParameterNumber", "PMD.CouplingBetweenObjects",
        "PMD.TooManyMethods", "PMD.CyclomaticComplexity", "PMD.ExcessiveParameterList"})
public class Allure3Commandline {

    public static final String NODE_DEFAULT_VERSION = "24.14.1";

    public static final String NODE_DEFAULT_DOWNLOAD_URL =
            "https://nodejs.org/dist/v%s/node-v%s-%s.%s";

    public static final String NPM_DEFAULT_REGISTRY = "https://registry.npmjs.org";

    private static final String NODE_CHECKSUM_URL = "https://nodejs.org/dist/v%s/SHASUMS256.txt";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new MapTypeReference();

    private final Path installationDirectory;

    private final String allureVersion;

    private final String nodeVersion;

    private final String nodeDownloadUrl;

    private final String npmRegistry;

    private final Path allurePackagePath;

    private final Proxy proxy;

    private final Properties downloadProperties;

    private final boolean offline;

    private final int timeout;

    private final Allure3Platform platform;

    public Allure3Commandline(final Path installationDirectory, final String allureVersion,
            final String nodeVersion, final String nodeDownloadUrl, final String npmRegistry,
            final Path allurePackagePath, final Proxy proxy, final Properties downloadProperties,
            final boolean offline, final int timeout) {
        this.installationDirectory = installationDirectory;
        this.allureVersion = allureVersion;
        this.nodeVersion = StringUtils.defaultIfBlank(nodeVersion, NODE_DEFAULT_VERSION);
        this.nodeDownloadUrl =
                StringUtils.defaultIfBlank(nodeDownloadUrl, NODE_DEFAULT_DOWNLOAD_URL);
        this.npmRegistry = StringUtils.defaultIfBlank(npmRegistry, NPM_DEFAULT_REGISTRY);
        this.allurePackagePath = allurePackagePath;
        this.proxy = proxy;
        this.downloadProperties = downloadProperties;
        this.offline = offline;
        this.timeout = timeout;
        this.platform = Allure3Platform.detect();
    }

    public void install() throws IOException {
        installNode();
        installAllure();
        ensureLaunchers();
    }

    public int generateReport(final List<Path> resultsPaths, final Path reportPath,
            final boolean singleFile, final Path buildDirectory, final String reportName,
            final Path userConfigPath) throws IOException {
        return generateReport(resultsPaths, reportPath, singleFile, buildDirectory, reportName,
                userConfigPath, Collections.<String, Object>emptyMap());
    }

    public int generateReport(final List<Path> resultsPaths, final Path reportPath,
            final boolean singleFile, final Path buildDirectory, final String reportName,
            final Path userConfigPath, final Map<String, Object> defaultConfig) throws IOException {
        checkAllureExists();
        ensureLaunchers();
        FileUtils.deleteQuietly(reportPath.toFile());

        final Path workDirectory = prepareWorkDirectory(buildDirectory);
        final Path config = writeConfig(workDirectory, reportPath, singleFile, reportName,
                userConfigPath, defaultConfig);

        return executeGenerate(resultsPaths, config);
    }

    public int serve(final List<Path> resultsPaths, final Path reportPath, final boolean singleFile,
            final Path buildDirectory, final String reportName, final Integer servePort,
            final Path userConfigPath) throws IOException {
        checkAllureExists();
        ensureLaunchers();
        FileUtils.deleteQuietly(reportPath.toFile());

        final Path workDirectory = prepareWorkDirectory(buildDirectory);
        final Path config = writeConfig(workDirectory, reportPath, singleFile, reportName,
                userConfigPath, Collections.<String, Object>emptyMap());

        executeGenerate(resultsPaths, config);
        return executeOpen(reportPath, config, servePort);
    }

    public boolean allureExists() {
        return Files.isExecutable(getNodeExecutable()) && Files.exists(getAllureCliPath());
    }

    public boolean allureNotExists() {
        return !allureExists();
    }

    Path getNodeExecutable() {
        return platform.getNodeExecutable(installationDirectory, nodeVersion);
    }

    Path getNpmExecutable() {
        return platform.getNpmExecutable(installationDirectory, nodeVersion);
    }

    Path getNpmCliPath() {
        return platform.getNpmCliPath(installationDirectory, nodeVersion);
    }

    Path getAllureHome() {
        return installationDirectory.resolve("allure-" + allureVersion);
    }

    Path getAllureExecutablePath() {
        return platform.getAllureExecutable(installationDirectory);
    }

    Path getAllureCliPath() {
        return getAllureHome().resolve("node_modules").resolve("allure").resolve("cli.js");
    }

    public String getVersion() {
        return allureVersion;
    }

    public String getNodeVersion() {
        return nodeVersion;
    }

    private void installNode() throws IOException {
        if (Files.isExecutable(getNodeExecutable())) {
            return;
        }
        if (offline) {
            throw new IOException(String.format(
                    "Cannot install Node.js %s for Allure 3 while Maven is offline. "
                            + "Pre-populate %s and rerun without offline mode to download it.",
                    nodeVersion, installationDirectory));
        }

        final String archiveFileName = platform.getArchiveFileName(nodeVersion);
        final URL archiveUrl = new URL(String.format(nodeDownloadUrl, nodeVersion, nodeVersion,
                platform.getClassifier(), platform.getArchiveExtension()));
        final URL checksumUrl = new URL(String.format(NODE_CHECKSUM_URL, nodeVersion));
        final String expectedChecksum = readChecksum(checksumUrl, archiveFileName);

        final Path archive = Files.createTempFile("node-" + nodeVersion, "-" + archiveFileName);
        try {
            AllureDownloadUtils.copy(archiveUrl, archive, proxy, downloadProperties);
            verifyChecksum(archive, expectedChecksum, archiveFileName);
            FileUtils.deleteQuietly(
                    platform.getNodeHome(installationDirectory, nodeVersion).toFile());
            unpackNode(archive.toFile());
        } finally {
            Files.deleteIfExists(archive);
        }

        getNodeExecutable().toFile().setExecutable(true);
        getNpmExecutable().toFile().setExecutable(true);
    }

    private void installAllure() throws IOException {
        if (allurePackagePath == null && Files.exists(getAllureCliPath())) {
            return;
        }
        if (allurePackagePath == null && offline) {
            throw new IOException(String.format(
                    "Cannot install allure@%s while Maven is offline. Pre-populate %s "
                            + "and rerun without offline mode to install it.",
                    allureVersion, getAllureHome()));
        }

        FileUtils.deleteQuietly(getAllureHome().toFile());
        Files.createDirectories(getAllureHome());
        writePackageJson();

        final Path installTarget = resolveInstallTarget();

        final CommandLine commandLine = new CommandLine(getNodeExecutable().toFile());
        addPathArgument(commandLine, getNpmCliPath());
        commandLine.addArgument("--prefix");
        addPathArgument(commandLine, getAllureHome());
        commandLine.addArgument("install");
        commandLine.addArgument("--no-package-lock");
        commandLine.addArgument("--no-save");
        commandLine.addArgument("--ignore-scripts");
        addInstallTarget(commandLine, installTarget);
        if (allurePackagePath == null) {
            commandLine.addArgument("--registry");
            commandLine.addArgument(npmRegistry);
            addProxyArguments(commandLine);
        }

        execute(commandLine, timeout);

        if (!Files.exists(getAllureCliPath())) {
            throw new IOException("Cannot find installed Allure 3 CLI at " + getAllureCliPath());
        }
    }

    private void writePackageJson() throws IOException {
        final Path packageJson = getAllureHome().resolve("package.json");
        Files.write(packageJson, Arrays.asList("{", "  \"name\": \"allure-maven-runtime\",",
                "  \"private\": true", "}"), StandardCharsets.UTF_8);
    }

    private Path resolveInstallTarget() {
        return allurePackagePath == null ? null : allurePackagePath.toAbsolutePath();
    }

    private void addInstallTarget(final CommandLine commandLine, final Path installTarget) {
        if (installTarget == null) {
            commandLine.addArgument("allure@" + allureVersion);
        } else {
            addPathArgument(commandLine, installTarget);
        }
    }

    private void addProxyArguments(final CommandLine commandLine) {
        final String proxyUrl = toProxyUrl(proxy);
        if (proxyUrl == null) {
            return;
        }
        commandLine.addArgument("--proxy");
        commandLine.addArgument(proxyUrl, false);
        commandLine.addArgument("--https-proxy");
        commandLine.addArgument(proxyUrl, false);
    }

    private static String toProxyUrl(final Proxy mavenProxy) {
        if (mavenProxy == null || !mavenProxy.isActive()) {
            return null;
        }
        final String protocol = StringUtils.defaultIfBlank(mavenProxy.getProtocol(), "http");
        final String credentials = StringUtils.isBlank(mavenProxy.getUsername()) ? ""
                : mavenProxy.getUsername() + ":"
                        + StringUtils.defaultString(mavenProxy.getPassword()) + "@";
        return String.format("%s://%s%s:%d", protocol, credentials, mavenProxy.getHost(),
                mavenProxy.getPort());
    }

    private String readChecksum(final URL checksumUrl, final String archiveFileName)
            throws IOException {
        final Path checksumFile = Files.createTempFile("node-" + nodeVersion, "-SHASUMS256.txt");
        try {
            AllureDownloadUtils.copy(checksumUrl, checksumFile, proxy, downloadProperties);
            try (BufferedReader reader = Files.newBufferedReader(checksumFile)) {
                final List<String> matches = reader.lines()
                        .filter(line -> line.endsWith("  " + archiveFileName)
                                || line.endsWith(" *" + archiveFileName))
                        .collect(Collectors.toList());
                if (matches.isEmpty()) {
                    throw new IOException(
                            "Cannot find checksum for Node.js archive " + archiveFileName);
                }
                return matches.get(0).split("\\s+")[0];
            }
        } finally {
            Files.deleteIfExists(checksumFile);
        }
    }

    private static void verifyChecksum(final Path file, final String expected,
            final String archiveFileName) throws IOException {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(Files.newInputStream(file), digest)) {
                final byte[] buffer = new byte[8192];
                int bytesRead = input.read(buffer);
                while (bytesRead >= 0) {
                    bytesRead = input.read(buffer);
                }
            }
            final String actual = toHex(digest.digest());
            if (!expected.equalsIgnoreCase(actual)) {
                throw new IOException(
                        String.format("Checksum mismatch for %s. Expected %s but got %s.",
                                archiveFileName, expected, actual));
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 is not available", e);
        }
    }

    private static String toHex(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private void unpackNode(final File file) throws IOException {
        if (platform.isWindows()) {
            try (ZipFile zipFile = new ZipFile(file)) {
                zipFile.extractAll(installationDirectory.toAbsolutePath().toString());
            } catch (ZipException e) {
                throw new IOException(e);
            }
            return;
        }

        try (InputStream input = Files.newInputStream(file.toPath());
                GzipCompressorInputStream gzip = new GzipCompressorInputStream(input);
                TarArchiveInputStream tar = new TarArchiveInputStream(gzip)) {
            TarArchiveEntry entry = tar.getNextEntry();
            while (entry != null) {
                unpackTarEntry(tar, entry);
                entry = tar.getNextEntry();
            }
        }
    }

    private void unpackTarEntry(final TarArchiveInputStream tar, final TarArchiveEntry entry)
            throws IOException {
        final Path target = installationDirectory.resolve(entry.getName()).normalize();
        if (!target.startsWith(installationDirectory.normalize())) {
            throw new IOException("Refusing to unpack archive entry outside "
                    + installationDirectory + ": " + entry.getName());
        }
        if (entry.isDirectory()) {
            Files.createDirectories(target);
        } else {
            Files.createDirectories(target.getParent());
            Files.copy(tar, target);
        }
    }

    private Path prepareWorkDirectory(final Path buildDirectory) throws IOException {
        final Path workDirectory = buildDirectory.resolve("allure-maven").resolve("allure3");
        FileUtils.deleteQuietly(workDirectory.toFile());
        Files.createDirectories(workDirectory);
        return workDirectory;
    }

    private Path writeConfig(final Path workDirectory, final Path reportPath,
            final boolean singleFile, final String reportName, final Path userConfigPath,
            final Map<String, Object> defaultConfig) throws IOException {
        if (isScriptConfig(userConfigPath)) {
            return writeScriptConfig(workDirectory, reportPath, singleFile, reportName,
                    userConfigPath, defaultConfig);
        }

        final Map<String, Object> config = readConfig(userConfigPath);
        applyDefaultConfig(config, defaultConfig);
        config.put("name", reportName);
        config.put("output", reportPath.toAbsolutePath().toString());

        final Map<String, Object> plugins = getOrCreateMap(config, "plugins", userConfigPath);
        final Map<String, Object> awesome = getOrCreateMap(plugins, "awesome", userConfigPath);
        final Map<String, Object> awesomeOptions =
                getOrCreateMap(awesome, "options", userConfigPath);
        awesomeOptions.put("singleFile", singleFile);

        final Path configPath = workDirectory.resolve("allurerc.json");
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), config);
        return configPath;
    }

    private Path writeScriptConfig(final Path workDirectory, final Path reportPath,
            final boolean singleFile, final String reportName, final Path userConfigPath,
            final Map<String, Object> defaultConfig) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final Path configPath = workDirectory.resolve("allurerc.mjs");
        final List<String> lines = new java.util.ArrayList<>(Arrays.asList(
                "import userConfig from " + mapper.writeValueAsString(
                        userConfigPath.toAbsolutePath().toUri().toString()) + ";",
                "", "const config = userConfig ?? {};",
                "const plugins = typeof config.plugins === \"object\" && config.plugins !== null",
                "  ? config.plugins", "  : {};",
                "const awesome = typeof plugins.awesome === \"object\" && plugins.awesome !== null",
                "  ? plugins.awesome", "  : {};",
                "const awesomeOptions = typeof awesome.options === \"object\" "
                        + "&& awesome.options !== null",
                "  ? awesome.options", "  : {};", "", "export default {", "  ...config,",
                "  name: " + mapper.writeValueAsString(reportName) + ",", "  output: "
                        + mapper.writeValueAsString(reportPath.toAbsolutePath().toString()) + ","));
        for (Map.Entry<String, Object> entry : defaultConfig.entrySet()) {
            lines.add("  " + entry.getKey() + ": config." + entry.getKey() + " ?? "
                    + mapper.writeValueAsString(entry.getValue()) + ",");
        }
        lines.addAll(Arrays.asList("  plugins: {", "    ...plugins,", "    awesome: {",
                "      ...awesome,", "      options: {", "        ...awesomeOptions,",
                "        singleFile: " + singleFile + ",", "      },", "    },", "  },", "};", ""));
        Files.write(configPath, lines, StandardCharsets.UTF_8);
        return configPath;
    }

    private void applyDefaultConfig(final Map<String, Object> config,
            final Map<String, Object> defaultConfig) {
        for (Map.Entry<String, Object> entry : defaultConfig.entrySet()) {
            if (!config.containsKey(entry.getKey())) {
                config.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private Map<String, Object> readConfig(final Path userConfigPath) throws IOException {
        if (userConfigPath == null) {
            return new LinkedHashMap<>();
        }

        final String fileName = userConfigPath.getFileName().toString().toLowerCase();
        final ObjectMapper mapper;
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            mapper = new ObjectMapper(new YAMLFactory());
        } else if (fileName.endsWith(".json")) {
            mapper = new ObjectMapper();
        } else {
            throw new IOException("Unsupported Allure 3 config file extension for " + userConfigPath
                    + ". Supported extensions: .json, .yml, .yaml");
        }

        final Map<String, Object> config = mapper.readValue(userConfigPath.toFile(), MAP_TYPE);
        return config == null ? new LinkedHashMap<>() : config;
    }

    private boolean isScriptConfig(final Path userConfigPath) {
        if (userConfigPath == null) {
            return false;
        }
        final String fileName = userConfigPath.getFileName().toString().toLowerCase();
        return fileName.endsWith(".js") || fileName.endsWith(".cjs") || fileName.endsWith(".mjs");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOrCreateMap(final Map<String, Object> parent, final String key,
            final Path userConfigPath) throws IOException {
        final Object value = parent.get(key);
        if (value == null) {
            final Map<String, Object> child = new LinkedHashMap<>();
            parent.put(key, child);
            return child;
        }
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        final String source = userConfigPath == null ? "generated config"
                : "user config " + userConfigPath.toAbsolutePath();
        throw new IOException(String
                .format("Invalid Allure 3 config in %s: '%s' must be an object.", source, key));
    }

    private int executeGenerate(final List<Path> resultsPaths, final Path config)
            throws IOException {
        final CommandLine commandLine = new CommandLine(getAllureExecutablePath().toFile());
        commandLine.addArgument("generate");
        addResultsArguments(commandLine, resultsPaths);
        commandLine.addArgument("--config");
        addPathArgument(commandLine, config);
        return execute(commandLine, timeout);
    }

    private int executeOpen(final Path reportPath, final Path config, final Integer port)
            throws IOException {
        final CommandLine commandLine = new CommandLine(getAllureExecutablePath().toFile());
        commandLine.addArgument("open");
        addPathArgument(commandLine, reportPath);
        commandLine.addArgument("--config");
        addPathArgument(commandLine, config);
        if (port != null && port > 0) {
            commandLine.addArgument("--port");
            commandLine.addArgument(Objects.toString(port));
        }
        return execute(commandLine, timeout);
    }

    private void checkAllureExists() throws IOException {
        if (allureNotExists()) {
            throw new IOException("There is no valid Allure 3 installation. "
                    + "Run allure:install first or allow the plugin to install allure@"
                    + allureVersion + ".");
        }
    }

    private void addResultsArguments(final CommandLine commandLine, final List<Path> resultsPaths) {
        for (Path resultsPath : resultsPaths) {
            addPathArgument(commandLine, resultsPath);
        }
    }

    private void ensureLaunchers() throws IOException {
        if (!Files.isExecutable(getNodeExecutable()) || !Files.exists(getAllureCliPath())) {
            return;
        }

        final Path launcher = getAllureExecutablePath();
        Files.createDirectories(launcher.getParent());
        if (platform.isWindows()) {
            Files.write(launcher, createWindowsLauncher().getBytes(StandardCharsets.UTF_8));
            return;
        }

        Files.write(launcher, createUnixLauncher().getBytes(StandardCharsets.UTF_8));
        launcher.toFile().setExecutable(true);
    }

    private String createUnixLauncher() {
        return new StringBuilder().append("#!/bin/sh\n")
                .append("SCRIPT_DIR=$(CDPATH= cd -- \"$(dirname \"$0\")\" && pwd)\n")
                .append("INSTALL_DIR=$(CDPATH= cd -- \"$SCRIPT_DIR/..\" && pwd)\n")
                .append("exec \"$INSTALL_DIR/")
                .append(platform.getNodeHome(installationDirectory, nodeVersion).getFileName())
                .append("/bin/node\" \"$INSTALL_DIR/allure-").append(allureVersion)
                .append("/node_modules/allure/cli.js\" \"$@\"\n").toString();
    }

    private String createWindowsLauncher() {
        return new StringBuilder().append("@echo off\r\n").append("\"%~dp0..\\")
                .append(platform.getNodeHome(installationDirectory, nodeVersion).getFileName())
                .append("\\node.exe\" \"%~dp0..\\allure-").append(allureVersion)
                .append("\\node_modules\\allure\\cli.js\" %*\r\n").toString();
    }

    private int execute(final CommandLine commandLine, final int timeout) throws IOException {
        final DefaultExecutor executor = DefaultExecutor.builder().get();
        final ExecuteWatchdog watchdog = ExecuteWatchdog.builder()
                .setTimeout(Duration.ofMillis(TimeUnit.SECONDS.toMillis(timeout))).get();
        executor.setWatchdog(watchdog);
        executor.setExitValue(0);
        return executor.execute(commandLine);
    }

    private void addPathArgument(final CommandLine commandLine, final Path path) {
        commandLine.addArgument(path.toAbsolutePath().toString(), platform.isWindows());
    }

    private static final class MapTypeReference extends TypeReference<Map<String, Object>> {
    }
}
