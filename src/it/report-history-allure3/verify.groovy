import groovy.json.JsonSlurper
import java.nio.file.Files
import java.nio.file.Paths

import static io.qameta.allure.maven.TestHelper.checkRegularReportMojoOnly
import static io.qameta.allure.maven.TestHelper.checkReportDirectory
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static ru.yandex.qatools.matchers.nio.PathMatchers.isDirectory

def basedirPath = basedir.absolutePath
def outputDirectory = Paths.get(basedirPath, 'target', 'site', 'allure-maven-plugin')
def configPath = Paths.get(basedirPath, 'target', 'allure-maven', 'allure3', 'allurerc.json')
def historyFile = Paths.get(basedirPath, '.allure', 'history', 'report', 'history.jsonl')
def resultsPath = Paths.get(basedirPath, 'target', 'allure-results')
def allureCli = Paths.get(basedirPath, '.allure', 'allure-3.4.1', 'node_modules', 'allure',
        'cli.js').toAbsolutePath().toString()
def args = Files.readAllLines(Paths.get(basedirPath, 'target', 'allure3 history args.txt'))

checkRegularReportMojoOnly(Paths.get(basedirPath))
checkReportDirectory(outputDirectory, 1)

def expectedInvocation = [ "cli=${allureCli}", 'command=generate', 'arg=generate',
        "arg=${resultsPath.toAbsolutePath()}", 'arg=--config',
        "arg=${configPath.toAbsolutePath()}", '---' ].collect { it.toString() }
assertThat(args, is(expectedInvocation))

def config = new JsonSlurper().parse(configPath.toFile())
assertThat(config.historyPath, is(historyFile.toAbsolutePath().toString()))
assertThat(config.appendHistory, is(true))
assertThat(historyFile.parent, isDirectory())
