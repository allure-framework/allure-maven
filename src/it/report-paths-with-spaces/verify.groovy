import java.nio.file.Paths

import static io.qameta.allure.maven.TestHelper.checkReportDirectory

def outputDirectory = Paths.get(basedir.absolutePath, 'target', 'site', 'allure report')
checkReportDirectory(outputDirectory, 1)
