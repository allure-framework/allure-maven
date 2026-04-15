import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

def version = '2.30.0'
def localRepository = Paths.get(basedir.absolutePath, 'target', 'it-local-repo')
def sharedRepository = localRepositoryPath.toPath()
copyRecursively(
        sharedRepository.resolve(Paths.get('io', 'qameta', 'allure', 'allure-maven')),
        localRepository.resolve(Paths.get('io', 'qameta', 'allure', 'allure-maven'))
)

def artifactDirectory = localRepository
        .resolve(Paths.get('io', 'qameta', 'allure', 'allure-commandline', version))
Files.createDirectories(artifactDirectory)

def zipPath = artifactDirectory.resolve("allure-commandline-${version}.zip")
def pomPath = artifactDirectory.resolve("allure-commandline-${version}.pom")
def captureFile = Paths.get(basedir.absolutePath, 'target', 'captured commands.txt').toAbsolutePath()
def captureDirectory = captureFile.getParent().toAbsolutePath()
def reportDirectory = Paths.get(basedir.absolutePath, 'target', 'site', 'preserved-report')
        .toAbsolutePath()

Files.write(pomPath, """\
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-commandline</artifactId>
    <version>${version}</version>
    <packaging>zip</packaging>
</project>
""".stripIndent().getBytes(StandardCharsets.UTF_8))

def unixLauncher = [
        '#!/bin/sh',
        'set -eu',
        'command="$1"',
        "mkdir -p \"${captureDirectory}\"",
        "touch \"${captureFile}\"",
        "printf 'command=%s\\n' \"\$command\" >> \"${captureFile}\"",
        'for arg in "$@"; do',
        "  printf 'arg=%s\\n' \"\$arg\" >> \"${captureFile}\"",
        'done',
        "printf '%s\\n' '---' >> \"${captureFile}\"",
        'if [ "$command" = "generate" ]; then',
        "  mkdir -p \"${reportDirectory}\"",
        "  : > \"${reportDirectory.resolve('index.html')}\"",
        'fi',
        'exit 0',
        ''
].join('\n')

def windowsLauncher = [
        '@echo off',
        'setlocal EnableExtensions DisableDelayedExpansion',
        "if not exist \"${captureDirectory}\" mkdir \"${captureDirectory}\"",
        "if not exist \"${captureFile}\" type nul > \"${captureFile}\"",
        'set "COMMAND=%~1"',
        ">> \"${captureFile}\" echo(command=%COMMAND%",
        ':loop',
        'if "%~1"=="" goto done',
        ">> \"${captureFile}\" echo(arg=%~1",
        'shift',
        'goto loop',
        ':done',
        ">> \"${captureFile}\" echo(---",
        'if /I not "%COMMAND%"=="generate" exit /b 0',
        "if not exist \"${reportDirectory}\" mkdir \"${reportDirectory}\"",
        "type nul > \"${reportDirectory.resolve('index.html')}\"",
        'exit /b 0',
        ''
].join('\r\n')

def zip = new ZipOutputStream(Files.newOutputStream(zipPath))
try {
    addZipEntry(zip, "allure-${version}/bin/allure", unixLauncher)
    addZipEntry(zip, "allure-${version}/bin/allure.bat", windowsLauncher)
} finally {
    zip.close()
}

static void addZipEntry(final ZipOutputStream zip, final String name, final String content) {
    zip.putNextEntry(new ZipEntry(name))
    zip.write(content.getBytes(StandardCharsets.UTF_8))
    zip.closeEntry()
}

static void copyRecursively(final Path source, final Path target) {
    if (!Files.exists(source)) {
        return
    }
    Files.walk(source).forEach { path ->
        def relative = source.relativize(path)
        def destination = target.resolve(relative.toString())
        if (Files.isDirectory(path)) {
            Files.createDirectories(destination)
        } else {
            Files.createDirectories(destination.getParent())
            Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
