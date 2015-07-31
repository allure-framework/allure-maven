import java.nio.file.Paths

import static ru.yandex.qatools.allure.report.TestHelper.checkReportDirectory

def basedirPath = basedir.absolutePath
def firstOutputDirectory = Paths.get(basedirPath, 'first', 'target', 'site', 'allure-maven-plugin')
def secondOutputDirectory = Paths.get(basedirPath, 'second', 'target', 'site', 'allure-maven-plugin')

checkReportDirectory(firstOutputDirectory, 1)
checkReportDirectory(secondOutputDirectory, 1)
