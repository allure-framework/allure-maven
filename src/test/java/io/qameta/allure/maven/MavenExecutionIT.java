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

import io.qameta.allure.Description;
import org.apache.maven.executor.ExecutorResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MavenExecutionIT extends MavenTestSupport {

    @Test
    @Description("A nonzero Maven exit must fail the test and preserve the build failure log.")
    void shouldRejectFailedBuild() throws Exception {
        final Path projectDirectory = prepareProject(
                "maven-execution",
                resource("/maven-executor/maven-execution-pom.xml", "pom.xml")
        );

        assertThatThrownBy(() -> runGoals(projectDirectory, List.of("unknown-phase")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("should succeed");

        assertThat(Files.readString(projectDirectory.resolve("build.log")))
                .contains("BUILD FAILURE", "Unknown lifecycle phase");
    }

    @Test
    @Description("Maven --fail-never returns zero for a failed build; error logs must still fail the test.")
    void shouldRejectErrorLogDespiteZeroExitCode() throws Exception {
        final Path projectDirectory = prepareProject(
                "maven-execution",
                resource("/maven-executor/maven-execution-pom.xml", "pom.xml")
        );

        assertThatThrownBy(() -> runGoals(projectDirectory, List.of("unknown-phase"), List.of("--fail-never")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Maven build log should be free of errors");

        assertThat(Files.readString(projectDirectory.resolve("build.log")))
                .contains("Unknown lifecycle phase", "Build failures were ignored");
    }

    @Test
    @Description("Timestamp prefixes must not hide Maven errors when --fail-never returns zero.")
    void shouldRejectTimestampedErrorLog() throws Exception {
        final Path projectDirectory = prepareProject(
                "maven-timestamped-errors",
                resource("/maven-executor/maven-execution-pom.xml", "pom.xml"),
                resource("/maven-executor-data/timestamped-logging-jvm.config", ".mvn/jvm.config")
        );

        assertThatThrownBy(() -> runGoals(projectDirectory, List.of("unknown-phase"), List.of("--fail-never")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Maven build log should be free of errors");

        assertThat(Files.readString(projectDirectory.resolve("build.log")))
                .containsPattern("(?m)^\\d+ \\[ERROR\\]")
                .contains("Unknown lifecycle phase", "Build failures were ignored");
    }

    @Test
    @Description("ANSI colors inside Maven error labels must not hide errors when --fail-never returns zero.")
    void shouldRejectAnsiColoredErrorLog() throws Exception {
        final Path projectDirectory = prepareProject(
                "maven-ansi-errors",
                resource("/maven-executor/maven-execution-pom.xml", "pom.xml")
        );

        assertThatThrownBy(
                () -> runGoals(
                        projectDirectory, List.of("unknown-phase"), List.of("--fail-never", "-Dstyle.color=always")
                )
        )
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Maven build log should be free of errors");

        assertThat(Files.readString(projectDirectory.resolve("build.log")))
                .contains("\u001B[", "ERROR", "Unknown lifecycle phase", "Build failures were ignored")
                .doesNotContain("[ERROR]");
    }

    @Test
    @Description("Combined timestamp prefixes and ANSI colors must not hide Maven errors with a zero exit code.")
    void shouldRejectTimestampedAnsiColoredErrorLog() throws Exception {
        final Path projectDirectory = prepareProject(
                "maven-timestamped-ansi-errors",
                resource("/maven-executor/maven-execution-pom.xml", "pom.xml"),
                resource("/maven-executor-data/timestamped-logging-jvm.config", ".mvn/jvm.config")
        );

        assertThatThrownBy(
                () -> runGoals(
                        projectDirectory, List.of("unknown-phase"), List.of("--fail-never", "-Dstyle.color=always")
                )
        )
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Maven build log should be free of errors");

        assertThat(readLines(projectDirectory.resolve("build.log")))
                .anyMatch(line -> line.matches("\\d+ .*") && line.contains("\u001B[") && line.contains("ERROR"));
        assertThat(Files.readString(projectDirectory.resolve("build.log")))
                .contains("Unknown lifecycle phase", "Build failures were ignored")
                .doesNotContain("[ERROR]");
    }

    @Test
    @Description("An expected failed build is identified by its exit status and keeps its diagnostic log.")
    void shouldAcceptExpectedBuildFailure() throws Exception {
        final Path projectDirectory = prepareProject(
                "maven-execution",
                resource("/maven-executor/maven-execution-pom.xml", "pom.xml")
        );

        final ExecutorResult result = expectFailure(projectDirectory, List.of("unknown-phase"), List.of());

        assertThat(result.exitCode()).contains(1);
        assertThat(Files.readString(projectDirectory.resolve("build.log")))
                .contains("BUILD FAILURE", "Unknown lifecycle phase");
    }

    @Test
    @Description("An expected-failure check must reject a successful Maven build instead of silently passing.")
    void shouldRejectUnexpectedBuildSuccess() throws Exception {
        final Path projectDirectory = prepareProject(
                "maven-execution",
                resource("/maven-executor/maven-execution-pom.xml", "pom.xml")
        );

        assertThatThrownBy(() -> expectFailure(projectDirectory, List.of("validate"), List.of()))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected Maven goals to fail");

        assertThat(Files.readString(projectDirectory.resolve("build.log"))).contains("BUILD SUCCESS");
    }
}
