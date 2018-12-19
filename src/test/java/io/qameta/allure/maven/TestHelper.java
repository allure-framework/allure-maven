package io.qameta.allure.maven;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.qatools.matchers.nio.PathMatchers.exists;
import static ru.yandex.qatools.matchers.nio.PathMatchers.isDirectory;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 * Date: 05.08.15
 */
@SuppressWarnings("unused")
public final class TestHelper {

    public static final List<String> FILE_NAMES = Arrays.asList(
            "behaviors.json", "categories.json", "packages.json",
            "timeline.json", "suites.json"
    );

    TestHelper() {
    }

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

    public static List<String> getTestCases(Path dataDirectory) {
        return Arrays.asList(dataDirectory.toFile().list(new WildcardFileFilter("*.json")));
    }
}
