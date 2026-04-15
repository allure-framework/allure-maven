import java.nio.file.Files
import java.nio.file.Paths

import static io.qameta.allure.maven.TestHelper.checkAggregateMojoOnly
import static io.qameta.allure.maven.TestHelper.checkReportDirectory
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is

def basedirPath = basedir.absolutePath
def outputDirectory = Paths.get(basedirPath, 'target', 'site', 'allure-maven-plugin')
def config = Paths.get(basedirPath, 'target', 'allure-maven', 'allure3', 'allurerc.json')
def firstResults = Paths.get(basedirPath, 'first', 'target', 'module-results')
def secondResults = Paths.get(basedirPath, 'second', 'target', 'override-results')
def allureCli = Paths.get(basedirPath, '.allure', 'allure-3.4.1', 'node_modules', 'allure',
        'cli.js').toAbsolutePath().toString()

checkAggregateMojoOnly(Paths.get(basedirPath))
checkReportDirectory(outputDirectory, 1)

def args = Files.readAllLines(Paths.get(basedirPath, 'target',
        'allure3 aggregate results args.txt'))
def expected = [ "cli=${allureCli}", 'command=generate', 'arg=generate',
        "arg=${firstResults.toAbsolutePath()}", "arg=${secondResults.toAbsolutePath()}",
        'arg=--config', "arg=${config.toAbsolutePath()}", '---' ].collect {
    it.toString()
}
assertThat(args, is(expected))
