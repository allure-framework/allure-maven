import java.nio.file.Files
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

    if (!System.getProperty('os.name').toLowerCase(Locale.ENGLISH).contains('win')) {
        File installDir = new File(basedir, '../.allure').canonicalFile
        File[] nodeHomes = installDir.listFiles({ File file ->
            file.isDirectory() && file.name.startsWith('node-v')
        } as FileFilter)
        if (nodeHomes == null || nodeHomes.length == 0) {
            throw new AssertionError("Expected downloaded Node.js runtime under ${installDir}")
        }

        File npm = new File(nodeHomes[0], 'bin/npm')
        if (!Files.isSymbolicLink(npm.toPath())) {
            throw new AssertionError("Expected ${npm} to be a symbolic link")
        }

        String target = Files.readSymbolicLink(npm.toPath()).toString()
        if (target != '../lib/node_modules/npm/bin/npm-cli.js') {
            throw new AssertionError("Expected ${npm} to point to npm-cli.js, got ${target}")
        }
    }

    writeAllureResult('passed')
    return true
} catch (Throwable error) {
    writeAllureResult('failed', error)
    throw error
}
