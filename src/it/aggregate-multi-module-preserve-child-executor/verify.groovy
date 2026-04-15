import groovy.json.JsonSlurper

import java.nio.file.Files
import java.nio.file.Paths

import static io.qameta.allure.maven.TestHelper.checkReportDirectory

def basedirPath = basedir.absolutePath
def outputDirectory = Paths.get(basedirPath, 'target', 'site', 'allure-maven-plugin')

checkReportDirectory(outputDirectory, 2)

def slurper = new JsonSlurper()
def firstExecutor = Paths.get(basedirPath, 'first', 'target', 'allure-results', 'executor.json')
def secondExecutor = Paths.get(basedirPath, 'second', 'target', 'allure-results', 'executor.json')

assert Files.exists(firstExecutor)
assert Files.exists(secondExecutor)
assert slurper.parse(firstExecutor.toFile()).buildName == 'Allure Report Test First Child'
assert slurper.parse(secondExecutor.toFile()).buildName == 'Allure Report Test Second Child'
