import java.nio.file.Paths

import static io.qameta.allure.maven.TestHelper.checkAggregateMojoOnly
import static io.qameta.allure.maven.TestHelper.checkReportDirectory

def basedirPath = basedir.absolutePath
checkAggregateMojoOnly(Paths.get(basedirPath))

def outputDirectory = Paths.get(basedirPath, 'target', 'site', 'allure-maven-plugin')

checkReportDirectory(outputDirectory, 2)
