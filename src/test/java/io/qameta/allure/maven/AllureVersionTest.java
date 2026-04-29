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

import static io.qameta.allure.Allure.addAttachment;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
@Tag("versioning")
class AllureVersionTest {

    @Test
    void shouldUseAllure3AsDefaultVersion() {
        assertResolvedVersion(null, true, AllureVersion.ALLURE3_DEFAULT_VERSION);
    }

    @Test
    void shouldUseAllure3ForBlankVersion() {
        assertResolvedVersion("  ", true, AllureVersion.ALLURE3_DEFAULT_VERSION);
    }

    @Test
    void shouldRouteAllure2VersionsToAllure2Runtime() {
        assertResolvedVersion("2.39.0", false, "2.39.0");
    }

    @Test
    void shouldRouteAllure3PrereleaseVersionsToAllure3Runtime() {
        assertResolvedVersion("3.0.0-beta.23", true, "3.0.0-beta.23");
    }

    @Test
    void shouldRejectUnsupportedMajorVersions() {
        step("Reject unsupported major version 1.5.4", () -> {
            final IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> AllureVersion.resolve("1.5.4"));
            addAttachment("Unsupported version error", String.valueOf(error.getMessage()));
        });
    }

    private static void assertResolvedVersion(final String requestedVersion,
            final boolean expectedAllure3, final String expectedVersion) {
        final String requested = describeRequestedVersion(requestedVersion);
        final AllureVersion resolvedVersion = step("Resolve report version " + requested,
                () -> AllureVersion.resolve(requestedVersion));

        step("Verify selected runtime and normalized version", () -> {
            addAttachment("Version resolution",
                    String.join(System.lineSeparator(), "requested=" + requested,
                            "resolved=" + resolvedVersion.getVersion(),
                            "runtime=" + (resolvedVersion.isAllure3() ? "allure3" : "allure2")));
            assertThat(resolvedVersion.isAllure3()).isEqualTo(expectedAllure3);
            assertThat(resolvedVersion.isAllure2()).isEqualTo(!expectedAllure3);
            assertThat(resolvedVersion.getVersion()).isEqualTo(expectedVersion);
        });
    }

    private static String describeRequestedVersion(final String requestedVersion) {
        if (requestedVersion == null) {
            return "<default>";
        }
        if (requestedVersion.isBlank()) {
            return "<blank>";
        }
        return requestedVersion;
    }
}
