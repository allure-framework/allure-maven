/*
 *  Copyright 2021 Qameta Software OÜ
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

/**
 * @author eroshenkoam (Artem Eroshenko).
 */
public final class DownloadUtils {

    private static final String BINTRAY_TEMPLATE =
            "https://dl.bintray.com/qameta/generic/io/qameta/allure/allure/%s/allure-%s.zip";

    private static final String BY_DOT = "\\.";

    private DownloadUtils() {
        throw new IllegalStateException("do not instance");
    }

    /**
     * This helper returns the canonical download URL for the allure CLI if none was specified. It
     * returns null if the version is new enough to be available from Maven Central.
     */
    public static String getAllureDownloadUrl(final String version, final String downloadUrl) {
        if (downloadUrl != null) {
            return downloadUrl;
        }
        if (versionCompare(version, "2.8.0") < 0) {
            return BINTRAY_TEMPLATE;
        }
        return null;
    }

    private static Integer versionCompare(final String first, final String second) {
        final String[] firstVersions = first.split(BY_DOT);
        final String[] secondVersions = second.split(BY_DOT);
        int i = 0;
        while (i < firstVersions.length && i < secondVersions.length
                && firstVersions[i].equals(secondVersions[i])) {
            i++;
        }
        if (i < firstVersions.length && i < secondVersions.length) {
            final int diff =
                    Integer.valueOf(firstVersions[i]).compareTo(Integer.valueOf(secondVersions[i]));
            return Integer.signum(diff);
        } else {
            return Integer.signum(firstVersions.length - secondVersions.length);
        }
    }

}
