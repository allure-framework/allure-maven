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
public class ProxyUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyUtils.class);

    static Proxy getProxy(MavenSession mavenSession, SettingsDecrypter decrypter) {
        if (mavenSession == null ||
                mavenSession.getSettings() == null ||
                mavenSession.getSettings().getProxies() == null ||
                mavenSession.getSettings().getProxies().isEmpty()) {
            LOGGER.info("Proxy is not specified.");
            return null;
        } else {
            final List<Proxy> mavenProxies = mavenSession.getSettings().getProxies();
            for (Proxy proxy : mavenProxies) {
                if (proxy.isActive()) {
                    proxy = decryptProxy(proxy, decrypter);
                    try (Socket socket = new Socket(proxy.getHost(), proxy.getPort())) {
                    } catch (IOException e) {
                        LOGGER.info(String.format("Proxy: %s:%s is not available", proxy.getHost(), proxy.getPort()));
                        continue;
                    }
                    LOGGER.info(String.format("Found proxy: %s:%s", proxy.getHost(), proxy.getPort()));
                    return proxy;
                }
            }
            LOGGER.info("No active proxies found.");
            return null;
        }
    }

    private static Proxy decryptProxy(Proxy proxy, SettingsDecrypter decrypter) {
        final DefaultSettingsDecryptionRequest decryptionRequest = new DefaultSettingsDecryptionRequest(proxy);
        SettingsDecryptionResult decryptedResult = decrypter.decrypt(decryptionRequest);
        return decryptedResult.getProxy();
    }
}
