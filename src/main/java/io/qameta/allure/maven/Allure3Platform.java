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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Node.js archive classifier for the current host.
 */
@SuppressWarnings("MultipleStringLiterals")
final class Allure3Platform {

    private static final String BIN_DIRECTORY = "bin";

    private final String classifier;

    private final String archiveExtension;

    private final boolean windows;

    private Allure3Platform(final String classifier, final String archiveExtension,
            final boolean windows) {
        this.classifier = classifier;
        this.archiveExtension = archiveExtension;
        this.windows = windows;
    }

    static Allure3Platform detect() {
        return detect(System.getProperty("os.name"), System.getProperty("os.arch"));
    }

    static Allure3Platform detect(final String osName, final String osArch) {
        final String os = normalizeOs(osName);
        final String arch = normalizeArch(osArch);
        final boolean isWindows = "win".equals(os);
        return new Allure3Platform(os + "-" + arch, isWindows ? "zip" : "tar.gz", isWindows);
    }

    private static String normalizeOs(final String osName) {
        final String name = osName.toLowerCase(Locale.ENGLISH);
        if (name.contains("win")) {
            return "win";
        }
        if (name.contains("mac") || name.contains("darwin")) {
            return "darwin";
        }
        if (name.contains("linux")) {
            return "linux";
        }
        throw new IllegalArgumentException("Unsupported operating system for Allure 3: " + osName);
    }

    private static String normalizeArch(final String osArch) {
        final String arch = osArch.toLowerCase(Locale.ENGLISH);
        if ("x86_64".equals(arch) || "amd64".equals(arch)) {
            return "x64";
        }
        if ("aarch64".equals(arch) || "arm64".equals(arch)) {
            return "arm64";
        }
        throw new IllegalArgumentException("Unsupported architecture for Allure 3: " + osArch);
    }

    String getArchiveFileName(final String nodeVersion) {
        return String.format("node-v%s-%s.%s", nodeVersion, classifier, archiveExtension);
    }

    Path getNodeHome(final Path installationDirectory, final String nodeVersion) {
        return installationDirectory.resolve(String.format("node-v%s-%s", nodeVersion, classifier));
    }

    Path getNodeExecutable(final Path installationDirectory, final String nodeVersion) {
        final Path nodeHome = getNodeHome(installationDirectory, nodeVersion);
        return windows ? nodeHome.resolve("node.exe")
                : nodeHome.resolve(BIN_DIRECTORY).resolve("node");
    }

    Path getNpmExecutable(final Path installationDirectory, final String nodeVersion) {
        final Path nodeHome = getNodeHome(installationDirectory, nodeVersion);
        return windows ? nodeHome.resolve("npm.cmd")
                : nodeHome.resolve(BIN_DIRECTORY).resolve("npm");
    }

    Path getNpmCliPath(final Path installationDirectory, final String nodeVersion) {
        final Path nodeHome = getNodeHome(installationDirectory, nodeVersion);
        final Path npmCliPath = Paths.get("node_modules", "npm", "bin", "npm-cli.js");
        return windows ? nodeHome.resolve(npmCliPath) : nodeHome.resolve("lib").resolve(npmCliPath);
    }

    Path getBinDirectory(final Path installationDirectory) {
        return installationDirectory.resolve(BIN_DIRECTORY);
    }

    Path getAllureExecutable(final Path installationDirectory) {
        return getBinDirectory(installationDirectory).resolve(windows ? "allure.bat" : "allure");
    }

    String getClassifier() {
        return classifier;
    }

    String getArchiveExtension() {
        return archiveExtension;
    }

    boolean isWindows() {
        return windows;
    }
}
