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
import org.junit.Test;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class CommandLineExecutorFactoryTest {

    @Test
    public void shouldConfigureShutdownHookProcessDestroyer() {
        final DefaultExecutor executor = CommandLineExecutorFactory.newExecutor(10);

        assertThat(executor.getProcessDestroyer(), instanceOf(ShutdownHookProcessDestroyer.class));
    }

    @Test
    public void shouldConfigureWatchdogTimeoutInSeconds() {
        final ExecuteWatchdog watchdog = CommandLineExecutorFactory.newExecutor(10).getWatchdog();

        assertThat(watchdog, notNullValue());
        assertThat(getTimeout(watchdog), is(Duration.ofSeconds(10)));
    }

    @Test
    public void shouldTreatOnlyZeroExitCodeAsSuccess() {
        final DefaultExecutor executor = CommandLineExecutorFactory.newExecutor(10);

        assertThat(executor.isFailure(0), is(false));
        assertThat(executor.isFailure(1), is(true));
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
