import groovy.json.JsonSlurper

import java.nio.file.Paths

import static ru.yandex.qatools.allure.report.TestHelper.checkReportDirectory
import static ru.yandex.qatools.allure.report.TestHelper.getTestCases

def outputDirectory = Paths.get(basedir.absolutePath, 'target', 'site', 'allure-maven-plugin')
checkReportDirectory(outputDirectory, 1)

def dataDirectory = outputDirectory.resolve('data')

def testCasePath = dataDirectory.resolve(getTestCases(dataDirectory).get(0))

def jsonSlurper = new JsonSlurper()
def testCase = jsonSlurper.parseText(testCasePath.text)

assert testCase.issues
assert testCase.issues.name == "123"
assert testCase.issues.url == "http://example.com/123"