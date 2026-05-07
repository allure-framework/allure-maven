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

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.reporting.MavenReportException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static io.qameta.allure.Allure.addAttachment;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
@Tag("serve")
class AllureServeMojoTest {

    @Test
    void shouldRejectServeHostForAllure3() throws Exception {
        final AllureServeMojo mojo = new AllureServeMojo();
        setField(mojo, "serveHost", "127.0.0.1");
        addAttachment(
                "Serve host override",
                String.join(System.lineSeparator(), "serveHost=127.0.0.1", "allureVersion=3.4.1")
        );

        step("Verify Allure 3 rejects serveHost", () -> {
            final MavenReportException error = assertThrows(
                    MavenReportException.class, () -> mojo
                            .generateReport(Collections.emptyList(), AllureVersion.resolve("3.4.1"))
            );
            addAttachment("Serve host rejection", String.valueOf(error.getMessage()));
            assertThat(error).hasMessageContaining("does not support allure.serve.host");
        });
    }

    @Test
    void shouldIgnoreSingleFileForAllure3ServeAndWarn() throws Exception {
        final Path projectDirectory = Files.createTempDirectory("allure3-serve-mojo");
        try {
            final FakeAllure3Commandline commandline = new FakeAllure3Commandline(projectDirectory);
            final TestServeMojo mojo = new TestServeMojo(commandline);
            final RecordingLog log = new RecordingLog();
            final Path resultsDirectory = projectDirectory.resolve("results");

            step("Prepare Allure 3 serve mojo with single-file flag enabled", () -> {
                mojo.singleFile = true;
                mojo.buildDirectory = projectDirectory.resolve("build").toString();
                mojo.reportDirectory = projectDirectory.resolve("report").toString();
                mojo.projectDirectory = projectDirectory.toString();
                mojo.setLog(log);
                addAttachment(
                        "Serve mojo inputs",
                        String.join(
                                System.lineSeparator(), "projectDirectory=" + projectDirectory,
                                "resultsDirectory=" + resultsDirectory,
                                "reportDirectory=" + mojo.reportDirectory,
                                "singleFile=" + mojo.singleFile
                        )
                );
            });

            step(
                    "Generate report through Allure 3 serve flow",
                    () -> mojo.generateReport(
                            Collections.singletonList(resultsDirectory),
                            AllureVersion.resolve("3.4.1")
                    )
            );

            step("Verify single-file flag is ignored and warning is logged", () -> {
                addAttachment(
                        "Serve warning log",
                        String.join(System.lineSeparator(), log.warnMessages)
                );
                assertThat(commandline.singleFile).isFalse();
                assertThat(log.warnMessages.get(0))
                        .contains("Ignoring singleFile for Allure 3 serve");
            });
        } finally {
            FileUtils.deleteQuietly(projectDirectory.toFile());
        }
    }

    private static void setField(final Object target, final String fieldName, final Object value)
            throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class TestServeMojo extends AllureServeMojo {

        private final Allure3Commandline commandline;

        private TestServeMojo(final Allure3Commandline commandline) {
            this.commandline = commandline;
        }

        @Override
        protected Allure3Commandline createAllure3Commandline(final AllureVersion allureVersion,
                                                              final int timeout) {
            return commandline;
        }
    }

    private static final class FakeAllure3Commandline extends Allure3Commandline {

        private boolean singleFile;

        private FakeAllure3Commandline(final Path installDirectory) {
            super(
                    installDirectory, "3.4.1", NODE_DEFAULT_VERSION, NODE_DEFAULT_DOWNLOAD_URL,
                    NPM_DEFAULT_REGISTRY, null, null, new Properties(), false, 10
            );
        }

        @Override
        public int serve(final List<Path> resultsPaths, final Path reportPath,
                         final boolean singleFile, final Path buildDirectory, final String reportName,
                         final Integer servePort, final Path userConfigPath) {
            this.singleFile = singleFile;
            return 0;
        }
    }

    private static final class RecordingLog implements Log {

        private final List<String> warnMessages = new ArrayList<String>();

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void debug(final CharSequence content) {
        }

        @Override
        public void debug(final CharSequence content, final Throwable error) {
        }

        @Override
        public void debug(final Throwable error) {
        }

        @Override
        public boolean isInfoEnabled() {
            return false;
        }

        @Override
        public void info(final CharSequence content) {
        }

        @Override
        public void info(final CharSequence content, final Throwable error) {
        }

        @Override
        public void info(final Throwable error) {
        }

        @Override
        public boolean isWarnEnabled() {
            return true;
        }

        @Override
        public void warn(final CharSequence content) {
            warnMessages.add(content.toString());
        }

        @Override
        public void warn(final CharSequence content, final Throwable error) {
            warn(content);
        }

        @Override
        public void warn(final Throwable error) {
            warn(error.getMessage());
        }

        @Override
        public boolean isErrorEnabled() {
            return false;
        }

        @Override
        public void error(final CharSequence content) {
        }

        @Override
        public void error(final CharSequence content, final Throwable error) {
        }

        @Override
        public void error(final Throwable error) {
        }
    }
}
