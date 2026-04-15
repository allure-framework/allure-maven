import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static ru.yandex.qatools.matchers.nio.PathMatchers.exists

def installDirectory = Paths.get(basedir.absolutePath, '.allure install')
def captureFile = Paths.get(basedir.absolutePath, 'target', 'allure3 install args.txt')
def allureCli = installDirectory.resolve(Paths.get('allure-3.4.1', 'node_modules', 'allure',
        'cli.js'))
def platform = System.getProperty('os.name').toLowerCase().contains('win')
        ? 'win'
        : (System.getProperty('os.name').toLowerCase().contains('mac')
                || System.getProperty('os.name').toLowerCase().contains('darwin')
                ? 'darwin' : 'linux')
def arch = ['x86_64', 'amd64'].contains(System.getProperty('os.arch').toLowerCase())
        ? 'x64' : 'arm64'
def nodeHome = installDirectory.resolve("node-v24.14.1-${platform}-${arch}")
def npmCli = System.getProperty('os.name').toLowerCase().contains('win')
        ? nodeHome.resolve(Paths.get('node_modules', 'npm', 'bin', 'npm-cli.js'))
        : nodeHome.resolve(Paths.get('lib', 'node_modules', 'npm', 'bin', 'npm-cli.js'))
def allureExecutable = installDirectory.resolve(Paths.get('bin',
        System.getProperty('os.name').toLowerCase().contains('win') ? 'allure.bat' : 'allure'))

assertThat(allureCli, exists())
assertThat(allureExecutable, exists())
def expected = [ "cli=${npmCli.toAbsolutePath()}", 'arg=--prefix',
        "arg=${installDirectory.resolve('allure-3.4.1').toAbsolutePath()}",
        'arg=install', 'arg=--no-package-lock', 'arg=--no-save',
        'arg=--ignore-scripts', 'arg=allure@3.4.1', 'arg=--registry',
        'arg=https://registry.npmjs.org' ].collect { it.toString() }
assertThat(Files.readAllLines(captureFile, StandardCharsets.UTF_8), is(expected))
