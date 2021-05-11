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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * Created by bvo2002 on 25.05.17.
 */
final class ProxyUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyUtils.class);

    private ProxyUtils() {
        throw new IllegalStateException("Do not instance");
    }

    @SuppressWarnings({"ModifiedControlVariable", "EmptyBlock", "PMD.AvoidInstantiatingObjectsInLoops"})
    public static Proxy getProxy(final MavenSession mavenSession, final SettingsDecrypter decrypter) {
        if (mavenSession == null || mavenSession.getSettings() == null
                || mavenSession.getSettings().getProxies() == null
                || mavenSession.getSettings().getProxies().isEmpty()) {
            LOGGER.info("Proxy is not specified.");
        } else {
            final List<Proxy> mavenProxies = mavenSession.getSettings().getProxies();
            for (Proxy proxy : mavenProxies) {
                if (proxy.isActive()) {
                    final Proxy decrypted = decryptProxy(proxy, decrypter);
                    try (Socket socket = new Socket(decrypted.getHost(), decrypted.getPort())) {
                        // do nothing
                    } catch (IOException e) {
                        LOGGER.info(String.format("Proxy: %s:%s is not available", decrypted.getHost(),
                                decrypted.getPort()));
                        continue;
                    }
                    LOGGER.info(
                            String.format("Found proxy: %s:%s", decrypted.getHost(), decrypted.getPort()));
                    return proxy;
                }
            }
            LOGGER.info("No active proxies found.");
        }
        return null;
    }

    private static Proxy decryptProxy(final Proxy proxy, final SettingsDecrypter decrypter) {
        final DefaultSettingsDecryptionRequest decryptionRequest =
                new DefaultSettingsDecryptionRequest(proxy);
        final SettingsDecryptionResult decryptedResult = decrypter.decrypt(decryptionRequest);
        return decryptedResult.getProxy();
    }
}
