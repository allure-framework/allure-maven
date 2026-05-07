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

import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;

import static io.qameta.allure.Allure.addAttachment;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@Tag("commandline")
class CommandLineExecutorFactoryTest {

    @Test
    void shouldConfigureShutdownHookProcessDestroyer() {
        final DefaultExecutor executor = step(
                "Create executor with 10 second timeout",
                () -> CommandLineExecutorFactory.newExecutor(10)
        );

        step("Verify shutdown hook process destroyer", () -> {
            addAttachment(
                    "Executor destroyer",
                    executor.getProcessDestroyer().getClass().getName()
            );
            assertThat(executor.getProcessDestroyer())
                    .isInstanceOf(ShutdownHookProcessDestroyer.class);
        });
    }

    @Test
    void shouldConfigureWatchdogTimeoutInSeconds() {
        final DefaultExecutor executor = step(
                "Create executor with 10 second timeout",
                () -> CommandLineExecutorFactory.newExecutor(10)
        );
        final ExecuteWatchdog watchdog = step("Capture watchdog from executor", executor::getWatchdog);
        final Duration timeout = step("Inspect watchdog timeout via reflection", () -> getTimeout(watchdog));

        step("Verify watchdog timeout", () -> {
            addAttachment(
                    "Watchdog timeout", String.join(
                            System.lineSeparator(),
                            "timeout=" + timeout, "watchdogClass=" + watchdog.getClass().getName()
                    )
            );
            assertThat(watchdog).isNotNull();
            assertThat(timeout).isEqualTo(Duration.ofSeconds(10));
        });
    }

    @Test
    void shouldTreatOnlyZeroExitCodeAsSuccess() {
        final DefaultExecutor executor = step(
                "Create executor with 10 second timeout",
                () -> CommandLineExecutorFactory.newExecutor(10)
        );

        step("Verify exit-code failure mapping", () -> {
            final boolean zeroExitFailure = executor.isFailure(0);
            final boolean oneExitFailure = executor.isFailure(1);
            addAttachment(
                    "Failure decisions", String.join(
                            System.lineSeparator(),
                            "exitCode=0 -> " + zeroExitFailure, "exitCode=1 -> " + oneExitFailure
                    )
            );
            assertThat(zeroExitFailure).isFalse();
            assertThat(oneExitFailure).isTrue();
        });
    }

    private static Duration getTimeout(final ExecuteWatchdog watchdog) {
        try {
            final Method getWatchdog = ExecuteWatchdog.class.getDeclaredMethod("getWatchdog");
            getWatchdog.setAccessible(true);
            return ((org.apache.commons.exec.Watchdog) getWatchdog.invoke(watchdog)).getTimeout();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to inspect watchdog timeout", e);
        }
    }
}
