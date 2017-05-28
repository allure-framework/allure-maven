import groovy.json.JsonSlurper

import java.nio.file.Paths

import static io.qameta.allure.maven.TestHelper.checkReportDirectory

def outputDirectory = Paths.get(basedir.absolutePath, 'target', 'site', 'allure-maven-plugin')
checkReportDirectory(outputDirectory, 2)

def dataDirectory = outputDirectory.resolve('data')

def cateforiesPath = dataDirectory.resolve("categories.json")

def jsonSlurper = new JsonSlurper()
def categories = jsonSlurper.parseText(cateforiesPath.text)

assert categories.statistic
assert categories.statistic.failed == 1