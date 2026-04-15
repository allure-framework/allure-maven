import java.nio.file.Paths

import static io.qameta.allure.maven.TestHelper.checkAggregateMojoOnly
import static io.qameta.allure.maven.TestHelper.checkReportDirectory

checkAggregateMojoOnly(Paths.get(basedir.absolutePath))

def outputDirectory = Paths.get(basedir.absolutePath, 'target', 'site', 'allure-maven-plugin')
checkReportDirectory(outputDirectory, 1)
