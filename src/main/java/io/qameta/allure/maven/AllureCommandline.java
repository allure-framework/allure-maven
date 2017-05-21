package io.qameta.allure.maven;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.io.FileUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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

        if (Files.exists(reportPath)) {
            FileUtils.deleteDirectory(reportPath.toFile());
        }

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

    public void download(String allureDownloadRoot, boolean forceClean) throws IOException {
        if (forceClean) {
            Files.delete(getAllureHome());
        }

        if (exists()) {
            return;
        }

        //AllureArtifactClient client = newClient(allureDownloadRoot, AllureArtifactClient.class);
        Path allureZip = Files.createTempFile("allure", version);
        //Files.write(allureZip, client.download(version).execute().body().bytes());

        URL allureUrl = new URL(allureDownloadRoot +
                "io/qameta/allure/allure/" + version + "/allure-" + version+".zip");

        FileUtils.copyURLToFile(allureUrl, allureZip.toFile());
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
