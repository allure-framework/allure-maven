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

import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.matchers.nio.PathMatchers.exists;
import static ru.yandex.qatools.matchers.nio.PathMatchers.isDirectory;

/**
 * @author Dmitry Baev dmitry.baev@qameta.io Date: 05.08.15
 */
@SuppressWarnings("unused")
public final class TestHelper {

    public static final List<String> FILE_NAMES = Arrays.asList("behaviors.json", "categories.json",
            "packages.json", "timeline.json", "suites.json");

    TestHelper() {}

    public static void checkReportDirectory(Path outputDirectory, int testCasesCount) {
        Path index = outputDirectory.resolve("index.html");
        assertThat(index, exists());

        Path dataDirectory = outputDirectory.resolve("data");

        assertThat(dataDirectory, isDirectory());

        for (String fileName : FILE_NAMES) {
            assertThat(dataDirectory.resolve(fileName), exists());
        }

        assertThat("There is not enough test case files in " + dataDirectory + " directory.",
                getTestCases(dataDirectory.resolve("test-cases")), hasSize(testCasesCount));
    }

    public static void checkSingleFile(Path outputDirectory) {
        Path index = outputDirectory.resolve("index.html");
        assertThat(index, exists());

        Path dataDirectory = outputDirectory.resolve("data");
        assertThat(dataDirectory, not(exists()));

        for (String fileName : FILE_NAMES) {
            assertThat(dataDirectory.resolve(fileName), not(exists()));
        }
    }

    public static List<String> getTestCases(Path dataDirectory) {
        return Arrays.asList(dataDirectory.toFile().list(new WildcardFileFilter("*.json")));
    }

    public static void checkAggregateMojoOnly(Path projectDirectory) throws IOException {
        final Path buildLog = projectDirectory.resolve("build.log");
        assertThat(buildLog, exists());

        final String content = Files.readString(buildLog, StandardCharsets.UTF_8);
        assertThat(content, containsString("Generate Allure report (aggregate)"));
        assertThat(content, not(containsString("Generate Allure report (report)")));
        assertThat(content, not(containsString("Generate Allure report to ")));
        assertThat(countMatches(content, "^\\[INFO\\] Generate report to ", Pattern.MULTILINE),
                is(1));
    }

    public static void checkRegularReportMojoOnly(Path projectDirectory) throws IOException {
        final Path buildLog = projectDirectory.resolve("build.log");
        assertThat(buildLog, exists());

        final String content = Files.readString(buildLog, StandardCharsets.UTF_8);
        assertThat(content, containsString("Generate Allure report (report)"));
        assertThat(content, not(containsString("Generate Allure report (aggregate)")));
        assertThat(content, not(containsString("Generate Allure report to ")));
        assertThat(countMatches(content,
                "^\\[INFO\\] Generate Allure report \\(report\\) with version ", Pattern.MULTILINE),
                is(1));
        assertThat(countMatches(content, "^\\[INFO\\] Generate report to ", Pattern.MULTILINE),
                is(1));
    }

    private static int countMatches(final String content, final String pattern, final int flags) {
        final Matcher matcher = Pattern.compile(pattern, flags).matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
