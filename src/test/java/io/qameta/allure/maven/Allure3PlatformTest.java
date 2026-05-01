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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@Tag("allure3")
@Tag("platform")
class Allure3PlatformTest {

    @Test
    void shouldResolveMacArm64ArchiveName() {
        final Allure3Platform platform = Allure3Platform.detect("Mac OS X", "aarch64");

        assertThat(platform.getArchiveFileName("24.14.1"))
                .isEqualTo("node-v24.14.1-darwin-arm64.tar.gz");
    }

    @Test
    void shouldResolveLinuxX64ArchiveName() {
        final Allure3Platform platform = Allure3Platform.detect("Linux", "amd64");

        assertThat(platform.getArchiveFileName("24.14.1"))
                .isEqualTo("node-v24.14.1-linux-x64.tar.gz");
    }

    @Test
    void shouldResolveWindowsX64ArchiveName() {
        final Allure3Platform platform = Allure3Platform.detect("Windows 11", "x86_64");

        assertThat(platform.getArchiveFileName("24.14.1")).isEqualTo("node-v24.14.1-win-x64.zip");
    }

    @Test
    void shouldResolveUnixLauncherPath() {
        final Allure3Platform platform = Allure3Platform.detect("Linux", "amd64");

        assertThat(platform.getAllureExecutable(Paths.get("/tmp/allure")))
                .isEqualTo(Paths.get("/tmp/allure", "bin", "allure"));
    }

    @Test
    void shouldResolveWindowsLauncherPath() {
        final Allure3Platform platform = Allure3Platform.detect("Windows 11", "x86_64");

        assertThat(platform.getAllureExecutable(Paths.get("C:\\allure")))
                .isEqualTo(Paths.get("C:\\allure", "bin", "allure.bat"));
    }
}
