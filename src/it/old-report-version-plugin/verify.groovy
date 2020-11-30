import java.nio.file.Paths

import static io.qameta.allure.maven.TestHelper.checkReportDirectory
import static org.hamcrest.CoreMatchers.containsString
import static org.junit.Assert.*

def base = Paths.get(basedir.absolutePath, 'target', 'site')

checkReportDirectory(base.resolve('allure'), 1)
checkReportDirectory(base.resolve('allure-maven-plugin'), 1)

assertThat Paths.get(basedir.absolutePath, 'build.log').toFile().text, containsString('from https://dl.bintray.com/')
