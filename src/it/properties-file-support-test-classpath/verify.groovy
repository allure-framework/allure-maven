import groovy.json.JsonSlurper

import java.nio.file.Paths

import static io.qameta.allure.maven.TestHelper.checkReportDirectory
import static io.qameta.allure.maven.TestHelper.getTestCases

def outputDirectory = Paths.get(basedir.absolutePath, 'target', 'site', 'allure-maven-plugin')
checkReportDirectory(outputDirectory, 1)

def dataDirectory = outputDirectory.resolve('data')
def testCasesDirectory = dataDirectory.resolve('test-cases')

def testCasePath = testCasesDirectory.resolve(getTestCases(testCasesDirectory).get(0))

def jsonSlurper = new JsonSlurper()
def testCase = jsonSlurper.parseText(testCasePath.text)

assert testCase.links
assert testCase.links.size() == 1

def link = testCase.links.get(0)

assert link
assert link.name == "issue-123"
assert link.url == "http://example.com/issue-123"