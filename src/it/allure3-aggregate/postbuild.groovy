import groovy.json.JsonOutput

long started = System.currentTimeMillis()
def writeAllureResult = { String status, Throwable error = null ->
    File resultsDir = new File(basedir, '../../allure-results').canonicalFile
    resultsDir.mkdirs()
    Map result = [
        uuid: UUID.randomUUID().toString(),
        historyId: 'maven.invoker.it.allure3-aggregate',
        testCaseId: 'maven.invoker.it.allure3-aggregate',
        fullName: 'maven.invoker.it.allure3-aggregate',
        name: 'allure3-aggregate',
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
    new File(resultsDir, "allure3-aggregate-${result.uuid}-result.json").text =
        JsonOutput.prettyPrint(JsonOutput.toJson(result))
}

try {
    File report = new File(basedir, 'target/site/allure-maven-plugin/index.html')
    if (!report.isFile()) {
        throw new AssertionError("Expected generated aggregate report index.html at ${report}")
    }

    writeAllureResult('passed')
    return true
} catch (Throwable error) {
    writeAllureResult('failed', error)
    throw error
}
