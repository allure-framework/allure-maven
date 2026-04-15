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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AllureVersionTest {

    @Test
    public void shouldUseAllure3AsDefaultVersion() {
        final AllureVersion version = AllureVersion.resolve(null);

        assertThat(version.isAllure3(), is(true));
        assertThat(version.getVersion(), is(AllureVersion.ALLURE3_DEFAULT_VERSION));
    }

    @Test
    public void shouldUseAllure3ForBlankVersion() {
        final AllureVersion version = AllureVersion.resolve("  ");

        assertThat(version.isAllure3(), is(true));
        assertThat(version.getVersion(), is(AllureVersion.ALLURE3_DEFAULT_VERSION));
    }

    @Test
    public void shouldRouteAllure2VersionsToAllure2Runtime() {
        final AllureVersion version = AllureVersion.resolve("2.39.0");

        assertThat(version.isAllure2(), is(true));
        assertThat(version.getVersion(), is("2.39.0"));
    }

    @Test
    public void shouldRouteAllure3PrereleaseVersionsToAllure3Runtime() {
        final AllureVersion version = AllureVersion.resolve("3.0.0-beta.23");

        assertThat(version.isAllure3(), is(true));
        assertThat(version.getVersion(), is("3.0.0-beta.23"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectUnsupportedMajorVersions() {
        AllureVersion.resolve("1.5.4");
    }
}
