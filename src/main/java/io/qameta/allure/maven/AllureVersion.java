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

import org.apache.commons.lang3.StringUtils;

/**
 * Resolved Allure report version and runtime family.
 */
public final class AllureVersion {

    public static final String ALLURE3_DEFAULT_VERSION = "3.4.1";

    private final String version;

    private final Runtime runtime;

    private AllureVersion(final String version, final Runtime runtime) {
        this.version = version;
        this.runtime = runtime;
    }

    public static AllureVersion resolve(final String requestedVersion) {
        if (StringUtils.isBlank(requestedVersion)) {
            return new AllureVersion(ALLURE3_DEFAULT_VERSION, Runtime.ALLURE3);
        }

        final String version = requestedVersion.trim();
        if (version.startsWith("2.")) {
            return new AllureVersion(version, Runtime.ALLURE2);
        }
        if (version.startsWith("3.")) {
            return new AllureVersion(version, Runtime.ALLURE3);
        }

        throw new IllegalArgumentException(
                String.format(
                        "Unsupported Allure report version '%s'. Use 2.x to keep Allure 2 "
                                + "or 3.x to use Allure 3.",
                        version
                )
        );
    }

    public String getVersion() {
        return version;
    }

    public boolean isAllure2() {
        return runtime == Runtime.ALLURE2;
    }

    public boolean isAllure3() {
        return runtime == Runtime.ALLURE3;
    }

    private enum Runtime {
        ALLURE2,
        ALLURE3
    }
}
