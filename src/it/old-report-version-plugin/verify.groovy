import java.nio.file.Paths

import static io.qameta.allure.maven.TestHelper.checkReportDirectory

def base = Paths.get(basedir.absolutePath, 'target', 'site')

checkReportDirectory(base.resolve('allure'), 1)
checkReportDirectory(base.resolve('allure-maven-plugin'), 1)
