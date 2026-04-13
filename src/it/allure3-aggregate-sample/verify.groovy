import java.nio.file.Paths
import java.nio.file.Files

import static io.qameta.allure.maven.TestHelper.checkReportDirectory
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is

def outputDirectory = Paths.get(basedir.absolutePath, 'target', 'site', 'allure-maven-plugin')
def config = Paths.get(basedir.absolutePath, 'target', 'allure-maven', 'allure3',
        'allurerc.json')
def results = Paths.get(basedir.absolutePath, 'target', 'allure-results')
def allureCli = Paths.get(basedir.absolutePath, '.allure', 'allure-3.4.1', 'node_modules',
        'allure', 'cli.js').toAbsolutePath().toString()
checkReportDirectory(outputDirectory, 1)

def args = Files.readAllLines(Paths.get(basedir.absolutePath, 'target',
        'allure3 aggregate args.txt'))
def expected = [ "cli=${allureCli}", 'command=generate', 'arg=generate',
        "arg=${results.toAbsolutePath()}", 'arg=--config',
        "arg=${config.toAbsolutePath()}", '---' ].collect { it.toString() }
assertThat(args, is(expected))
