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

import org.junit.Test;

import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class Allure3PlatformTest {

    @Test
    public void shouldResolveMacArm64ArchiveName() {
        final Allure3Platform platform = Allure3Platform.detect("Mac OS X", "aarch64");

        assertThat(platform.getArchiveFileName("24.14.1"), is("node-v24.14.1-darwin-arm64.tar.gz"));
    }

    @Test
    public void shouldResolveLinuxX64ArchiveName() {
        final Allure3Platform platform = Allure3Platform.detect("Linux", "amd64");

        assertThat(platform.getArchiveFileName("24.14.1"), is("node-v24.14.1-linux-x64.tar.gz"));
    }

    @Test
    public void shouldResolveWindowsX64ArchiveName() {
        final Allure3Platform platform = Allure3Platform.detect("Windows 11", "x86_64");

        assertThat(platform.getArchiveFileName("24.14.1"), is("node-v24.14.1-win-x64.zip"));
    }

    @Test
    public void shouldResolveUnixLauncherPath() {
        final Allure3Platform platform = Allure3Platform.detect("Linux", "amd64");

        assertThat(platform.getAllureExecutable(Paths.get("/tmp/allure")),
                is(Paths.get("/tmp/allure", "bin", "allure")));
    }

    @Test
    public void shouldResolveWindowsLauncherPath() {
        final Allure3Platform platform = Allure3Platform.detect("Windows 11", "x86_64");

        assertThat(platform.getAllureExecutable(Paths.get("C:\\allure")),
                is(Paths.get("C:\\allure", "bin", "allure.bat")));
    }
}
