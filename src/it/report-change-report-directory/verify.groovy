import java.nio.file.Paths

import static ru.yandex.qatools.allure.report.TestHelper.checkReportDirectory

def outputDirectory = Paths.get(basedir.absolutePath, 'target', 'allure')
checkReportDirectory(outputDirectory, 1)