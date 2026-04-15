import java.nio.file.Paths

import static io.qameta.allure.maven.TestHelper.checkReportDirectory
import static org.hamcrest.MatcherAssert.assertThat
import static ru.yandex.qatools.matchers.nio.PathMatchers.exists

def base = Paths.get(basedir.absolutePath, 'target', 'site')

checkReportDirectory(base.resolve('allure-maven-plugin'), 1)
assertThat(base.resolve('allure').resolve('allure-maven.html'), exists())
