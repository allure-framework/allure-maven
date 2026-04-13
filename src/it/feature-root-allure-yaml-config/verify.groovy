import groovy.json.JsonSlurper
import java.nio.file.Paths
import java.nio.file.Files

import static io.qameta.allure.maven.TestHelper.checkReportDirectory
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is

def basedirPath = basedir.absolutePath
def outputDirectory = Paths.get(basedirPath, 'target', 'site', 'allure-maven-plugin')
def configPath = Paths.get(basedirPath, 'target', 'allure-maven', 'allure3', 'allurerc.json')
def resultsPath = Paths.get(basedirPath, 'target', 'allure-results')
def allureCli = Paths.get(basedirPath, '.allure', 'allure-3.4.1', 'node_modules', 'allure',
        'cli.js').toAbsolutePath().toString()

checkReportDirectory(outputDirectory, 1)

def args = Files.readAllLines(Paths.get(basedirPath, 'target', 'allure3 args.txt'))
def expectedArgs = [ "cli=${allureCli}", 'command=generate', 'arg=generate',
        "arg=${resultsPath.toAbsolutePath()}", 'arg=--config',
        "arg=${configPath.toAbsolutePath()}", '---' ].collect { it.toString() }
assertThat(args, is(expectedArgs))

def config = new JsonSlurper().parse(Paths.get(basedirPath, 'target',
        'allure-maven', 'allure3', 'allurerc.json').toFile())
assertThat(config.plugins.custom.enabled, is(true))
assertThat(config.plugins.awesome.options.reportLanguage, is('en'))
assertThat(config.plugins.awesome.options.singleFile, is(false))
