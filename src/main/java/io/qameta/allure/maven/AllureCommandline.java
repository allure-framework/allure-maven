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
import java.net.SocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class AllureCommandline {

    private final String version;

    private final Path installationDirectory;

    public AllureCommandline(final Path installationDirectory, final String version) {
        this.installationDirectory = installationDirectory;
        this.version = version;
    }

    public int generateReport(List<Path> resultsPaths, Path reportPath) throws IOException {
        if (notExists()) {
            throw new FileNotFoundException("Can't find allure installation");
        }

        FileUtils.deleteQuietly(reportPath.toFile());

        CommandLine commandLine = new CommandLine(getAllureExecutablePath().toAbsolutePath().toFile());
        commandLine.addArgument("generate");
        commandLine.addArgument("--clean");
        for (Path resultsPath : resultsPaths) {
            commandLine.addArgument(resultsPath.toAbsolutePath().toString(), true);
        }
        commandLine.addArgument("-o");
        commandLine.addArgument(reportPath.toAbsolutePath().toString(), true);

        DefaultExecutor executor = new DefaultExecutor();
        ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
        executor.setWatchdog(watchdog);
        executor.setExitValue(0);
        return executor.execute(commandLine);
    }

    public int serve(List<Path> resultsPaths, Path reportPath) throws IOException {
        if (notExists()) {
            throw new FileNotFoundException("Can't find allure installation");
        }

        FileUtils.deleteQuietly(reportPath.toFile());

        CommandLine commandLine = new CommandLine(getAllureExecutablePath().toAbsolutePath().toFile());
        commandLine.addArgument("serve");
        for (Path resultsPath : resultsPaths) {
            commandLine.addArgument(resultsPath.toAbsolutePath().toString(), true);
        }

        DefaultExecutor executor = new DefaultExecutor();
        ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
        executor.setWatchdog(watchdog);
        executor.setExitValue(0);
        return executor.execute(commandLine);
    }

    private Path getAllureExecutablePath() {
        String allureExecutable = isWindows() ? "allure.bat" : "allure";
        return getAllureHome().resolve("bin").resolve(allureExecutable);
    }

    private Path getAllureHome() {
        return installationDirectory.resolve("allure-" + version);
    }

    private boolean exists() {
        Path allureExecutablePath = getAllureExecutablePath();
        return Files.exists(allureExecutablePath) && Files.isExecutable(allureExecutablePath);
    }

    boolean notExists() {
        return !exists();
    }

    public void download(String allureDownloadUrl, Proxy mavenProxy) throws IOException {

        if (exists()) {
            return;
        }

        Path allureZip = Files.createTempFile("allure", version);
        URL allureUrl = new URL(String.format(allureDownloadUrl, version, version));

        if (mavenProxy != null) {
            InetSocketAddress proxyAddress = new InetSocketAddress(mavenProxy.getHost(), mavenProxy.getPort());
            java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.DIRECT, proxyAddress);
            InputStream inputStream = allureUrl.openConnection(proxy).getInputStream();
            Files.copy(inputStream, allureZip, StandardCopyOption.REPLACE_EXISTING);
        } else {
            FileUtils.copyURLToFile(allureUrl, allureZip.toFile());
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
