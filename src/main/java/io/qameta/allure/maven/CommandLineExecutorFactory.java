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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

final class CommandLineExecutorFactory {

    private CommandLineExecutorFactory() {
        // Utility class.
    }

    static DefaultExecutor newExecutor(final int timeout) {
        final DefaultExecutor executor = DefaultExecutor.builder().get();
        final ExecuteWatchdog watchdog = ExecuteWatchdog.builder()
                .setTimeout(Duration.ofMillis(TimeUnit.SECONDS.toMillis(timeout))).get();
        executor.setWatchdog(watchdog);
        executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());
        executor.setExitValue(0);
        return executor;
    }
}
