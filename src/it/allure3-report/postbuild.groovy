import groovy.json.JsonOutput

long started = System.currentTimeMillis()
def writeAllureResult = { String status, Throwable error = null ->
    File resultsDir = new File(basedir, '../../allure-results').canonicalFile
    resultsDir.mkdirs()
    Map result = [
        uuid: UUID.randomUUID().toString(),
        historyId: 'maven.invoker.it.allure3-report',
        testCaseId: 'maven.invoker.it.allure3-report',
        fullName: 'maven.invoker.it.allure3-report',
        name: 'allure3-report',
        status: status,
        stage: 'finished',
        start: started,
        stop: System.currentTimeMillis()
    ]
    if (error != null) {
        result.statusDetails = [
            message: error.message,
            trace: error.stackTrace.join(System.lineSeparator())
        ]
    }
    new File(resultsDir, "allure3-report-${result.uuid}-result.json").text =
        JsonOutput.prettyPrint(JsonOutput.toJson(result))
}

try {
    File report = new File(basedir, 'target/site/allure-maven-plugin/index.html')
    if (!report.isFile()) {
        throw new AssertionError("Expected generated report index.html at ${report}")
    }

    writeAllureResult('passed')
    return true
} catch (Throwable error) {
    writeAllureResult('failed', error)
    throw error
}
