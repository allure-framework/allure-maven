package io.qameta.allure.maven;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.io.FileUtils;
import org.apache.maven.settings.Proxy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AllureCommandline {

    public static final String ALLURE_DEFAULT_VERSION = "2.7.0";

    private static final int DEFAULT_TIMEOUT = 3600;

    private final String version;

    private final int timeout;

    private final Path installationDirectory;

    public AllureCommandline(final Path installationDirectory, final String version) {
        this(installationDirectory, version, DEFAULT_TIMEOUT);
    }

    public AllureCommandline(final Path installationDirectory, final String version, int timeout) {
        this.installationDirectory = installationDirectory;
        this.version = version;
        this.timeout = timeout;
    }

    public int generateReport(List<Path> resultsPaths, Path reportPath) throws IOException {

        this.checkAllureExists();

        FileUtils.deleteQuietly(reportPath.toFile());

        CommandLine commandLine = new CommandLine(getAllureExecutablePath().toAbsolutePath().toFile());
        commandLine.addArgument("generate");
        commandLine.addArgument("--clean");
        for (Path resultsPath : resultsPaths) {
            commandLine.addArgument(resultsPath.toAbsolutePath().toString(), true);
        }
        commandLine.addArgument("-o");
        commandLine.addArgument(reportPath.toAbsolutePath().toString(), true);

        return execute(commandLine, timeout);
    }

    public int serve(List<Path> resultsPaths, Path reportPath, String serveHost, Integer servePort) throws IOException {

        this.checkAllureExists();

        FileUtils.deleteQuietly(reportPath.toFile());

        CommandLine commandLine = new CommandLine(getAllureExecutablePath().toAbsolutePath().toFile());
        commandLine.addArgument("serve");
        if (serveHost != null && serveHost.matches("(\\d{1,3}\\.){3}\\d{1,3}")) {
            commandLine.addArgument("--host");
            commandLine.addArgument(serveHost);
        }
        if (servePort > 0) {
            commandLine.addArgument("--port");
            commandLine.addArgument("" + servePort);
        }
        for (Path resultsPath : resultsPaths) {
            commandLine.addArgument(resultsPath.toAbsolutePath().toString(), true);
        }
        return execute(commandLine, timeout);
    }

    private void checkAllureExists() throws FileNotFoundException {
        if (allureNotExists()) {
            throw new FileNotFoundException("There is no valid allure installation." +
                    " Make sure you're using allure version not less then 2.x.");
        }
    }

    private int execute(CommandLine commandLine, int timeout) throws IOException {
        DefaultExecutor executor = new DefaultExecutor();
        ExecuteWatchdog watchdog = new ExecuteWatchdog(TimeUnit.SECONDS.toMillis(timeout));
        executor.setWatchdog(watchdog);
        executor.setExitValue(0);
        return executor.execute(commandLine);
    }

    private Path getAllureExecutablePath() {
        String allureExecutable = isWindows() ? "allure.bat" : "allure";
        return getAllureHome().resolve("bin").resolve(allureExecutable);
    }

    private Path getAllureHome() {
        return installationDirectory.resolve(String.format("allure-%s", version != null ? version : ALLURE_DEFAULT_VERSION));
    }

    private boolean allureExists() {
        Path allureExecutablePath = getAllureExecutablePath();
        return Files.exists(allureExecutablePath) && Files.isExecutable(allureExecutablePath);
    }

    boolean allureNotExists() {
        return !allureExists();
    }

    public void download(String allureDownloadUrl, Proxy mavenProxy) throws IOException {

        Path allureZip;
        String allureUrl;
        URL url;

        if (allureExists()) {
            return;
        }

        if (version != null) {
            allureZip = Files.createTempFile("allure", version);
            allureUrl = String.format(allureDownloadUrl, version, version);
            url = new URL(allureUrl);
        } else {
            allureZip = Files.createTempFile("allure", ALLURE_DEFAULT_VERSION);
            allureUrl = String.format("/allure-%s.zip", ALLURE_DEFAULT_VERSION);
            url = AllureCommandline.class.getResource(allureUrl);
        }

        if (mavenProxy != null && version != null) {
            InetSocketAddress proxyAddress = new InetSocketAddress(mavenProxy.getHost(), mavenProxy.getPort());
            java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, proxyAddress);
            InputStream inputStream = url.openConnection(proxy).getInputStream();
            Files.copy(inputStream, allureZip, StandardCopyOption.REPLACE_EXISTING);
        } else {
            FileUtils.copyURLToFile(url, allureZip.toFile());
        }

        try {
            ZipFile zipFile = new ZipFile(allureZip.toFile());
            zipFile.extractAll(getInstallationDirectory().toAbsolutePath().toString());
        } catch (ZipException e) {
            throw new IOException(e);
        }

        Path allureExecutable = getAllureExecutablePath();
        if (Files.exists(allureExecutable)) {
            allureExecutable.toFile().setExecutable(true);
        }
    }

    public Path getInstallationDirectory() {
        return installationDirectory;
    }

    public String getVersion() {
        return version;
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
